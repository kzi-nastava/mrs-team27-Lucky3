package com.example.mobile.ui.ride;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.ui.ride.ReportInconsistencyDialog;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActiveRideFragment extends Fragment {

    private static final String TAG = "ActiveRide";
    private static final double STOP_COMPLETION_THRESHOLD_METERS = 50.0;
    private static final long LOCATION_POLL_INTERVAL = 10_000; // 10s
    private static final long RIDE_POLL_INTERVAL = 15_000;     // 15s

    private SharedPreferencesManager preferencesManager;
    private RideResponse ride;
    private long rideId = -1;

    private MapView mapView;
    private RideMapRenderer mapRenderer;

    // Map overlays
    private Marker vehicleMarker;
    private Polyline approachRoute;
    private Polyline rideRouteOverlay;
    private final List<Marker> mapMarkers = new ArrayList<>();
    private boolean mapCameraInitialized = false;
    private boolean yellowRouteNeedsRedraw = true;

    // Stop auto-completion
    private final Set<Integer> pendingStopCompletions = new HashSet<>();

    // Remaining distance/time from OSRM route data (updated in drawRoutes)
    private volatile double remainingDistanceKm = -1;
    private volatile double remainingTimeMin = -1;
    private volatile double lastYellowDistanceKm = 0;
    // Route distance/time for non-IN_PROGRESS rides (only the ride route, no approach)
    private volatile double routeDistanceKm = -1;
    private volatile double routeTimeMin = -1;

    // Polling
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private boolean isPollingActive = false;

    private TextView tvStatusBadge;
    private TextView tvRideType, tvCost, tvCostLabel;
    private TextView tvTimeLabel, tvTimeValue, tvDistanceLabel, tvDistanceValue;
    private LinearLayout rideInfoCard, routeStopsContainer;
    private LinearLayout driverInfoCard, passengerInfoCard, cancellationInfoCard;
    private TextView tvDriverName, tvDriverVehicle;
    private LinearLayout passengersList;
    private TextView tvCancelledBy, tvCancellationReason;
    private MaterialButton btnDriverCancel, btnPassengerCancel;
    private MaterialButton btnReportInconsistency, btnPanic;
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
        tvStatusBadge = root.findViewById(R.id.tv_status_badge);
        tvRideType = root.findViewById(R.id.tv_ride_type);
        tvCost = root.findViewById(R.id.tv_cost);
        tvCostLabel = root.findViewById(R.id.tv_cost_label);
        tvTimeLabel = root.findViewById(R.id.tv_time_label);
        tvTimeValue = root.findViewById(R.id.tv_time_value);
        tvDistanceLabel = root.findViewById(R.id.tv_distance_label);
        tvDistanceValue = root.findViewById(R.id.tv_distance_value);
        rideInfoCard = root.findViewById(R.id.ride_info_card);
        routeStopsContainer = root.findViewById(R.id.route_stops_container);
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
        btnReportInconsistency = root.findViewById(R.id.btn_report_inconsistency);
        btnPanic = root.findViewById(R.id.btn_panic);
        actionButtons = root.findViewById(R.id.action_buttons);

        // Set PANIC button text with emoji
        btnPanic.setText("\uD83E\uDD2F PANIC");
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
        // Update ride info card
        boolean isInProgress = "IN_PROGRESS".equals(ride.getStatus());
        rideInfoCard.setVisibility(View.VISIBLE);

        // Ride type
        String vehicleType = ride.getVehicleType();
        if (vehicleType != null) {
            tvRideType.setText(vehicleType.toUpperCase());
        }

        // Cost
        if (isInProgress) {
            tvCostLabel.setText("Current Cost");
            double cost = ride.getTotalCost() != null ? ride.getTotalCost() : 0;
            tvCost.setText(String.format(Locale.US, "RSD %.0f", cost));
        } else {
            tvCostLabel.setText("Est. Cost");
            double cost = ride.getEffectiveCost();
            tvCost.setText(String.format(Locale.US, "RSD %.0f", cost));
        }

        // Time & Distance
        if (isInProgress) {
            tvTimeLabel.setText("Time Left");
            tvDistanceLabel.setText("Distance Left");
            // Values come from OSRM route data (updated in drawRoutes)
            if (remainingDistanceKm >= 0) {
                updateRemainingTimeDistance();
            } else {
                tvTimeValue.setText("—");
                tvDistanceValue.setText("—");
            }
        } else {
            tvTimeLabel.setText("Est. Time");
            tvDistanceLabel.setText("Distance");
            // Values come from OSRM route data (yellow route only, no approach)
            if (routeDistanceKm >= 0) {
                updateRouteTimeDistance();
            } else {
                // Fallback to backend estimate until OSRM data arrives
                Integer estimatedTime = ride.getEstimatedTimeInMinutes();
                if (estimatedTime != null) {
                    tvTimeValue.setText(estimatedTime + " min");
                } else {
                    tvTimeValue.setText("—");
                }
                double distance = ride.getEffectiveDistance();
                tvDistanceValue.setText(String.format(Locale.US, "%.1f km", distance));
            }
        }

        // Build route stops dynamically
        buildRouteStops();
    }

    /**
     * Updates Time Left and Distance Left labels from OSRM route data.
     * Called from updateRoute() and from drawRoutes() on the main thread.
     */
    private void updateRemainingTimeDistance() {
        if (remainingDistanceKm >= 0) {
            tvDistanceValue.setText(String.format(Locale.US, "%.1f km", remainingDistanceKm));
        } else {
            tvDistanceValue.setText("—");
        }
        if (remainingTimeMin >= 0) {
            int mins = (int) Math.round(remainingTimeMin);
            tvTimeValue.setText(mins < 1 ? "< 1 min" : mins + " min");
        } else {
            tvTimeValue.setText("—");
        }
    }

    /**
     * Updates Est. Time and Distance for non-IN_PROGRESS rides from OSRM route data.
     * Uses only the ride route (yellow), excludes approach distance.
     */
    private void updateRouteTimeDistance() {
        if (routeDistanceKm >= 0) {
            tvDistanceValue.setText(String.format(Locale.US, "%.1f km", routeDistanceKm));
        } else {
            tvDistanceValue.setText("—");
        }
        if (routeTimeMin >= 0) {
            int mins = (int) Math.round(routeTimeMin);
            tvTimeValue.setText(mins < 1 ? "< 1 min" : mins + " min");
        } else {
            tvTimeValue.setText("—");
        }
    }

    /**
     * Builds the route stops UI dynamically:
     * - Start: always green dot
     * - Intermediate stops: green if completed, gray if pending
     * - End: always red dot
     * Connecting dashed lines between each stop.
     */
    private void buildRouteStops() {
        routeStopsContainer.removeAllViews();
        Set<Integer> completed = ride.getCompletedStopIndexes();

        // Start location
        LocationDto start = ride.getEffectiveStartLocation();
        if (start != null) {
            addRouteStopRow("Start", start.getAddress(), R.drawable.bg_dot_green,
                    getResources().getColor(R.color.gray_400, null));
        }

        // Intermediate stops
        List<LocationDto> stops = ride.getStops();
        if (stops != null) {
            for (int i = 0; i < stops.size(); i++) {
                // Add connecting line before each stop
                addConnectionLine();

                boolean isCompleted = completed.contains(i);
                int dotDrawable = isCompleted ? R.drawable.bg_dot_green : R.drawable.bg_dot_gray;
                int labelColor = isCompleted
                        ? getResources().getColor(R.color.green_500, null)
                        : getResources().getColor(R.color.gray_400, null);
                String label = "Stop " + (i + 1) + (isCompleted ? " ✓" : "");
                addRouteStopRow(label, stops.get(i).getAddress(), dotDrawable, labelColor);
            }
        }

        // Add connecting line before destination
        addConnectionLine();

        // End location
        LocationDto end = ride.getEffectiveEndLocation();
        if (end != null) {
            addRouteStopRow("End", end.getAddress(), R.drawable.bg_dot_red,
                    getResources().getColor(R.color.gray_400, null));
        }
    }

    private void addRouteStopRow(String label, String address, int dotDrawable, int labelColor) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        row.setPadding(0, 4, 0, 4);

        // Dot + label column
        LinearLayout dotColumn = new LinearLayout(requireContext());
        dotColumn.setOrientation(LinearLayout.VERTICAL);
        dotColumn.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams dotColParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dotColParams.setMarginEnd(12);
        dotColumn.setLayoutParams(dotColParams);

        // Dot
        View dot = new View(requireContext());
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                dpToPx(10), dpToPx(10));
        dotParams.topMargin = dpToPx(4);
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(dotDrawable);
        dotColumn.addView(dot);

        row.addView(dotColumn);

        // Text column (label + address)
        LinearLayout textColumn = new LinearLayout(requireContext());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // Label (Start, Stop 1, End)
        TextView labelView = new TextView(requireContext());
        labelView.setText(label);
        labelView.setTextColor(labelColor);
        labelView.setTextSize(11);
        textColumn.addView(labelView);

        // Address
        TextView addressView = new TextView(requireContext());
        addressView.setText(address != null ? address : "—");
        addressView.setTextColor(getResources().getColor(R.color.white, null));
        addressView.setTextSize(14);
        textColumn.addView(addressView);

        row.addView(textColumn);
        routeStopsContainer.addView(row);
    }

    private void addConnectionLine() {
        View line = new View(requireContext());
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                dpToPx(2), dpToPx(20));
        lineParams.setMarginStart(dpToPx(4));
        lineParams.topMargin = dpToPx(2);
        lineParams.bottomMargin = dpToPx(2);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(getResources().getColor(R.color.gray_700, null));
        routeStopsContainer.addView(line);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateMap() {
        if (mapRenderer == null || ride == null || mapView == null) return;
        try {
            LocationDto start = ride.getEffectiveStartLocation();
            LocationDto end = ride.getEffectiveEndLocation();
            if (start == null || end == null) return;

            // Clear old overlays and place fresh markers
            clearRouteOverlays();
            placeRideMarkers();

            // Flag yellow route for redraw
            yellowRouteNeedsRedraw = true;

            // Start polling for vehicle location + ride status
            startPolling();
        } catch (Exception e) {
            Log.e(TAG, "Error updating map", e);
        }
    }

    // ========== Polling ==========

    private void startPolling() {
        if (isPollingActive) return;
        isPollingActive = true;
        pollingHandler.postDelayed(locationRunnable, 0); // immediate first poll
        pollingHandler.postDelayed(rideRunnable, RIDE_POLL_INTERVAL);
    }

    private void stopPolling() {
        isPollingActive = false;
        pollingHandler.removeCallbacks(locationRunnable);
        pollingHandler.removeCallbacks(rideRunnable);
    }

    private final Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingActive || !isAdded()) return;
            pollVehicleLocation();
            pollingHandler.postDelayed(this, LOCATION_POLL_INTERVAL);
        }
    };

    private final Runnable rideRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingActive || !isAdded()) return;
            refreshRide();
            pollingHandler.postDelayed(this, RIDE_POLL_INTERVAL);
        }
    };

    private void refreshRide() {
        if (rideId <= 0) return;
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    ride = response.body();
                    updateHeader();
                    updateRoute();
                    updateActionButtons();
                    updateCancellationInfo();
                }
            }
            @Override
            public void onFailure(Call<RideResponse> call, Throwable t) {
                Log.e(TAG, "Failed to refresh ride", t);
            }
        });
    }

    // ========== Map overlays ==========

    private void clearRouteOverlays() {
        if (mapView == null) return;
        if (approachRoute != null) {
            mapView.getOverlays().remove(approachRoute);
            approachRoute = null;
        }
        if (rideRouteOverlay != null) {
            mapView.getOverlays().remove(rideRouteOverlay);
            rideRouteOverlay = null;
        }
        for (Marker m : mapMarkers) {
            mapView.getOverlays().remove(m);
        }
        mapMarkers.clear();
    }

    /**
     * Places markers. Rules:
     * - IN_PROGRESS: hide start dot, hide completed stop dots
     * - Other: show all dots
     */
    private void placeRideMarkers() {
        if (ride == null || mapView == null) return;
        boolean isInProgress = "IN_PROGRESS".equals(ride.getStatus());
        Set<Integer> completed = ride.getCompletedStopIndexes();

        // Pickup — hide for IN_PROGRESS
        if (!isInProgress && ride.getEffectiveStartLocation() != null) {
            addMapMarker(new GeoPoint(
                    ride.getEffectiveStartLocation().getLatitude(),
                    ride.getEffectiveStartLocation().getLongitude()),
                    "Pickup", R.drawable.ic_dot_green);
        }

        // Stops — hide completed
        if (ride.getStops() != null) {
            for (int i = 0; i < ride.getStops().size(); i++) {
                if (completed.contains(i)) continue;
                LocationDto stop = ride.getStops().get(i);
                addMapMarker(new GeoPoint(stop.getLatitude(), stop.getLongitude()),
                        stop.getAddress() != null ? stop.getAddress() : "Stop " + (i + 1),
                        R.drawable.ic_dot_gray);
            }
        }

        // Destination — always show
        if (ride.getEffectiveEndLocation() != null) {
            addMapMarker(new GeoPoint(
                    ride.getEffectiveEndLocation().getLatitude(),
                    ride.getEffectiveEndLocation().getLongitude()),
                    "Destination", R.drawable.ic_dot_red);
        }

        mapView.invalidate();
    }

    private void addMapMarker(GeoPoint point, String title, int iconRes) {
        if (mapView == null) return;
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setIcon(requireContext().getDrawable(iconRes));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mapView.getOverlays().add(marker);
        mapMarkers.add(marker);
    }

    // ========== Vehicle location + routes ==========

    private void pollVehicleLocation() {
        if (ride == null || mapView == null) return;
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        ClientUtils.vehicleService.getActiveVehicles()
                .enqueue(new Callback<List<VehicleLocationResponse>>() {
                    @Override
                    public void onResponse(Call<List<VehicleLocationResponse>> call,
                                           Response<List<VehicleLocationResponse>> response) {
                        if (!isAdded() || mapView == null) return;
                        if (!response.isSuccessful() || response.body() == null) return;

                        // Find the ride's driver vehicle
                        Long driverId = ride.getDriverId();
                        if (driverId == null && ride.getDriver() != null) {
                            driverId = ride.getDriver().getId();
                        }
                        if (driverId == null) return;

                        for (VehicleLocationResponse v : response.body()) {
                            if (driverId.equals(v.getDriverId())) {
                                updateVehicleOnMap(v);
                                drawRoutes(v);
                                // Only driver auto-completes stops
                                if ("DRIVER".equals(preferencesManager.getUserRole())) {
                                    checkStopCompletion(v);
                                }
                                break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VehicleLocationResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to poll vehicle location", t);
                    }
                });
    }

    private void updateVehicleOnMap(VehicleLocationResponse vehicle) {
        if (mapView == null) return;
        GeoPoint vehiclePoint = new GeoPoint(vehicle.getLatitude(), vehicle.getLongitude());

        if (vehicleMarker == null) {
            vehicleMarker = new Marker(mapView);
            vehicleMarker.setTitle("Vehicle");
            vehicleMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            vehicleMarker.setIcon(requireContext().getDrawable(R.drawable.ic_dot_blue));
            mapView.getOverlays().add(vehicleMarker);
        }

        vehicleMarker.setPosition(vehiclePoint);

        // Center map on vehicle the first time
        if (!mapCameraInitialized) {
            mapView.getController().setCenter(vehiclePoint);
            mapView.getController().setZoom(16.0);
            mapCameraInitialized = true;
        }

        mapView.invalidate();
    }

    /**
     * Blue route updates every poll. Yellow only when yellowRouteNeedsRedraw is set.
     */
    private void drawRoutes(VehicleLocationResponse vehicle) {
        if (ride == null || mapView == null) return;

        GeoPoint vehiclePoint = new GeoPoint(vehicle.getLatitude(), vehicle.getLongitude());
        boolean isInProgress = "IN_PROGRESS".equals(ride.getStatus());
        boolean redrawYellow = yellowRouteNeedsRedraw;
        if (redrawYellow) yellowRouteNeedsRedraw = false;

        // Build full waypoints: pickup → stops → destination
        List<GeoPoint> allWaypoints = new ArrayList<>();
        if (ride.getEffectiveStartLocation() != null) {
            allWaypoints.add(new GeoPoint(
                    ride.getEffectiveStartLocation().getLatitude(),
                    ride.getEffectiveStartLocation().getLongitude()));
        }
        if (ride.getStops() != null) {
            for (LocationDto stop : ride.getStops()) {
                allWaypoints.add(new GeoPoint(stop.getLatitude(), stop.getLongitude()));
            }
        }
        if (ride.getEffectiveEndLocation() != null) {
            allWaypoints.add(new GeoPoint(
                    ride.getEffectiveEndLocation().getLatitude(),
                    ride.getEffectiveEndLocation().getLongitude()));
        }
        if (allWaypoints.size() < 2) return;

        ArrayList<GeoPoint> blueWaypoints = new ArrayList<>();
        ArrayList<GeoPoint> yellowWaypoints = new ArrayList<>();

        if (isInProgress) {
            Set<Integer> completed = ride.getCompletedStopIndexes();
            int stopsCount = ride.getStops() != null ? ride.getStops().size() : 0;
            int nextTargetIdx = allWaypoints.size() - 1;
            for (int i = 0; i < stopsCount; i++) {
                if (!completed.contains(i)) {
                    nextTargetIdx = i + 1;
                    break;
                }
            }
            blueWaypoints.add(vehiclePoint);
            blueWaypoints.add(allWaypoints.get(nextTargetIdx));
            for (int i = nextTargetIdx; i < allWaypoints.size(); i++) {
                yellowWaypoints.add(allWaypoints.get(i));
            }
        } else {
            blueWaypoints.add(vehiclePoint);
            blueWaypoints.add(allWaypoints.get(0));
            yellowWaypoints.addAll(allWaypoints);
        }

        new Thread(() -> {
            try {
                RoadManager roadManager = new OSRMRoadManager(
                        requireContext().getApplicationContext(), "Lucky3-mobile");

                double blueDistKm = 0;
                Polyline blueOverlay = null;
                if (blueWaypoints.size() >= 2) {
                    Road blueRoad = roadManager.getRoad(blueWaypoints);
                    if (blueRoad.mStatus == Road.STATUS_OK) {
                        blueOverlay = RoadManager.buildRoadOverlay(blueRoad);
                        blueOverlay.setColor(Color.parseColor("#3b82f6"));
                        blueOverlay.setWidth(10f);
                        blueDistKm = blueRoad.mLength;
                    }
                }

                double yellowDistKm = 0;
                Polyline yellowOverlay = null;
                if (redrawYellow && yellowWaypoints.size() >= 2) {
                    Road yellowRoad = roadManager.getRoad(yellowWaypoints);
                    if (yellowRoad.mStatus == Road.STATUS_OK) {
                        yellowOverlay = RoadManager.buildRoadOverlay(yellowRoad);
                        yellowOverlay.setColor(Color.parseColor("#eab308"));
                        yellowOverlay.setWidth(12f);
                        Paint paint = yellowOverlay.getOutlinePaint();
                        paint.setPathEffect(new DashPathEffect(new float[]{30, 20}, 0));
                        yellowDistKm = yellowRoad.mLength;
                    }
                }

                // Update distance/time from OSRM route data
                if (isInProgress) {
                    // IN_PROGRESS: blue (vehicle→next stop) + yellow (remaining route)
                    if (redrawYellow) {
                        lastYellowDistanceKm = yellowDistKm;
                    }
                    remainingDistanceKm = blueDistKm + lastYellowDistanceKm;
                    remainingTimeMin = remainingDistanceKm * 3.2;
                } else {
                    // SCHEDULED/PENDING/ACCEPTED: only yellow route (pickup→stops→destination)
                    if (redrawYellow && yellowDistKm > 0) {
                        routeDistanceKm = yellowDistKm;
                        routeTimeMin = yellowDistKm * 3.2;
                    }
                }

                final Polyline finalBlue = blueOverlay;
                final Polyline finalYellow = yellowOverlay;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mapView == null || !isAdded()) return;

                    if (approachRoute != null) {
                        mapView.getOverlays().remove(approachRoute);
                    }
                    if (finalYellow != null) {
                        if (rideRouteOverlay != null) {
                            mapView.getOverlays().remove(rideRouteOverlay);
                        }
                        rideRouteOverlay = finalYellow;
                        mapView.getOverlays().add(rideRouteOverlay);
                    }
                    if (finalBlue != null) {
                        approachRoute = finalBlue;
                        mapView.getOverlays().add(approachRoute);
                    }

                    // Ensure vehicle marker stays on top of route overlays
                    if (vehicleMarker != null) {
                        mapView.getOverlays().remove(vehicleMarker);
                        mapView.getOverlays().add(vehicleMarker);
                    }

                    mapView.invalidate();

                    // Update time/distance display with fresh route data
                    if (isInProgress) {
                        updateRemainingTimeDistance();
                    } else {
                        updateRouteTimeDistance();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to draw routes", e);
            }
        }).start();
    }

    // ========== Stop Auto-Completion ==========

    private void checkStopCompletion(VehicleLocationResponse vehicle) {
        if (ride == null || !"IN_PROGRESS".equals(ride.getStatus())) return;
        if (ride.getStops() == null || ride.getStops().isEmpty()) return;

        Set<Integer> completed = ride.getCompletedStopIndexes();
        for (int i = 0; i < ride.getStops().size(); i++) {
            if (completed.contains(i) || pendingStopCompletions.contains(i)) continue;

            LocationDto stop = ride.getStops().get(i);
            double meters = haversineMeters(
                    vehicle.getLatitude(), vehicle.getLongitude(),
                    stop.getLatitude(), stop.getLongitude());

            if (meters <= STOP_COMPLETION_THRESHOLD_METERS) {
                pendingStopCompletions.add(i);
                completeStopOnBackend(i);
            }
        }
    }

    private void completeStopOnBackend(int stopIndex) {
        if (ride == null) return;
        String token = "Bearer " + preferencesManager.getToken();

        ClientUtils.rideService.completeStop(ride.getId(), stopIndex, token)
                .enqueue(new Callback<RideResponse>() {
                    @Override
                    public void onResponse(Call<RideResponse> call,
                                           Response<RideResponse> response) {
                        pendingStopCompletions.remove(stopIndex);
                        if (!isAdded()) return;

                        if (response.isSuccessful() && response.body() != null) {
                            ride = response.body();
                            onStopCompleted(stopIndex);
                        } else {
                            Log.e(TAG, "Failed to complete stop " + stopIndex + ": " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<RideResponse> call, Throwable t) {
                        pendingStopCompletions.remove(stopIndex);
                        Log.e(TAG, "Network error completing stop " + stopIndex, t);
                    }
                });
    }

    private void onStopCompleted(int stopIndex) {
        if (!isAdded() || mapView == null) return;

        String stopName = "Stop " + (stopIndex + 1);
        if (ride.getStops() != null && stopIndex < ride.getStops().size()) {
            String addr = ride.getStops().get(stopIndex).getAddress();
            if (addr != null && !addr.isEmpty()) stopName = addr;
        }
        Toast.makeText(requireContext(), "✓ Completed: " + stopName, Toast.LENGTH_SHORT).show();

        // Refresh markers and flag yellow route for redraw
        clearRouteOverlays();
        placeRideMarkers();
        yellowRouteNeedsRedraw = true;

        // Rebuild route stops UI so completed dot turns green
        buildRouteStops();
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
        btnReportInconsistency.setVisibility(View.GONE);
        btnPanic.setVisibility(View.GONE);

        if (ride.isCancelled() || "FINISHED".equals(status)) {
            actionButtons.setVisibility(View.GONE);
            return;
        }

        actionButtons.setVisibility(View.VISIBLE);

        // IN_PROGRESS: show Report Inconsistency (passenger only) + PANIC (both roles)
        if ("IN_PROGRESS".equals(status)) {
            // PANIC button for both roles
            btnPanic.setVisibility(View.VISIBLE);
            // PANIC does nothing (just a button stub as requested)

            // Report Inconsistency for passengers only
            if ("PASSENGER".equals(role) && isCurrentUserPassenger()) {
                btnReportInconsistency.setVisibility(View.VISIBLE);
                btnReportInconsistency.setOnClickListener(v -> {
                    ReportInconsistencyDialog dialog =
                            ReportInconsistencyDialog.newInstance(rideId);
                    dialog.show(getParentFragmentManager(), "report_inconsistency");
                });
            }
            return;
        }

        // Non-IN_PROGRESS: show cancel buttons
        boolean canCancel = "PENDING".equals(status)
                || "ACCEPTED".equals(status)
                || "SCHEDULED".equals(status);

        if (!canCancel) {
            actionButtons.setVisibility(View.GONE);
            return;
        }

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
        if (ride != null) startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
        if (mapView != null) mapView.onDetach();
    }
}
