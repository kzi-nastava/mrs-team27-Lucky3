package com.example.mobile.ui.passenger;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentPassengerHomeBinding;
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.services.VehicleService;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.ClientUtils;
import com.google.android.material.button.MaterialButton;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.Navigation;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerHomeFragment extends Fragment {
    private static final String TAG = "PassengerHomeFragment";
    private static final int REFRESH_INTERVAL_MS = 10000; // 10 seconds

    private FragmentPassengerHomeBinding binding;
    private MapView map;
    private MaterialButton btnRequestRide;
    private MaterialButton btnLinkPassengers;
    private ImageButton btnMenu;
    private TextView tvAvailableCount;
    private TextView tvOccupiedCount;

    private ArrayList<String> rideLocations = new ArrayList<>();
    private ArrayList<String> passengerEmails = new ArrayList<>();
    private String vehicleType;
    private boolean babyTransport;
    private boolean petTransport;

    private RideMapRenderer mapRenderer;
    private final List<Marker> vehicleMarkers = new ArrayList<>();
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    // Vehicle counts
    private int availableCount = 0;
    private int occupiedCount = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPassengerHomeBinding.inflate(inflater, container, false);

        // Get references from new layout
        btnMenu = binding.btnMenu;
        btnRequestRide = binding.btnRequestRide;
        btnLinkPassengers = binding.btnLinkPassengers;
        tvAvailableCount = binding.tvAvailableCount;
        tvOccupiedCount = binding.tvOccupiedCount;

        // Menu button opens drawer
        btnMenu.setOnClickListener(v -> {
            ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
        });

        // Setting up the map
        map = binding.map;
        mapRenderer = new RideMapRenderer(requireActivity(), map);
        mapRenderer.initMap();

        // Show dialog on button click
        btnRequestRide.setOnClickListener(v -> openRequestRideDialog());
        btnLinkPassengers.setOnClickListener(v -> openLinkPassengersDialog());

        // When dialogs are called, this function listens for data inserted in dialog form
        listenRideRequestAndEmails();

        // Setup vehicle refresh
        setupVehicleRefresh();

        return binding.getRoot();
    }

    private void setupVehicleRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchActiveVehicles();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }

    private void fetchActiveVehicles() {
        VehicleService vehicleService = ClientUtils.vehicleService;
        vehicleService.getActiveVehicles().enqueue(new Callback<List<VehicleLocationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<VehicleLocationResponse>> call, 
                                   @NonNull Response<List<VehicleLocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VehicleLocationResponse> vehicles = response.body();
                    updateVehicleMarkers(vehicles);
                    updateVehicleCounts(vehicles);
                } else {
                    Log.e(TAG, "Failed to fetch vehicles: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VehicleLocationResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching vehicles", t);
            }
        });
    }

    private void updateVehicleMarkers(List<VehicleLocationResponse> vehicles) {
        // Remove existing vehicle markers
        for (Marker marker : vehicleMarkers) {
            map.getOverlays().remove(marker);
        }
        vehicleMarkers.clear();

        // Add new markers for each vehicle
        for (VehicleLocationResponse vehicle : vehicles) {
            Marker marker = new Marker(map);
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
            map.getOverlays().add(marker);
            vehicleMarkers.add(marker);
        }

        map.invalidate();
    }

    private void updateVehicleCounts(List<VehicleLocationResponse> vehicles) {
        availableCount = 0;
        occupiedCount = 0;

        for (VehicleLocationResponse vehicle : vehicles) {
            if (vehicle.isAvailable()) {
                availableCount++;
            } else {
                occupiedCount++;
            }
        }

        // Update UI
        if (tvAvailableCount != null) {
            tvAvailableCount.setText(String.format("Available (%d)", availableCount));
        }
        if (tvOccupiedCount != null) {
            tvOccupiedCount.setText(String.format("Occupied (%d)", occupiedCount));
        }
    }

    private void listenRideRequestAndEmails() {
        // Listening for ride order from new OrderRideDialog
        getParentFragmentManager().setFragmentResultListener(
                OrderRideDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> locations =
                            bundle.getStringArrayList(OrderRideDialog.KEY_STOPS);
                    String vehicle = bundle.getString(OrderRideDialog.KEY_VEHICLE_TYPE);
                    boolean baby = bundle.getBoolean(OrderRideDialog.KEY_BABY_TRANSPORT, false);
                    boolean pet = bundle.getBoolean(OrderRideDialog.KEY_PET_TRANSPORT, false);

                    if (locations != null && !locations.isEmpty()) {
                        rideLocations = locations;
                        vehicleType = vehicle;
                        babyTransport = baby;
                        petTransport = pet;

                        mapRenderer.showRideByAddresses(rideLocations);
                        tryBuildRideRequest();
                    }
                });

        // Also listen for old RequestRideFormDialog (for backward compatibility)
        getParentFragmentManager().setFragmentResultListener(
                RequestRideFormDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> locations =
                            bundle.getStringArrayList(RequestRideFormDialog.KEY_LOCATIONS);
                    String vehicle = bundle.getString(RequestRideFormDialog.KEY_VEHICLE_TYPE);
                    boolean baby = bundle.getBoolean(RequestRideFormDialog.KEY_BABY_TRANSPORT, false);
                    boolean pet = bundle.getBoolean(RequestRideFormDialog.KEY_PET_TRANSPORT, false);

                    if (locations != null && !locations.isEmpty()) {
                        rideLocations = locations;
                        vehicleType = vehicle;
                        babyTransport = baby;
                        petTransport = pet;

                        mapRenderer.showRideByAddresses(rideLocations);
                        tryBuildRideRequest();
                    }
                });

        // Listening for emails from new LinkPassengersNewDialog
        getParentFragmentManager().setFragmentResultListener(
                LinkPassengersNewDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> emails =
                            bundle.getStringArrayList(LinkPassengersNewDialog.KEY_EMAILS);
                    if (emails != null) {
                        passengerEmails = emails;
                        Toast.makeText(requireContext(), 
                                emails.size() + " passenger(s) linked", 
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Also listen for old LinkPassengersDialog (for backward compatibility)
        getParentFragmentManager().setFragmentResultListener(
                LinkPassengersDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> emails =
                            bundle.getStringArrayList(LinkPassengersDialog.KEY_EMAILS);
                    if (emails != null) {
                        passengerEmails = emails;
                        Toast.makeText(requireContext(), 
                                emails.size() + " passenger(s) linked", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void tryBuildRideRequest() {
        // TODO: send dto object to backend
        // TODO: send emails, notifications, check if ride can be created...
        onRideCreatedSuccessfully(123);
    }

    private void openRequestRideDialog() {
        // Use the new modern OrderRideDialog
        OrderRideDialog dialog = OrderRideDialog.newInstance();
        dialog.show(getParentFragmentManager(), "OrderRideDialog");
    }

    private void openLinkPassengersDialog() {
        // Use the new modern LinkPassengersNewDialog
        LinkPassengersNewDialog dialog = LinkPassengersNewDialog.newInstance();
        dialog.show(getParentFragmentManager(), "LinkPassengersDialog");
    }

    private void onRideCreatedSuccessfully(double price) {
        RideCreatedDialog dialog =
                RideCreatedDialog.newInstance(price, passengerEmails);
        dialog.show(getParentFragmentManager(), "RideCreatedDialog");
    }

    public void navigateToActiveRide(long rideId) {
        if (!isAdded()) return;
        Bundle args = new Bundle();
        args.putLong("rideId", rideId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_nav_passenger_home_to_nav_active_ride, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        // Start fetching vehicles
        fetchActiveVehicles();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
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