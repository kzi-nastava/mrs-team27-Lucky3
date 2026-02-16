package com.example.mobile.ui.driver;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverDashboardBinding;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.models.DriverStatusResponse;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.ListViewHelper;
import com.example.mobile.utils.SharedPreferencesManager;

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

import com.example.mobile.utils.NavbarHelper;

/**
 * Driver Dashboard — the default landing page for drivers.
 *
 * Shows:
 * - Welcome header with online/offline toggle
 * - Stats grid (earnings, rides, rating, online hours)
 * - Active ride card (checks IN_PROGRESS > ACCEPTED > PENDING > SCHEDULED)
 * - Live map with vehicle location and approach route
 * - Scheduled rides list with batched loading
 */
public class DriverDashboardFragment extends Fragment {

    private static final String TAG = "DriverDashboard";

    private FragmentDriverDashboardBinding binding;
    private SharedPreferencesManager preferencesManager;

    // Map
    private RideMapRenderer mapRenderer;
    private MapView mapView;
    private Marker vehicleMarker;
    private Polyline approachRoute;
    private Polyline rideRouteOverlay;
    private final List<Marker> mapMarkers = new ArrayList<>();
    private boolean mapCameraInitialized = false;
    private boolean yellowRouteNeedsRedraw = true;

    // Stop auto-completion
    private static final double STOP_COMPLETION_THRESHOLD_METERS = 50.0;
    private final Set<Integer> pendingStopCompletions = new HashSet<>();

    // Active ride
    private RideResponse activeRide;

    // Scheduled rides
    private ScheduledRideAdapter scheduledAdapter;
    private final List<RideResponse> scheduledRides = new ArrayList<>();
    private int scheduledPage = 0;
    private static final int SCHEDULED_PAGE_SIZE = 5;
    private int totalScheduledCount = 0;
    private boolean allScheduledLoaded = false;

    // Polling
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private static final long STATS_POLL_INTERVAL = 60_000;   // 60s
    private static final long LOCATION_POLL_INTERVAL = 10_000; // 10s
    private static final long RIDE_POLL_INTERVAL = 15_000;     // 15s
    private boolean isPollingActive = false;

    // Status
    private boolean isOnline = false;
    private boolean isTogglingStatus = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferencesManager = new SharedPreferencesManager(requireContext());

        setupNavbar(root);
        setupStatusToggle(root);
        setupScheduledRidesList();
        setupMap(root);

