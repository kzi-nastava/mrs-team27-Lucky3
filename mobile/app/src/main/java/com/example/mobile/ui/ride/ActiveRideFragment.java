package com.example.mobile.ui.ride;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.viewmodels.ActiveRideViewModel;

import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

public class ActiveRideFragment extends Fragment {

    private static final String TAG = "ActiveRideFragment";

    private ActiveRideViewModel viewModel;

    private MapView mapView;
    private RideMapRenderer mapRenderer;
    private TextView tvRideTitle;
    private TextView tvRideStatus;
    private TextView tvRideId;
    private TextView tvPickupAddress;
    private TextView tvDestinationAddress;
    private TextView tvEstimatedCost;
    private TextView tvVehicleType;
    private TextView tvDistance;
    private TextView tvEta;
    private View driverInfoCard;
    private TextView tvDriverName;
    private TextView tvDriverVehicle;
    private View passengersInfoCard;
    private TextView tvPassengersList;
    private View cancellationInfoCard;
    private TextView tvCancellationReason;
    private Button btnCancelRide;
    private ProgressBar progressBar;

    private long rideId = -1;
    private String userRole;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_active_ride, container, false);

        viewModel = new ViewModelProvider(this).get(ActiveRideViewModel.class);

        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId", -1);
        }
        userRole = viewModel.getUserRole();

        initViews(root);
        setupNavbar(root);
        setupListeners();
        observeViewModel();

        if (rideId > 0) {
            viewModel.loadRide(rideId);
        } else {
            Toast.makeText(requireContext(), "Invalid ride ID", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    private void initViews(View root) {
        mapView = root.findViewById(R.id.map_view);
        mapRenderer = new RideMapRenderer(requireActivity(), mapView);
        mapRenderer.initMap();

        tvRideTitle = root.findViewById(R.id.tv_ride_title);
        tvRideStatus = root.findViewById(R.id.tv_ride_status);
        tvRideId = root.findViewById(R.id.tv_ride_id);
        tvPickupAddress = root.findViewById(R.id.tv_pickup_address);
        tvDestinationAddress = root.findViewById(R.id.tv_destination_address);
        tvEstimatedCost = root.findViewById(R.id.tv_estimated_cost);
        tvVehicleType = root.findViewById(R.id.tv_vehicle_type);
        tvDistance = root.findViewById(R.id.tv_distance);
        tvEta = root.findViewById(R.id.tv_eta);
        driverInfoCard = root.findViewById(R.id.driver_info_card);
        tvDriverName = root.findViewById(R.id.tv_driver_name);
        tvDriverVehicle = root.findViewById(R.id.tv_driver_vehicle);
        passengersInfoCard = root.findViewById(R.id.passengers_info_card);
        tvPassengersList = root.findViewById(R.id.tv_passengers_list);
        cancellationInfoCard = root.findViewById(R.id.cancellation_info_card);
        tvCancellationReason = root.findViewById(R.id.tv_cancellation_reason);
        btnCancelRide = root.findViewById(R.id.btn_cancel_ride);
        progressBar = root.findViewById(R.id.progress_bar);
    }

    private void setupNavbar(View root) {
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v ->
                    ((com.example.mobile.MainActivity) requireActivity()).openDrawer());
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Active Ride");
        }
    }

    private void setupListeners() {
        btnCancelRide.setOnClickListener(v -> openCancelDialog());

        getParentFragmentManager().setFragmentResultListener(
                CancelRideDialog.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    String reason = bundle.getString(CancelRideDialog.KEY_REASON, "");
                    viewModel.cancelRide(rideId, reason);
                });
    }

    private void openCancelDialog() {
        boolean isDriver = "DRIVER".equals(userRole);
        CancelRideDialog dialog = CancelRideDialog.newInstance(isDriver);
        dialog.show(getParentFragmentManager(), "CancelRideDialog");
    }

    private void observeViewModel() {
        viewModel.getRide().observe(getViewLifecycleOwner(), this::bindRide);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getCancelSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), "Ride cancelled successfully", Toast.LENGTH_SHORT).show();
                navigateAfterCancel();
            }
        });
    }

    private void bindRide(RideResponse ride) {
        if (ride == null) return;

        tvRideId.setText("Ride #" + ride.getId());
        tvRideStatus.setText(ride.getDisplayStatus().toUpperCase());
        tvRideTitle.setText("DRIVER".equals(userRole) ? "Your Ride" : "Active Ride");

        LocationDto pickup = ride.getEffectiveStartLocation();
        LocationDto dest = ride.getEffectiveEndLocation();

        if (pickup != null && pickup.getAddress() != null) {
            tvPickupAddress.setText(pickup.getAddress());
        }
        if (dest != null && dest.getAddress() != null) {
            tvDestinationAddress.setText(dest.getAddress());
        }

        Double cost = ride.getEstimatedCost();
        if (cost != null) {
            tvEstimatedCost.setText(Math.round(cost) + " RSD");
        }

        String vType = ride.getVehicleType();
        if (vType != null) {
            tvVehicleType.setText(vType);
        }

        Double dist = ride.getEffectiveDistance();
        if (dist != null && dist > 0) {
            tvDistance.setText(String.format("%.1f km", dist));
        }

        Integer eta = ride.getEstimatedTimeInMinutes();
        if (eta != null) {
            tvEta.setText(eta + " min");
        }

        showMapRoute(pickup, dest);
        bindRoleSpecificInfo(ride);
        updateCancelButton(ride);
        showCancellationInfo(ride);
    }

    private void showMapRoute(LocationDto pickup, LocationDto dest) {
        if (pickup == null || dest == null) return;
        if (pickup.getAddress() == null || dest.getAddress() == null) return;
        try {
            List<String> addresses = new ArrayList<>();
            addresses.add(pickup.getAddress());
            addresses.add(dest.getAddress());
            mapRenderer.showRideByAddresses(addresses);
        } catch (Exception e) {
            Log.e(TAG, "Error showing map route", e);
        }
    }

    private void bindRoleSpecificInfo(RideResponse ride) {
        if ("PASSENGER".equals(userRole)) {
            driverInfoCard.setVisibility(View.VISIBLE);
            passengersInfoCard.setVisibility(View.GONE);
            RideResponse.DriverInfo driver = ride.getDriver();
            if (driver != null) {
                String driverName = "";
                if (driver.getName() != null) driverName += driver.getName();
                if (driver.getSurname() != null) driverName += " " + driver.getSurname();
                tvDriverName.setText(driverName.trim().isEmpty() ? "Assigned driver" : driverName.trim());

                RideResponse.VehicleInfo vehicle = driver.getVehicle();
                if (vehicle != null) {
                    String vehicleInfo = "";
                    if (vehicle.getModel() != null) vehicleInfo += vehicle.getModel();
                    if (vehicle.getLicensePlates() != null) vehicleInfo += " • " + vehicle.getLicensePlates();
                    tvDriverVehicle.setText(vehicleInfo);
                }
            }
        } else if ("DRIVER".equals(userRole)) {
            driverInfoCard.setVisibility(View.GONE);
            passengersInfoCard.setVisibility(View.VISIBLE);
            List<RideResponse.PassengerInfo> passengers = ride.getPassengers();
            if (passengers != null && !passengers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (RideResponse.PassengerInfo p : passengers) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("• ").append(p.getFullName());
                    if (p.getEmail() != null) sb.append(" (").append(p.getEmail()).append(")");
                }
                tvPassengersList.setText(sb.toString());
            } else {
                tvPassengersList.setText("No passengers");
            }
        }
    }

    private void updateCancelButton(RideResponse ride) {
        String status = ride.getStatus();
        if (status == null) {
            btnCancelRide.setVisibility(View.GONE);
            return;
        }

        boolean canCancel = false;

        if ("DRIVER".equals(userRole)) {
            canCancel = "PENDING".equals(status) || "ACCEPTED".equals(status) || "SCHEDULED".equals(status);
        } else if ("PASSENGER".equals(userRole)) {
            canCancel = "PENDING".equals(status) || "ACCEPTED".equals(status) || "SCHEDULED".equals(status);
        }

        btnCancelRide.setVisibility(canCancel ? View.VISIBLE : View.GONE);
    }

    private void showCancellationInfo(RideResponse ride) {
        if (ride.isCancelled()) {
            cancellationInfoCard.setVisibility(View.VISIBLE);
            btnCancelRide.setVisibility(View.GONE);

            String reason = ride.getRejectionReason();
            if (reason != null && !reason.trim().isEmpty()) {
                String cancelledBy = "CANCELLED_BY_DRIVER".equals(ride.getStatus()) ? "Driver" : "Passenger";
                tvCancellationReason.setText("Cancelled by " + cancelledBy + ": " + reason);
            } else {
                tvCancellationReason.setText("This ride has been cancelled.");
            }
        } else {
            cancellationInfoCard.setVisibility(View.GONE);
        }
    }

    private void navigateAfterCancel() {
        if (!isAdded()) return;
        try {
            if ("DRIVER".equals(userRole)) {
                Navigation.findNavController(requireView()).navigate(R.id.nav_driver_dashboard);
            } else {
                Navigation.findNavController(requireView()).navigate(R.id.nav_passenger_home);
            }
        } catch (Exception e) {
            Log.e(TAG, "Navigation after cancel failed", e);
            requireActivity().onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
