package com.example.mobile.ui.passenger;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentPassengerHomeBinding;
import com.example.mobile.models.CreateRideRequest;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideRequirements;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.SharedPreferencesManager;
import com.example.mobile.viewmodels.PassengerHomeViewModel;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class PassengerHomeFragment extends Fragment {
    private static final String TAG = "PassengerHomeFragment";
    private static final int REFRESH_INTERVAL_MS = 10000; // 10 seconds

    private FragmentPassengerHomeBinding binding;
    private PassengerHomeViewModel viewModel;
    private RideMapRenderer mapRenderer;

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private final List<Marker> vehicleMarkers = new ArrayList<>();

    // Temporary storage for ride creation
    private ArrayList<String> rideLocations = new ArrayList<>();
    private ArrayList<String> passengerEmails = new ArrayList<>();
    private String vehicleType;
    private boolean babyTransport;
    private boolean petTransport;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPassengerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();
        setupMap();
        setupListeners();
        setupFragmentResultListeners();
        observeViewModel();
        setupVehicleRefresh();

        // Initial data load
        viewModel.checkForActiveRide();
        viewModel.fetchActiveVehicles();
    }

    private void initViewModel() {
        SharedPreferencesManager preferencesManager = new SharedPreferencesManager(requireContext());
        viewModel = new PassengerHomeViewModel(preferencesManager);
    }

    private void setupMap() {
        mapRenderer = new RideMapRenderer(requireActivity(), binding.map);
        mapRenderer.initMap();
    }

    private void setupListeners() {
        binding.btnMenu.setOnClickListener(v -> {
            ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
        });

        binding.btnRequestRide.setOnClickListener(v -> openRequestRideDialog());
        binding.btnLinkPassengers.setOnClickListener(v -> openLinkPassengersDialog());
    }

    private void setupFragmentResultListeners() {
        // Listen for ride order from OrderRideDialog
        getParentFragmentManager().setFragmentResultListener(
                OrderRideDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> handleRideOrderResult(bundle)
        );

        // Listen for old RequestRideFormDialog (backward compatibility)
        getParentFragmentManager().setFragmentResultListener(
                RequestRideFormDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> handleRideOrderResultLegacy(bundle)
        );

        // Listen for emails from LinkPassengersNewDialog
        getParentFragmentManager().setFragmentResultListener(
                LinkPassengersNewDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> handleLinkPassengersResult(bundle)
        );

        // Listen for old LinkPassengersDialog (backward compatibility)
        getParentFragmentManager().setFragmentResultListener(
                LinkPassengersDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> handleLinkPassengersResultLegacy(bundle)
        );
    }

    private void observeViewModel() {
        // Observe vehicles
        viewModel.getVehicles().observe(getViewLifecycleOwner(), this::updateVehicleMarkers);

        // Observe vehicle counts
        viewModel.getVehicleCounts().observe(getViewLifecycleOwner(), this::updateVehicleCounts);

        // Observe active ride
        viewModel.getActiveRide().observe(getViewLifecycleOwner(), this::showActiveRideCard);

        // Observe has active ride
        viewModel.getHasActiveRide().observe(getViewLifecycleOwner(), hasActiveRide -> {
            if (hasActiveRide != null && hasActiveRide) {
                // Optionally disable request ride button
                // binding.btnRequestRide.setEnabled(false);
            }
        });

        // Observe ride creation state
        viewModel.getRideCreationState().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case SUCCESS:
                    handleRideCreationSuccess(resource.getData());
                    break;
                case ERROR:
                    handleRideCreationError(resource.getMessage());
                    break;
                case LOADING:
                    showLoading(true);
                    break;
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLoading);

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
    }

    private void setupVehicleRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                viewModel.fetchActiveVehicles();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }

    private void updateVehicleMarkers(List<VehicleLocationResponse> vehicles) {
        if (vehicles == null || binding == null) return;

        // Remove existing vehicle markers
        for (Marker marker : vehicleMarkers) {
            binding.map.getOverlays().remove(marker);
        }
        vehicleMarkers.clear();

        // Add new markers for each vehicle
        for (VehicleLocationResponse vehicle : vehicles) {
            Marker marker = new Marker(binding.map);
            GeoPoint position = new GeoPoint(vehicle.getLatitude(), vehicle.getLongitude());
            marker.setPosition(position);
            marker.setTitle(vehicle.getVehicleType());
            marker.setSnippet(vehicle.isAvailable() ? "Available" : "Occupied");

            // Set marker icon based on availability
            Drawable icon = vehicle.isAvailable()
                    ? ContextCompat.getDrawable(requireContext(), R.drawable.ic_vehicle_available)
                    : ContextCompat.getDrawable(requireContext(), R.drawable.ic_vehicle_occupied);

            if (icon != null) {
                marker.setIcon(icon);
            }

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            binding.map.getOverlays().add(marker);
            vehicleMarkers.add(marker);
        }

        binding.map.invalidate();
    }

    private void updateVehicleCounts(PassengerHomeViewModel.VehicleCounts counts) {
        if (counts == null || binding == null) return;

        binding.tvAvailableCount.setText(String.format("Available (%d)", counts.getAvailable()));
        binding.tvOccupiedCount.setText(String.format("Occupied (%d)", counts.getOccupied()));
    }

    private void showActiveRideCard(RideResponse ride) {
        if (binding == null || ride == null) return;

        View card = binding.getRoot().findViewById(R.id.passenger_active_ride_card);
        if (card == null) return;
        card.setVisibility(View.VISIBLE);

        View root = binding.getRoot();

        // Update status
        View statusView = root.findViewById(R.id.passenger_active_ride_status);
        if (statusView instanceof android.widget.TextView) {
            ((android.widget.TextView) statusView).setText(ride.getDisplayStatus().toUpperCase());
        }

        // Update route
        View routeView = root.findViewById(R.id.passenger_active_ride_route);
        if (routeView instanceof android.widget.TextView) {
            String from = ride.getEffectiveStartLocation() != null
                    ? ride.getEffectiveStartLocation().getAddress() : "—";
            String to = ride.getEffectiveEndLocation() != null
                    ? ride.getEffectiveEndLocation().getAddress() : "—";
            String fromTrunc = from != null && from.length() > 25 ? from.substring(0, 25) + "..." : from;
            String toTrunc = to != null && to.length() > 25 ? to.substring(0, 25) + "..." : to;
            ((android.widget.TextView) routeView).setText(fromTrunc + " → " + toTrunc);
        }

        // Setup button click
        View btn = root.findViewById(R.id.btn_view_passenger_active_ride);
        if (btn != null) {
            btn.setOnClickListener(v -> navigateToActiveRide(ride.getId()));
        }
    }

    private void handleRideOrderResult(Bundle bundle) {
        ArrayList<String> locations = bundle.getStringArrayList(OrderRideDialog.KEY_STOPS);
        String vehicle = bundle.getString(OrderRideDialog.KEY_VEHICLE_TYPE);
        boolean baby = bundle.getBoolean(OrderRideDialog.KEY_BABY_TRANSPORT, false);
        boolean pet = bundle.getBoolean(OrderRideDialog.KEY_PET_TRANSPORT, false);

        if (locations != null && !locations.isEmpty()) {
            rideLocations = locations;
            vehicleType = vehicle;
            babyTransport = baby;
            petTransport = pet;

            mapRenderer.showRideByAddresses(rideLocations);
            createRideRequest();
        }
    }

    private void handleRideOrderResultLegacy(Bundle bundle) {
        ArrayList<String> locations = bundle.getStringArrayList(RequestRideFormDialog.KEY_LOCATIONS);
        String vehicle = bundle.getString(RequestRideFormDialog.KEY_VEHICLE_TYPE);
        boolean baby = bundle.getBoolean(RequestRideFormDialog.KEY_BABY_TRANSPORT, false);
        boolean pet = bundle.getBoolean(RequestRideFormDialog.KEY_PET_TRANSPORT, false);

        if (locations != null && !locations.isEmpty()) {
            rideLocations = locations;
            vehicleType = vehicle;
            babyTransport = baby;
            petTransport = pet;

            mapRenderer.showRideByAddresses(rideLocations);
            createRideRequest();
        }
    }

    private void handleLinkPassengersResult(Bundle bundle) {
        ArrayList<String> emails = bundle.getStringArrayList(LinkPassengersNewDialog.KEY_EMAILS);
        if (emails != null) {
            passengerEmails = emails;
            Toast.makeText(requireContext(),
                    emails.size() + " passenger(s) linked",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLinkPassengersResultLegacy(Bundle bundle) {
        ArrayList<String> emails = bundle.getStringArrayList(LinkPassengersDialog.KEY_EMAILS);
        if (emails != null) {
            passengerEmails = emails;
            Toast.makeText(requireContext(),
                    emails.size() + " passenger(s) linked",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void createRideRequest() {
        if (rideLocations == null || rideLocations.size() < 2) {
            showError("At least start and destination are required");
            return;
        }

        showLoading(true);

        // Geocode addresses in background thread
        new Thread(() -> {
            try {
                // First location is start, last is destination
                String startAddress = rideLocations.get(0);
                String destinationAddress = rideLocations.get(rideLocations.size() - 1);

                // Geocode start and destination
                GeoPoint startPoint = mapRenderer.geocodeLocation(startAddress);
                GeoPoint destPoint = mapRenderer.geocodeLocation(destinationAddress);

                if (startPoint == null || destPoint == null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Could not find address locations");
                    });
                    return;
                }

                // Create LocationDto for start and destination
                LocationDto start = new LocationDto(startAddress, startPoint.getLatitude(), startPoint.getLongitude());
                LocationDto destination = new LocationDto(destinationAddress, destPoint.getLatitude(), destPoint.getLongitude());

                // Build CreateRideRequest DTO
                CreateRideRequest request = new CreateRideRequest(start, destination);

                // Add intermediate stops (if any)
                if (rideLocations.size() > 2) {
                    List<LocationDto> stops = new ArrayList<>();
                    for (int i = 1; i < rideLocations.size() - 1; i++) {
                        String stopAddress = rideLocations.get(i);
                        GeoPoint stopPoint = mapRenderer.geocodeLocation(stopAddress);

                        if (stopPoint != null) {
                            stops.add(new LocationDto(stopAddress, stopPoint.getLatitude(), stopPoint.getLongitude()));
                        } else {
                            Log.e(TAG, "Could not geocode stop: " + stopAddress);
                        }
                    }
                    request.setStops(stops);
                }

                // Set passenger emails
                if (passengerEmails != null && !passengerEmails.isEmpty()) {
                    request.setPassengerEmails(passengerEmails);
                }

                // Set ride requirements
                RideRequirements requirements = new RideRequirements(
                        vehicleType != null ? vehicleType : "STANDARD",
                        babyTransport,
                        petTransport
                );
                request.setRequirements(requirements);

                // Call ViewModel to create ride on main thread
                requireActivity().runOnUiThread(() -> {
                    viewModel.createRide(request);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating ride request", e);
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error preparing ride request");
                });
            }
        }).start();
    }

    private void handleRideCreationSuccess(RideResponse ride) {
        showLoading(false);

        if (ride != null) {
            double price = ride.getEstimatedCost() != null ? ride.getEstimatedCost() : 0.0;
            showRideCreatedDialog(price);
            Toast.makeText(requireContext(), "Ride created successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRideCreationError(String errorMessage) {
        showLoading(false);
        showError(errorMessage);
    }

    private void showLoading(Boolean isLoading) {
        if (binding == null || isLoading == null) return;

        binding.btnRequestRide.setEnabled(!isLoading);
        binding.btnLinkPassengers.setEnabled(!isLoading);
        // If you have a progress bar: binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void openRequestRideDialog() {
        OrderRideDialog dialog = OrderRideDialog.newInstance();
        dialog.show(getParentFragmentManager(), "OrderRideDialog");
    }

    private void openLinkPassengersDialog() {
        LinkPassengersNewDialog dialog = LinkPassengersNewDialog.newInstance();
        dialog.show(getParentFragmentManager(), "LinkPassengersDialog");
    }

    private void showRideCreatedDialog(double price) {
        RideCreatedDialog dialog = RideCreatedDialog.newInstance(price, passengerEmails);
        dialog.show(getParentFragmentManager(), "RideCreatedDialog");
    }

    private void navigateToActiveRide(Long rideId) {
        Bundle args = new Bundle();
        args.putLong("rideId", rideId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_nav_passenger_home_to_nav_active_ride, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.map.onResume();

        // Start fetching vehicles
        viewModel.fetchActiveVehicles();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.map.onPause();

        // Stop refreshing
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }

        binding = null;
    }
}