        loadDriverStatus();
        loadDriverStats();
        loadActiveRide();
        loadScheduledRides();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        startPolling();
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
        binding = null;
    }

    // ========== Setup ==========

    private void setupNavbar(View root) {
        NavbarHelper.setup(this, root, "Dashboard");
    }

    private void setupStatusToggle(View root) {
        SwitchCompat statusSwitch = root.findViewById(R.id.status_switch);
        if (statusSwitch != null) {
            statusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!buttonView.isPressed()) return; // Ignore programmatic changes
                toggleDriverStatus(isChecked);
            });
        }
    }

    private void setupScheduledRidesList() {
        ListView listView = binding.scheduledRidesList;
        scheduledAdapter = new ScheduledRideAdapter(requireContext(), scheduledRides);
        scheduledAdapter.setOnCancelClickListener((ride, position) -> {
            // Cancel button present but logic not implemented per requirements
            Toast.makeText(requireContext(), "Cancel ride #" + ride.getId(), Toast.LENGTH_SHORT).show();
        });
        listView.setAdapter(scheduledAdapter);

        binding.btnShowAllScheduled.setOnClickListener(v -> loadMoreScheduledRides());
    }

    private void setupMap(View root) {
        mapView = root.findViewById(R.id.map_view);
        if (mapView != null) {
            mapRenderer = new RideMapRenderer(requireContext(), mapView);
            mapRenderer.initMap();
        }
    }

    // ========== Polling ==========

    private void startPolling() {
        if (isPollingActive) return;
        isPollingActive = true;

        pollingHandler.postDelayed(statsRunnable, STATS_POLL_INTERVAL);
        pollingHandler.postDelayed(locationRunnable, LOCATION_POLL_INTERVAL);
        pollingHandler.postDelayed(rideRunnable, RIDE_POLL_INTERVAL);
    }

    private void stopPolling() {
        isPollingActive = false;
        pollingHandler.removeCallbacks(statsRunnable);
        pollingHandler.removeCallbacks(locationRunnable);
        pollingHandler.removeCallbacks(rideRunnable);
    }

    private final Runnable statsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingActive || !isAdded()) return;
            loadDriverStats();
            loadDriverStatus();
            pollingHandler.postDelayed(this, STATS_POLL_INTERVAL);
        }
    };

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
            loadActiveRide();
            pollingHandler.postDelayed(this, RIDE_POLL_INTERVAL);
        }
    };

    // ========== Driver Status ==========

    private void loadDriverStatus() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.driverService.getDriverStatus(userId, token)
                .enqueue(new Callback<DriverStatusResponse>() {
                    @Override
                    public void onResponse(Call<DriverStatusResponse> call,
                                           Response<DriverStatusResponse> response) {
                        if (!isAdded() || binding == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            DriverStatusResponse status = response.body();
                            isOnline = status.isActive();
                            preferencesManager.saveDriverStatus(isOnline);
                            updateStatusUI(status);
                        }
                    }

                    @Override
                    public void onFailure(Call<DriverStatusResponse> call, Throwable t) {
                        Log.e(TAG, "Failed to load driver status", t);
                    }
                });
    }

    private void toggleDriverStatus(boolean goOnline) {
        if (isTogglingStatus) return;

        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        isTogglingStatus = true;
        String token = "Bearer " + preferencesManager.getToken();

        ClientUtils.driverService.toggleDriverStatus(userId, goOnline, token)
                .enqueue(new Callback<DriverStatusResponse>() {
                    @Override
                    public void onResponse(Call<DriverStatusResponse> call,
                                           Response<DriverStatusResponse> response) {
                        isTogglingStatus = false;
                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            DriverStatusResponse status = response.body();

                            if (status.isWorkingHoursExceeded() && goOnline) {
                                // Can't go online — show warning, revert switch
                                showHoursWarning(status);
                                updateSwitchSilently(false);
                                isOnline = false;
                                Toast.makeText(requireContext(),
                                        "Working hours limit exceeded. Cannot go online.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                isOnline = status.isActive();
                                preferencesManager.saveDriverStatus(isOnline);
                                updateStatusUI(status);
                            }
                        } else {
                            // Revert on error
                            updateSwitchSilently(!goOnline);
                            Toast.makeText(requireContext(),
                                    "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DriverStatusResponse> call, Throwable t) {
                        isTogglingStatus = false;
                        if (!isAdded() || binding == null) return;
                        updateSwitchSilently(!goOnline);
                        Toast.makeText(requireContext(),
                                "Network error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to toggle status", t);
                    }
                });
    }

    private void updateStatusUI(DriverStatusResponse status) {
        if (binding == null) return;
        View root = binding.getRoot();

        // Welcome text
        String name = preferencesManager.getUserFullName();
        TextView welcomeText = root.findViewById(R.id.welcome_text);
        if (welcomeText != null) {
            String firstName = "Driver";
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split(" ");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    firstName = parts[0];
                }
            }
            welcomeText.setText("Welcome, " + firstName);
        }

        // Status text
        TextView statusText = root.findViewById(R.id.status_text);
        if (statusText != null) {
            if (status.isActive()) {
                statusText.setText("You are currently online");
                statusText.setTextColor(getResources().getColor(R.color.green_500, null));
            } else if (status.isInactiveRequested()) {
                statusText.setText("Going offline after current ride...");
                statusText.setTextColor(getResources().getColor(R.color.yellow_500, null));
            } else {
                statusText.setText("You are currently offline");
                statusText.setTextColor(getResources().getColor(R.color.gray_400, null));
            }
        }

        // Status dot
        View statusDot = root.findViewById(R.id.status_dot);
        if (statusDot != null) {
            statusDot.setBackgroundResource(status.isActive()
                    ? R.drawable.bg_dot_green : R.drawable.bg_dot_red);
        }

        // Status label
        TextView statusLabel = root.findViewById(R.id.status_label);
        if (statusLabel != null) {
            statusLabel.setText(status.isActive() ? "Online" : "Offline");
            statusLabel.setTextColor(getResources().getColor(
                    status.isActive() ? R.color.green_500 : R.color.red_500, null));
        }

        // Switch (silent update)
        updateSwitchSilently(status.isActive());

        // Working hours warning
        if (status.isWorkingHoursExceeded()) {
            showHoursWarning(status);
        } else {
            hideHoursWarning();
        }
    }

    private void updateSwitchSilently(boolean checked) {
        SwitchCompat sw = binding.getRoot().findViewById(R.id.status_switch);
        if (sw != null && sw.isChecked() != checked) {
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(checked);
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!buttonView.isPressed()) return;
                toggleDriverStatus(isChecked);
            });
        }
    }

    private void showHoursWarning(DriverStatusResponse status) {
        if (binding == null) return;
        binding.hoursWarning.setVisibility(View.VISIBLE);
        TextView warningText = binding.getRoot().findViewById(R.id.hours_warning_text);
        if (warningText != null) {
            String msg = "Working hours limit exceeded (" + status.getWorkedHoursToday()
                    + " today). You cannot go online.";
            warningText.setText(msg);
        }
    }

    private void hideHoursWarning() {
        if (binding == null) return;
        binding.hoursWarning.setVisibility(View.GONE);
    }

    // ========== Driver Stats ==========

    private void loadDriverStats() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.driverService.getStats(userId, token)
                .enqueue(new Callback<DriverStatsResponse>() {
                    @Override
                    public void onResponse(Call<DriverStatsResponse> call,
                                           Response<DriverStatsResponse> response) {
                        if (!isAdded() || binding == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            updateStatsUI(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<DriverStatsResponse> call, Throwable t) {
                        Log.e(TAG, "Failed to load stats", t);
                    }
                });
    }

    private void updateStatsUI(DriverStatsResponse stats) {
        if (binding == null) return;
        View root = binding.getRoot();

        TextView earnings = root.findViewById(R.id.stat_earnings);
        if (earnings != null) {
            earnings.setText(String.format(Locale.US, "%.2f RSD", stats.getTotalEarnings()));
        }

        TextView rides = root.findViewById(R.id.stat_rides);
        if (rides != null) {
            rides.setText(String.valueOf(stats.getCompletedRides()));
        }

        TextView rating = root.findViewById(R.id.stat_rating);
        if (rating != null) {
            rating.setText(String.format(Locale.US, "%.2f", stats.getAverageRating()));
        }

        TextView ratingCount = root.findViewById(R.id.stat_rating_count);
        if (ratingCount != null) {
            ratingCount.setText(stats.getTotalRatings() + " ratings");
        }

        TextView hours = root.findViewById(R.id.stat_hours);
        if (hours != null) {
            hours.setText(stats.getOnlineHoursToday());
        }
    }

    // ========== Active Ride ==========

    /**
     * Checks for active rides in priority: IN_PROGRESS > ACCEPTED > PENDING > SCHEDULED
     */
    private void loadActiveRide() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        String token = "Bearer " + preferencesManager.getToken();
        checkRideByStatus(userId, token, "IN_PROGRESS");
    }

    private void checkRideByStatus(Long userId, String token, String status) {
        ClientUtils.rideService.getActiveRides(userId, null, status, 0, 1, token)
                .enqueue(new Callback<PageResponse<RideResponse>>() {
                    @Override
                    public void onResponse(Call<PageResponse<RideResponse>> call,
                                           Response<PageResponse<RideResponse>> response) {
                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null
                                && response.body().getContent() != null
                                && !response.body().getContent().isEmpty()) {
                            activeRide = response.body().getContent().get(0);
                            showActiveRideCard();
                            showMapWithRoute();
                            return;
                        }

                        // Chain to next priority status
                        String nextStatus = getNextPriorityStatus(status);
                        if (nextStatus != null) {
                            checkRideByStatus(userId, token, nextStatus);
                        } else {
                            // No active ride found
                            activeRide = null;
                            hideActiveRideCard();
                            hideMap();
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to check " + status + " rides", t);
                    }
                });
    }

    private String getNextPriorityStatus(String current) {
        switch (current) {
            case "IN_PROGRESS": return "ACCEPTED";
            case "ACCEPTED": return "PENDING";
            case "PENDING": return "SCHEDULED";
            default: return null;
        }
    }

    private void showActiveRideCard() {
        if (binding == null || activeRide == null) return;
        View root = binding.getRoot();

        View card = root.findViewById(R.id.active_ride_card);
        if (card == null) return;
        card.setVisibility(View.VISIBLE);

        // Status badge
        TextView statusView = root.findViewById(R.id.active_ride_status);
        if (statusView != null) {
            statusView.setText(activeRide.getDisplayStatus().toUpperCase());
        }

        // Route
        TextView routeView = root.findViewById(R.id.active_ride_route);
        if (routeView != null) {
            String from = activeRide.getEffectiveStartLocation() != null
                    ? activeRide.getEffectiveStartLocation().getAddress() : "—";
            String to = activeRide.getEffectiveEndLocation() != null
                    ? activeRide.getEffectiveEndLocation().getAddress() : "—";
            routeView.setText(truncate(from, 25) + " → " + truncate(to, 25));
        }

        // Passenger
        TextView passengerView = root.findViewById(R.id.active_ride_passenger);
        if (passengerView != null && activeRide.getPassengers() != null
                && !activeRide.getPassengers().isEmpty()) {
            RideResponse.PassengerInfo p = activeRide.getPassengers().get(0);
            passengerView.setText("Passenger: " + p.getFullName());
        }

        // View Ride button
        View btn = root.findViewById(R.id.btn_view_active_ride);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("rideId", activeRide.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_nav_driver_dashboard_to_nav_active_ride, args);
            });
        }
    }

    private void hideActiveRideCard() {
        if (binding == null) return;
        View card = binding.getRoot().findViewById(R.id.active_ride_card);
        if (card != null) card.setVisibility(View.GONE);
    }

    // ========== Map ==========

    private void showMapWithRoute() {
        if (binding == null || activeRide == null || mapView == null) return;

        binding.mapSection.setVisibility(View.VISIBLE);

        // Place markers (pickup, stops, destination) — clear old ones first
        clearRouteOverlays();
        placeRideMarkers();

        // Mark yellow route for redraw on ride load
        yellowRouteNeedsRedraw = true;

        // Start vehicle location polling (which also draws routes)
        pollVehicleLocation();
    }

    private void hideMap() {
        if (binding == null) return;
        binding.mapSection.setVisibility(View.GONE);
        mapCameraInitialized = false;
        yellowRouteNeedsRedraw = true;
    }

    /**
     * Removes old route overlays and markers (but NOT the vehicle marker) so
     * they can be redrawn without resetting camera position.
     */
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
     * Places markers on the map. Rules:
     * - IN_PROGRESS: hide start dot (already departed), hide completed stop dots
     * - Other statuses: show all dots
     */
    private void placeRideMarkers() {
        if (activeRide == null || mapView == null) return;
        boolean isInProgress = "IN_PROGRESS".equals(activeRide.getStatus());
        Set<Integer> completed = activeRide.getCompletedStopIndexes();

        // Pickup marker — hide for IN_PROGRESS
        if (!isInProgress && activeRide.getEffectiveStartLocation() != null) {
            GeoPoint pickup = new GeoPoint(
                    activeRide.getEffectiveStartLocation().getLatitude(),
                    activeRide.getEffectiveStartLocation().getLongitude());
            addMapMarker(pickup, "Pickup", R.drawable.ic_dot_green);
        }

        // Stop markers — hide completed ones
        if (activeRide.getStops() != null) {
            for (int i = 0; i < activeRide.getStops().size(); i++) {
                if (completed.contains(i)) continue; // skip completed stops
                com.example.mobile.models.LocationDto stop = activeRide.getStops().get(i);
                GeoPoint stopPoint = new GeoPoint(stop.getLatitude(), stop.getLongitude());
                String label = stop.getAddress() != null ? stop.getAddress() : "Stop " + (i + 1);
                addMapMarker(stopPoint, label, R.drawable.ic_dot_gray);
            }
        }

        // Destination marker — always show
        if (activeRide.getEffectiveEndLocation() != null) {
            GeoPoint dest = new GeoPoint(
                    activeRide.getEffectiveEndLocation().getLatitude(),
                    activeRide.getEffectiveEndLocation().getLongitude());
            addMapMarker(dest, "Destination", R.drawable.ic_dot_red);
        }

        mapView.invalidate();
    }

    private void pollVehicleLocation() {
        if (activeRide == null || mapView == null) return;

        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        ClientUtils.vehicleService.getActiveVehicles()
                .enqueue(new Callback<List<VehicleLocationResponse>>() {
                    @Override
                    public void onResponse(Call<List<VehicleLocationResponse>> call,
                                           Response<List<VehicleLocationResponse>> response) {
                        if (!isAdded() || binding == null || mapView == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            for (VehicleLocationResponse v : response.body()) {
                                if (userId.equals(v.getDriverId())) {
                                    updateVehicleOnMap(v);
                                    drawRoutes(v);
                                    checkStopCompletion(v);
                                    break;
                                }
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
            vehicleMarker.setTitle("Your Vehicle");
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
     * Draws two route segments from the vehicle position.
     * Blue route updates every poll. Yellow route only when yellowRouteNeedsRedraw is set.
     *
     * For IN_PROGRESS rides:
     *   Blue  = vehicle → next uncompleted stop (or destination if all stops done)
     *   Yellow = next uncompleted stop → remaining stops → destination
     *
     * For other rides (ACCEPTED, PENDING, SCHEDULED):
     *   Blue  = vehicle → pickup
     *   Yellow = pickup → stops → destination
     */
    private void drawRoutes(VehicleLocationResponse vehicle) {
        if (activeRide == null || mapView == null) return;

        GeoPoint vehiclePoint = new GeoPoint(vehicle.getLatitude(), vehicle.getLongitude());
        String rideStatus = activeRide.getStatus();
        boolean isInProgress = "IN_PROGRESS".equals(rideStatus);
        boolean redrawYellow = yellowRouteNeedsRedraw;
        if (redrawYellow) yellowRouteNeedsRedraw = false;

        // Build the full ordered list of ride waypoints: pickup → stops → destination
        List<GeoPoint> allWaypoints = new ArrayList<>();

        if (activeRide.getEffectiveStartLocation() != null) {
            allWaypoints.add(new GeoPoint(
                    activeRide.getEffectiveStartLocation().getLatitude(),
                    activeRide.getEffectiveStartLocation().getLongitude()));
        }
        if (activeRide.getStops() != null) {
            for (com.example.mobile.models.LocationDto stop : activeRide.getStops()) {
                allWaypoints.add(new GeoPoint(stop.getLatitude(), stop.getLongitude()));
            }
        }
        if (activeRide.getEffectiveEndLocation() != null) {
            allWaypoints.add(new GeoPoint(
                    activeRide.getEffectiveEndLocation().getLatitude(),
                    activeRide.getEffectiveEndLocation().getLongitude()));
        }

        if (allWaypoints.size() < 2) return;

        // Determine blue target and yellow waypoints
        ArrayList<GeoPoint> blueWaypoints = new ArrayList<>();
        ArrayList<GeoPoint> yellowWaypoints = new ArrayList<>();

        if (isInProgress) {
            Set<Integer> completed = activeRide.getCompletedStopIndexes();
            int stopsCount = activeRide.getStops() != null ? activeRide.getStops().size() : 0;

            int nextTargetIdx = allWaypoints.size() - 1; // default to destination
            for (int i = 0; i < stopsCount; i++) {
                if (!completed.contains(i)) {
                    nextTargetIdx = i + 1; // +1 because allWaypoints[0] = pickup
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
            blueWaypoints.add(allWaypoints.get(0)); // pickup

            yellowWaypoints.addAll(allWaypoints);
        }

        new Thread(() -> {
            try {
                RoadManager roadManager = new OSRMRoadManager(
                        requireContext().getApplicationContext(), "Lucky3-mobile");

                // Always rebuild blue route
                Polyline blueOverlay = null;
                if (blueWaypoints.size() >= 2) {
                    Road blueRoad = roadManager.getRoad(blueWaypoints);
                    if (blueRoad.mStatus == Road.STATUS_OK) {
                        blueOverlay = RoadManager.buildRoadOverlay(blueRoad);
                        blueOverlay.setColor(Color.parseColor("#3b82f6"));
                        blueOverlay.setWidth(10f);
                    }
                }

                // Only rebuild yellow route when flagged
                Polyline yellowOverlay = null;
                if (redrawYellow && yellowWaypoints.size() >= 2) {
                    Road yellowRoad = roadManager.getRoad(yellowWaypoints);
                    if (yellowRoad.mStatus == Road.STATUS_OK) {
                        yellowOverlay = RoadManager.buildRoadOverlay(yellowRoad);
                        yellowOverlay.setColor(Color.parseColor("#eab308"));
                        yellowOverlay.setWidth(12f);
                        Paint paint = yellowOverlay.getOutlinePaint();
                        paint.setPathEffect(new DashPathEffect(new float[]{30, 20}, 0));
                    }
                }

                final Polyline finalBlue = blueOverlay;
                final Polyline finalYellow = yellowOverlay;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mapView == null || !isAdded()) return;

                    // Remove old blue route
                    if (approachRoute != null) {
                        mapView.getOverlays().remove(approachRoute);
                    }

                    // Remove + replace yellow only when redrawn
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
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to draw routes", e);
            }
        }).start();
    }

    // ========== Stop Auto-Completion ==========

    /**
     * Checks proximity of vehicle to each uncompleted stop.
     * If within 50m, calls the backend to complete the stop.
     */
    private void checkStopCompletion(VehicleLocationResponse vehicle) {
        if (activeRide == null || !"IN_PROGRESS".equals(activeRide.getStatus())) return;
        if (activeRide.getStops() == null || activeRide.getStops().isEmpty()) return;

        Set<Integer> completed = activeRide.getCompletedStopIndexes();

        for (int i = 0; i < activeRide.getStops().size(); i++) {
            if (completed.contains(i) || pendingStopCompletions.contains(i)) continue;

            com.example.mobile.models.LocationDto stop = activeRide.getStops().get(i);
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
        if (activeRide == null) return;
        String token = "Bearer " + preferencesManager.getToken();

        ClientUtils.rideService.completeStop(activeRide.getId(), stopIndex, token)
                .enqueue(new Callback<RideResponse>() {
                    @Override
                    public void onResponse(Call<RideResponse> call,
                                           Response<RideResponse> response) {
                        pendingStopCompletions.remove(stopIndex);
                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            activeRide = response.body();
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

    /**
     * Called after a stop is successfully completed.
     * Refreshes markers and flags yellow route for redraw.
     */
    private void onStopCompleted(int stopIndex) {
        if (!isAdded() || binding == null || mapView == null) return;

        String stopName = "Stop " + (stopIndex + 1);
        if (activeRide.getStops() != null && stopIndex < activeRide.getStops().size()) {
            String addr = activeRide.getStops().get(stopIndex).getAddress();
            if (addr != null && !addr.isEmpty()) stopName = addr;
        }
        Toast.makeText(requireContext(), "✓ Completed: " + stopName, Toast.LENGTH_SHORT).show();

        // Refresh markers (removes completed stop dot)
        clearRouteOverlays();
        placeRideMarkers();

        // Flag yellow route for redraw on next poll
        yellowRouteNeedsRedraw = true;
    }

    /**
     * Haversine formula — returns distance in meters between two lat/lng points.
     */
    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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

    // ========== Scheduled Rides ==========

    private void loadScheduledRides() {
        scheduledPage = 0;
        scheduledRides.clear();
        allScheduledLoaded = false;
        fetchScheduledRidesPage(0);
    }

    private void loadMoreScheduledRides() {
        if (allScheduledLoaded) return;
        fetchScheduledRidesPage(scheduledPage);
    }

    private void fetchScheduledRidesPage(int page) {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getActiveRides(
                userId, null, "SCHEDULED", page, SCHEDULED_PAGE_SIZE, token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call,
                                   Response<PageResponse<RideResponse>> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<RideResponse> pageResponse = response.body();
                    List<RideResponse> content = pageResponse.getContent();
                    totalScheduledCount = (int) pageResponse.getTotalElements();

                    if (content != null && !content.isEmpty()) {
                        scheduledRides.addAll(content);
                        scheduledPage = page + 1;

                        if (scheduledRides.size() >= totalScheduledCount) {
                            allScheduledLoaded = true;
                        }
                    } else {
                        allScheduledLoaded = true;
                    }

                    updateScheduledUI();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to load scheduled rides", t);
            }
        });
    }

    private void updateScheduledUI() {
        if (binding == null) return;

        // Count badge
        binding.scheduledCount.setText(String.valueOf(totalScheduledCount));

        if (scheduledRides.isEmpty()) {
            binding.noScheduledRides.setVisibility(View.VISIBLE);
            binding.scheduledRidesList.setVisibility(View.GONE);
            binding.btnShowAllScheduled.setVisibility(View.GONE);
        } else {
            binding.noScheduledRides.setVisibility(View.GONE);
            binding.scheduledRidesList.setVisibility(View.VISIBLE);
            scheduledAdapter.notifyDataSetChanged();
            ListViewHelper.setListViewHeightBasedOnChildren(binding.scheduledRidesList);

            // Show "Show all" button if there are more
            if (!allScheduledLoaded && scheduledRides.size() < totalScheduledCount) {
                binding.btnShowAllScheduled.setVisibility(View.VISIBLE);
                binding.btnShowAllScheduled.setText(
                        "Show all future rides (" + totalScheduledCount + ")");
            } else {
                binding.btnShowAllScheduled.setVisibility(View.GONE);
            }
        }
    }

    // ========== Helpers ==========

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}

