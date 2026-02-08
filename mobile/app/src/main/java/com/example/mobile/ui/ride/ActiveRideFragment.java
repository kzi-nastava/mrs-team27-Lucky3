package com.example.mobile.ui.ride;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActiveRideFragment extends Fragment {

    private static final String TAG = "ActiveRide";

    private SharedPreferencesManager preferencesManager;
    private RideResponse ride;
    private long rideId = -1;

    private MapView mapView;
    private RideMapRenderer mapRenderer;

    private TextView tvRideId, tvStatusBadge;
    private TextView tvPickupAddress, tvDestinationAddress;
    private TextView tvDistance, tvDuration, tvCost;
    private LinearLayout driverInfoCard, passengerInfoCard, cancellationInfoCard;
    private TextView tvDriverName, tvDriverVehicle;
    private LinearLayout passengersList;
    private TextView tvCancelledBy, tvCancellationReason;
    private MaterialButton btnDriverCancel, btnPassengerCancel;
    private LinearLayout actionButtons;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_active_ride, container, false);
        preferencesManager = new SharedPreferencesManager(requireContext());

        bindViews(root);
        setupMap(root);
        setupBackButton(root);
        setupCancelDialogListeners();

        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId", -1);
        }

        if (rideId > 0) {
            loadRide();
        } else {
            Toast.makeText(getContext(), "Invalid ride ID", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    private void bindViews(View root) {
        tvRideId = root.findViewById(R.id.tv_ride_id);
        tvStatusBadge = root.findViewById(R.id.tv_status_badge);
        tvPickupAddress = root.findViewById(R.id.tv_pickup_address);
        tvDestinationAddress = root.findViewById(R.id.tv_destination_address);
        tvDistance = root.findViewById(R.id.tv_distance);
        tvDuration = root.findViewById(R.id.tv_duration);
        tvCost = root.findViewById(R.id.tv_cost);
        driverInfoCard = root.findViewById(R.id.driver_info_card);
        passengerInfoCard = root.findViewById(R.id.passenger_info_card);
        cancellationInfoCard = root.findViewById(R.id.cancellation_info_card);
        tvDriverName = root.findViewById(R.id.tv_driver_name);
        tvDriverVehicle = root.findViewById(R.id.tv_driver_vehicle);
        passengersList = root.findViewById(R.id.passengers_list);
        tvCancelledBy = root.findViewById(R.id.tv_cancelled_by);
        tvCancellationReason = root.findViewById(R.id.tv_cancellation_reason);
        btnDriverCancel = root.findViewById(R.id.btn_driver_cancel);
        btnPassengerCancel = root.findViewById(R.id.btn_passenger_cancel);
        actionButtons = root.findViewById(R.id.action_buttons);
    }

    private void setupMap(View root) {
        mapView = root.findViewById(R.id.map_view);
        if (mapView != null) {
            mapRenderer = new RideMapRenderer(requireContext(), mapView);
            mapRenderer.initMap();
        }
    }

    private void setupBackButton(View root) {
        root.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupCancelDialogListeners() {
        getParentFragmentManager().setFragmentResultListener(
                DriverCancelRideDialog.REQUEST_KEY, this, (requestKey, result) -> {
                    if (result.getBoolean(DriverCancelRideDialog.KEY_CANCELLED, false)) {
                        onRideCancelled();
                    }
                });

        getParentFragmentManager().setFragmentResultListener(
                PassengerCancelRideDialog.REQUEST_KEY, this, (requestKey, result) -> {
                    if (result.getBoolean(PassengerCancelRideDialog.KEY_CANCELLED, false)) {
                        onRideCancelled();
                    }
                });
    }

    private void loadRide() {
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    ride = response.body();
                    updateUI();
                } else {
                    Log.e(TAG, "Failed to load ride: " + response.code());
                    Toast.makeText(getContext(), "Failed to load ride", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RideResponse> call, Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Network error loading ride", t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (ride == null) return;

        updateHeader();
        updateRoute();
        updateMap();
        updateRoleSpecificCards();
        updateCancellationInfo();
        updateActionButtons();
    }

    private void updateHeader() {
        tvRideId.setText("Ride #" + ride.getId());

        String status = ride.getStatus();
        if (status != null) {
            tvStatusBadge.setText(ride.getDisplayStatus().toUpperCase());
            if (ride.isCancelled()) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
                tvStatusBadge.setTextColor(getResources().getColor(R.color.red_500, null));
            } else if ("FINISHED".equals(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_completed);
                tvStatusBadge.setTextColor(getResources().getColor(R.color.green_500, null));
            } else {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active);
                tvStatusBadge.setTextColor(getResources().getColor(R.color.yellow_500, null));
            }
        }
    }

    private void updateRoute() {
        LocationDto start = ride.getEffectiveStartLocation();
        LocationDto end = ride.getEffectiveEndLocation();

        if (start != null && start.getAddress() != null) {
            tvPickupAddress.setText(start.getAddress());
        }
        if (end != null && end.getAddress() != null) {
            tvDestinationAddress.setText(end.getAddress());
        }

        double distance = ride.getEffectiveDistance();
        tvDistance.setText(String.format(Locale.US, "%.1f km", distance));

        Integer estimatedTime = ride.getEstimatedTimeInMinutes();
        if (estimatedTime != null) {
            tvDuration.setText(estimatedTime + " min");
        }

        double cost = ride.getEffectiveCost();
        tvCost.setText(String.format(Locale.US, "%.0f RSD", cost));
    }

    private void updateMap() {
        if (mapRenderer == null || ride == null) return;
        try {
            LocationDto start = ride.getEffectiveStartLocation();
            LocationDto end = ride.getEffectiveEndLocation();
            if (start != null && end != null) {
                List<GeoPoint> points = new ArrayList<>();
                points.add(new GeoPoint(start.getLatitude(), start.getLongitude()));

                List<LocationDto> stops = ride.getStops();
                if (stops != null) {
                    for (LocationDto stop : stops) {
                        points.add(new GeoPoint(stop.getLatitude(), stop.getLongitude()));
                    }
                }
                points.add(new GeoPoint(end.getLatitude(), end.getLongitude()));

                List<com.example.mobile.models.RoutePointResponse> routePointResponses = new ArrayList<>();
                for (GeoPoint gp : points) {
                    LocationDto loc = new LocationDto("", gp.getLatitude(), gp.getLongitude());
                    com.example.mobile.models.RoutePointResponse rpr = new com.example.mobile.models.RoutePointResponse();
                    rpr.setLocation(loc);
                    routePointResponses.add(rpr);
                }
                mapRenderer.showRoute(routePointResponses);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating map", e);
        }
    }

    private void updateRoleSpecificCards() {
        String role = preferencesManager.getUserRole();
        Long userId = preferencesManager.getUserId();

        if ("DRIVER".equals(role)) {
            passengerInfoCard.setVisibility(View.VISIBLE);
            driverInfoCard.setVisibility(View.GONE);
            populatePassengers();
        } else if ("PASSENGER".equals(role)) {
            driverInfoCard.setVisibility(View.VISIBLE);
            passengerInfoCard.setVisibility(View.GONE);
            populateDriverInfo();
        }
    }

    private void populateDriverInfo() {
        RideResponse.DriverInfo driver = ride.getDriver();
        if (driver == null) {
            tvDriverName.setText("Awaiting driver...");
            tvDriverVehicle.setText("");
            return;
        }

        String name = "";
        if (driver.getName() != null) name += driver.getName();
        if (driver.getSurname() != null) name += " " + driver.getSurname();
        tvDriverName.setText(name.trim().isEmpty() ? "Unknown" : name.trim());

        if (driver.getVehicle() != null) {
            RideResponse.VehicleInfo v = driver.getVehicle();
            String vehicleText = "";
            if (v.getModel() != null) vehicleText += v.getModel();
            if (v.getLicensePlates() != null) vehicleText += " • " + v.getLicensePlates();
            tvDriverVehicle.setText(vehicleText);
        }
    }

    private void populatePassengers() {
        passengersList.removeAllViews();
        List<RideResponse.PassengerInfo> passengers = ride.getPassengers();
        if (passengers == null || passengers.isEmpty()) return;

        for (RideResponse.PassengerInfo p : passengers) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            ImageView icon = new ImageView(requireContext());
            icon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            icon.setImageResource(R.drawable.ic_person);
            icon.setColorFilter(getResources().getColor(R.color.yellow_500, null));
            icon.setPadding(8, 8, 8, 8);
            row.addView(icon);

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.setMarginStart(12);
            textCol.setLayoutParams(lp);

            TextView nameView = new TextView(requireContext());
            nameView.setText(p.getFullName());
            nameView.setTextColor(getResources().getColor(R.color.white, null));
            nameView.setTextSize(14);
            textCol.addView(nameView);

            if (p.getEmail() != null) {
                TextView emailView = new TextView(requireContext());
                emailView.setText(p.getEmail());
                emailView.setTextColor(getResources().getColor(R.color.gray_400, null));
                emailView.setTextSize(12);
                textCol.addView(emailView);
            }

            row.addView(textCol);
            passengersList.addView(row);
        }
    }

    private void updateCancellationInfo() {
        if (!ride.isCancelled()) {
            cancellationInfoCard.setVisibility(View.GONE);
            return;
        }

        cancellationInfoCard.setVisibility(View.VISIBLE);

        String status = ride.getStatus();
        if ("CANCELLED_BY_DRIVER".equals(status)) {
            tvCancelledBy.setText("Cancelled by the driver");
        } else if ("CANCELLED_BY_PASSENGER".equals(status)) {
            tvCancelledBy.setText("Cancelled by the passenger");
        } else {
            tvCancelledBy.setText("Ride was cancelled");
        }

        String reason = ride.getRejectionReason();
        if (reason != null && !reason.trim().isEmpty()) {
            tvCancellationReason.setText("Reason: " + reason);
            tvCancellationReason.setVisibility(View.VISIBLE);
        } else {
            tvCancellationReason.setVisibility(View.GONE);
        }
    }

    private void updateActionButtons() {
        String role = preferencesManager.getUserRole();
        Long userId = preferencesManager.getUserId();
        String status = ride.getStatus();

        btnDriverCancel.setVisibility(View.GONE);
        btnPassengerCancel.setVisibility(View.GONE);

        if (ride.isCancelled() || "FINISHED".equals(status) || "IN_PROGRESS".equals(status)) {
            actionButtons.setVisibility(View.GONE);
            return;
        }

        boolean canCancel = "PENDING".equals(status)
                || "ACCEPTED".equals(status)
                || "SCHEDULED".equals(status);

        if (!canCancel) {
            actionButtons.setVisibility(View.GONE);
            return;
        }

        actionButtons.setVisibility(View.VISIBLE);

        if ("DRIVER".equals(role) && isCurrentUserDriver()) {
            btnDriverCancel.setVisibility(View.VISIBLE);
            btnDriverCancel.setOnClickListener(v -> openDriverCancelDialog());
        } else if ("PASSENGER".equals(role) && isCurrentUserPassenger()) {
            btnPassengerCancel.setVisibility(View.VISIBLE);
            btnPassengerCancel.setOnClickListener(v -> openPassengerCancelDialog());
        }
    }

    private boolean isCurrentUserDriver() {
        if (ride.getDriver() == null) return false;
        Long userId = preferencesManager.getUserId();
        return ride.getDriver().getId() != null && ride.getDriver().getId().equals(userId);
    }

    private boolean isCurrentUserPassenger() {
        Long userId = preferencesManager.getUserId();
        if (ride.getPassengers() == null) return false;
        for (RideResponse.PassengerInfo p : ride.getPassengers()) {
            if (p.getId() != null && p.getId().equals(userId)) return true;
        }
        return false;
    }

    private void openDriverCancelDialog() {
        DriverCancelRideDialog dialog = DriverCancelRideDialog.newInstance(rideId);
        dialog.show(getParentFragmentManager(), "driver_cancel");
    }

    private void openPassengerCancelDialog() {
        PassengerCancelRideDialog dialog = PassengerCancelRideDialog.newInstance(rideId);
        dialog.show(getParentFragmentManager(), "passenger_cancel");
    }

    private void onRideCancelled() {
        Toast.makeText(getContext(), "Ride cancelled successfully", Toast.LENGTH_SHORT).show();

        String role = preferencesManager.getUserRole();
        try {
            if ("DRIVER".equals(role)) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.nav_driver_dashboard);
            } else {
                Navigation.findNavController(requireView())
                        .navigate(R.id.nav_passenger_home);
            }
        } catch (Exception e) {
            Log.e(TAG, "Navigation error after cancel", e);
            Navigation.findNavController(requireView()).navigateUp();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) mapView.onDetach();
    }
}
