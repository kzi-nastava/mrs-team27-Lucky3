package com.example.mobile.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.RoutePointResponse;
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.example.mobile.utils.StompClient;
import com.example.mobile.utils.WebSocketManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Admin Active Ride Detail Fragment.
 * Shows live ride detail with map tracking the vehicle in real-time via WebSocket.
 * Includes driver info (with rating + online hours), passengers, route, costs, panic status.
 */
public class AdminActiveRideDetailFragment extends Fragment {

    private static final String TAG = "AdminActiveRideDetail";

    private long rideId = -1;
    private RideResponse ride;

    // Map
    private MapView mapView;
    private RideMapRenderer mapRenderer;
    private Marker vehicleMarker;
    private Polyline approachRoute;
    private Polyline rideRouteOverlay;

    // WebSocket
    private String vehicleSubId;
    private String rideSubId;

    // Polling fallback
    private Handler pollHandler;
    private Runnable vehiclePollRunnable;
    private Runnable ridePollRunnable;
    private static final long VEHICLE_POLL_INTERVAL = 10_000;
    private static final long RIDE_POLL_INTERVAL = 15_000;
    private boolean wsConnected = false;

    // Refresh ride data periodically (in case WS misses updates)
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 30_000;

    private SharedPreferencesManager preferencesManager;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    // View references
    private View root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_admin_active_ride_detail, container, false);
        preferencesManager = new SharedPreferencesManager(requireContext());

        // Get rideId from arguments
        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId", -1);
        }

        // Init map
        mapView = root.findViewById(R.id.map_view);
        if (mapView != null) {
            mapRenderer = new RideMapRenderer(requireContext(), mapView);
            mapRenderer.initMap();
        }

        // Back button
        root.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        if (rideId > 0) {
            showLoading(true);
            loadRide();
        } else {
            Toast.makeText(getContext(), "Invalid ride ID", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // ===== DATA LOADING =====

    private void loadRide() {
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse> call,
                                   @NonNull Response<RideResponse> response) {
                if (!isAdded() || root == null) return;
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ride = response.body();
                    updateUI();
                    connectWebSocket();
                    startPeriodicRefresh();
                } else {
                    Toast.makeText(getContext(), "Failed to load ride", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshRide() {
        if (!isAdded() || rideId <= 0) return;
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse> call,
                                   @NonNull Response<RideResponse> response) {
                if (!isAdded() || root == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    ride = response.body();
                    updateUI();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse> call, @NonNull Throwable t) {
                // silently ignore
            }
        });
    }

    private void loadDriverStats(long driverId) {
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.driverService.getStats(driverId, token).enqueue(new Callback<DriverStatsResponse>() {
            @Override
            public void onResponse(@NonNull Call<DriverStatsResponse> call,
                                   @NonNull Response<DriverStatsResponse> response) {
                if (!isAdded() || root == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    updateDriverStats(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<DriverStatsResponse> call, @NonNull Throwable t) {
                // silently ignore
            }
        });
    }

    // ===== UI UPDATE =====

    private void updateUI() {
        if (root == null || ride == null) return;

        updateStatusBadge();
        updateRideId();
        updatePanicInfo();
        updateRideInfo();
        updateRoute();
        updateDriverInfo();
        updatePassengers();
        updateMap();
    }

    private void updateStatusBadge() {
        TextView badge = root.findViewById(R.id.tv_status_badge);
        String status = ride.getStatus() != null ? ride.getStatus() : "";
        badge.setText(ride.getDisplayStatus().toUpperCase());

        int bgRes;
        int textColor;
        switch (status) {
            case "IN_PROGRESS":
                bgRes = R.drawable.bg_badge_green;
                textColor = R.color.green_500;
                break;
            case "ACCEPTED":
                bgRes = R.drawable.bg_badge_blue;
                textColor = R.color.blue_500;
                break;
            case "PENDING":
                bgRes = R.drawable.bg_badge_active;
                textColor = R.color.yellow_500;
                break;
            case "SCHEDULED":
                bgRes = R.drawable.bg_badge_gray;
                textColor = R.color.gray_400;
                break;
            case "PANIC":
                bgRes = R.drawable.bg_badge_panic;
                textColor = R.color.red_500;
                break;
            default:
                bgRes = R.drawable.bg_badge_active;
                textColor = R.color.yellow_500;
                break;
        }
        badge.setBackgroundResource(bgRes);
        badge.setTextColor(ContextCompat.getColor(requireContext(), textColor));
    }

    private void updateRideId() {
        TextView tvRideId = root.findViewById(R.id.tv_ride_id);
        tvRideId.setText("Ride #" + (ride.getId() != null ? ride.getId() : "?"));
    }

    private void updatePanicInfo() {
        TextView panicBanner = root.findViewById(R.id.tv_panic_banner);
        LinearLayout panicCard = root.findViewById(R.id.panic_card);

        if (Boolean.TRUE.equals(ride.getPanicPressed())) {
            panicBanner.setVisibility(View.VISIBLE);
            panicCard.setVisibility(View.VISIBLE);

            TextView panicReason = root.findViewById(R.id.tv_panic_reason);
            String reason = ride.getPanicReason();
            if (reason != null && !reason.isEmpty()) {
                panicReason.setText("Reason: " + reason);
                panicReason.setVisibility(View.VISIBLE);
            } else {
                panicReason.setText("No reason provided");
            }
        } else {
            panicBanner.setVisibility(View.GONE);
            panicCard.setVisibility(View.GONE);
        }
    }

    private void updateRideInfo() {
        // Current cost
        TextView tvCurrentCost = root.findViewById(R.id.tv_current_cost);
        double currentCost = ride.getEffectiveCost();
        tvCurrentCost.setText(currentCost > 0
                ? String.format(Locale.US, "%.0f RSD", currentCost) : "\u2014");

        // Estimated cost
        TextView tvEstCost = root.findViewById(R.id.tv_est_cost);
        Double estCost = ride.getEstimatedCost();
        tvEstCost.setText(estCost != null && estCost > 0
                ? String.format(Locale.US, "%.0f RSD", estCost) : "\u2014");

        // Est time left
        TextView tvEstTime = root.findViewById(R.id.tv_est_time);
        String status = ride.getStatus() != null ? ride.getStatus() : "";
        if ("IN_PROGRESS".equals(status)) {
            int minLeft = computeEstimatedTimeLeft();
            tvEstTime.setText(minLeft > 0 ? "~" + minLeft + " min" : "\u2014");
        } else {
            Integer estMin = ride.getEstimatedTimeInMinutes();
            tvEstTime.setText(estMin != null ? estMin + " min" : "\u2014");
        }

        // Distance
        TextView tvDistance = root.findViewById(R.id.tv_distance);
        double dist = ride.getEffectiveDistance();
        tvDistance.setText(dist > 0 ? String.format(Locale.US, "%.1f km", dist) : "\u2014");

        // Vehicle type
        TextView tvVehicleType = root.findViewById(R.id.tv_vehicle_type);
        String vt = ride.getVehicleType();
        if (vt != null) {
            tvVehicleType.setText(vt.substring(0, 1).toUpperCase() + vt.substring(1).toLowerCase());
        } else {
            tvVehicleType.setText("\u2014");
        }
    }

    private void updateRoute() {
        LocationDto dep = ride.getEffectiveStartLocation();
        LocationDto dest = ride.getEffectiveEndLocation();

        TextView tvDep = root.findViewById(R.id.tv_departure);
        tvDep.setText(dep != null && dep.getAddress() != null ? dep.getAddress() : "\u2014");

        TextView tvDepTime = root.findViewById(R.id.tv_departure_time);
        if (ride.getStartTime() != null) {
            Date startDate = parseDate(ride.getStartTime());
            tvDepTime.setText(startDate != null ? "Started at " + timeFormat.format(startDate) : "");
        } else {
            tvDepTime.setText("");
        }

        TextView tvDest = root.findViewById(R.id.tv_destination);
        tvDest.setText(dest != null && dest.getAddress() != null ? dest.getAddress() : "\u2014");

        // Populate stops if any
        LinearLayout stopsContainer = root.findViewById(R.id.stops_container);
        stopsContainer.removeAllViews();
        List<LocationDto> stops = ride.getStops();
        if (stops != null && !stops.isEmpty()) {
            for (int i = 0; i < stops.size(); i++) {
                LocationDto stop = stops.get(i);
                TextView tvStop = new TextView(requireContext());
                tvStop.setText("\u25CF Stop " + (i + 1) + ": " +
                        (stop.getAddress() != null ? stop.getAddress() : "Unknown"));
                tvStop.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
                tvStop.setTextSize(12);
                tvStop.setPadding(0, 4, 0, 4);
                stopsContainer.addView(tvStop);
            }
        }
    }

    private void updateDriverInfo() {
        RideResponse.DriverInfo driver = ride.getDriver();
        LinearLayout driverCard = root.findViewById(R.id.driver_card);

        if (driver == null) {
            driverCard.setVisibility(View.GONE);
            return;
        }
        driverCard.setVisibility(View.VISIBLE);

        // Name
        TextView tvName = root.findViewById(R.id.tv_driver_name);
        String name = (driver.getName() != null ? driver.getName() : "") + " " +
                (driver.getSurname() != null ? driver.getSurname() : "");
        tvName.setText(name.trim().isEmpty() ? "Unknown Driver" : name.trim());

        // Email
        TextView tvEmail = root.findViewById(R.id.tv_driver_email);
        tvEmail.setText(driver.getEmail() != null ? driver.getEmail() : "");

        // Profile picture
        ImageView ivPhoto = root.findViewById(R.id.iv_driver_photo);
        loadProfileImage(driver.getProfilePicture(), ivPhoto);

        // Vehicle info
        RideResponse.VehicleInfo vehicle = driver.getVehicle();
        TextView tvModel = root.findViewById(R.id.tv_vehicle_model);
        TextView tvPlates = root.findViewById(R.id.tv_vehicle_plates);
        TextView tvIcon = root.findViewById(R.id.tv_vehicle_icon);
        TextView tvTypeBadge = root.findViewById(R.id.tv_vehicle_type_badge);

        if (vehicle != null) {
            tvModel.setText(vehicle.getModel() != null ? vehicle.getModel() : "\u2014");
            tvPlates.setText(vehicle.getLicensePlates() != null ? vehicle.getLicensePlates() : "\u2014");
            String type = vehicle.getVehicleType() != null ? vehicle.getVehicleType() : "";
            tvTypeBadge.setText(type);
            tvIcon.setText(getVehicleIcon(type));
        } else {
            tvModel.setText(ride.getModel() != null ? ride.getModel() : "\u2014");
            tvPlates.setText(ride.getLicensePlates() != null ? ride.getLicensePlates() : "\u2014");
            String type = ride.getVehicleType() != null ? ride.getVehicleType() : "";
            tvTypeBadge.setText(type);
            tvIcon.setText(getVehicleIcon(type));
        }

        // Load driver stats (rating, rides, online hours)
        if (driver.getId() != null) {
            loadDriverStats(driver.getId());
        }
    }

    private void updateDriverStats(DriverStatsResponse stats) {
        if (root == null) return;

        TextView tvRating = root.findViewById(R.id.tv_driver_rating);
        double avg = stats.getAverageRating();
        tvRating.setText(avg > 0
                ? String.format(Locale.US, "\u2B50 %.1f", avg)
                : "\u2B50 \u2014");

        TextView tvRides = root.findViewById(R.id.tv_driver_rides);
        tvRides.setText("\uD83D\uDE97 " + stats.getCompletedRides() + " rides");

        TextView tvOnline = root.findViewById(R.id.tv_driver_online);
        String onlineHours = stats.getOnlineHoursToday();
        tvOnline.setText("\uD83D\uDD50 " + (onlineHours != null ? onlineHours : "\u2014"));
    }

    private void updatePassengers() {
        LinearLayout container = root.findViewById(R.id.passengers_container);
        container.removeAllViews();

        List<RideResponse.PassengerInfo> passengers = ride.getPassengers();
        TextView header = root.findViewById(R.id.tv_passengers_header);
        int count = passengers != null ? passengers.size() : 0;
        header.setText("Passengers (" + count + ")");

        if (passengers == null || passengers.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("No passengers");
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
            tv.setTextSize(12);
            container.addView(tv);
            return;
        }

        for (int i = 0; i < passengers.size(); i++) {
            RideResponse.PassengerInfo p = passengers.get(i);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, i < passengers.size() - 1 ? 12 : 0);

            TextView nameText = new TextView(requireContext());
            nameText.setText("\uD83D\uDC64 " + p.getFullName());
            nameText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            nameText.setTextSize(14);
            row.addView(nameText);

            if (p.getEmail() != null) {
                TextView emailText = new TextView(requireContext());
                emailText.setText("   " + p.getEmail());
                emailText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
                emailText.setTextSize(12);
                row.addView(emailText);
            }

            container.addView(row);

            if (i < passengers.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_800));
                container.addView(divider);
            }
        }
    }

    // ===== MAP =====

    private void updateMap() {
        if (mapRenderer == null || ride == null) return;

        try {
            // Draw route
            List<RoutePointResponse> routePoints = ride.getRoutePoints();
            if (routePoints != null && !routePoints.isEmpty()) {
                mapRenderer.showRoute(routePoints);
            } else {
                // Fallback: build waypoints
                LocationDto start = ride.getEffectiveStartLocation();
                LocationDto end = ride.getEffectiveEndLocation();
                if (start != null && end != null) {
                    List<RoutePointResponse> waypoints = new ArrayList<>();
                    waypoints.add(makeRoutePoint(start, 0));
                    List<LocationDto> stops = ride.getStops();
                    if (stops != null) {
                        for (int i = 0; i < stops.size(); i++) {
                            waypoints.add(makeRoutePoint(stops.get(i), i + 1));
                        }
                    }
                    waypoints.add(makeRoutePoint(end, waypoints.size()));
                    mapRenderer.showRoute(waypoints);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating map", e);
        }
    }

    private void updateVehicleOnMap(double lat, double lng, boolean panic) {
        if (mapView == null || !isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            try {
                GeoPoint pos = new GeoPoint(lat, lng);

                if (vehicleMarker == null) {
                    vehicleMarker = new Marker(mapView);
                    vehicleMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    vehicleMarker.setInfoWindow(null);
                    mapView.getOverlays().add(vehicleMarker);
                }

                // Use blue dot normally, red if panic
                int drawableRes = panic ? R.drawable.bg_dot_red : R.drawable.bg_dot_blue;
                Drawable d = ContextCompat.getDrawable(requireContext(), drawableRes);
                vehicleMarker.setIcon(d);
                vehicleMarker.setPosition(pos);
                vehicleMarker.setTitle("Vehicle");

                mapView.getController().animateTo(pos);
                mapView.invalidate();
            } catch (Exception e) {
                Log.e(TAG, "Error updating vehicle marker", e);
            }
        });
    }

    private RoutePointResponse makeRoutePoint(LocationDto loc, int order) {
        RoutePointResponse rp = new RoutePointResponse();
        rp.setLocation(loc);
        rp.setOrder(order);
        return rp;
    }

    // ===== WEBSOCKET =====

    private void connectWebSocket() {
        WebSocketManager ws = WebSocketManager.getInstance();
        ws.connect(requireContext(), new StompClient.ConnectionCallback() {
            @Override
            public void onConnected() {
                if (!isAdded()) return;
                wsConnected = true;
                subscribeToVehicles();
                subscribeToRide();
            }

            @Override
            public void onDisconnected(String reason) {
                wsConnected = false;
                startPollingFallback();
            }

            @Override
            public void onError(String error) {
                wsConnected = false;
                startPollingFallback();
            }
        });
    }

    private void subscribeToVehicles() {
        if (!isAdded()) return;
        vehicleSubId = WebSocketManager.getInstance().subscribe(
                "/topic/vehicles",
                new com.google.gson.reflect.TypeToken<List<VehicleLocationResponse>>() {}.getType(),
                (StompClient.MessageCallback<List<VehicleLocationResponse>>) vehicles -> {
                    if (!isAdded() || ride == null || ride.getDriverId() == null) return;
                    for (VehicleLocationResponse v : vehicles) {
                        if (ride.getDriverId().equals(v.getDriverId())) {
                            boolean panic = v.isCurrentPanic() ||
                                    Boolean.TRUE.equals(ride.getPanicPressed());
                            updateVehicleOnMap(v.getLatitude(), v.getLongitude(), panic);
                            break;
                        }
                    }
                });
    }

    private void subscribeToRide() {
        if (!isAdded()) return;
        rideSubId = WebSocketManager.getInstance().subscribe(
                "/topic/ride/" + rideId,
                RideResponse.class,
                (StompClient.MessageCallback<RideResponse>) updatedRide -> {
                    if (!isAdded() || root == null) return;
                    ride = updatedRide;
                    requireActivity().runOnUiThread(this::updateUI);
                });
    }

    // ===== POLLING FALLBACK =====

    private void startPollingFallback() {
        if (wsConnected || !isAdded()) return;

        pollHandler = new Handler(Looper.getMainLooper());

        // Poll vehicle location
        vehiclePollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || wsConnected) return;
                pollVehicleLocation();
                pollHandler.postDelayed(this, VEHICLE_POLL_INTERVAL);
            }
        };
        pollHandler.postDelayed(vehiclePollRunnable, VEHICLE_POLL_INTERVAL);

        // Poll ride data
        ridePollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || wsConnected) return;
                refreshRide();
                pollHandler.postDelayed(this, RIDE_POLL_INTERVAL);
            }
        };
        pollHandler.postDelayed(ridePollRunnable, RIDE_POLL_INTERVAL);
    }

    private void pollVehicleLocation() {
        ClientUtils.vehicleService.getActiveVehicles().enqueue(new Callback<List<VehicleLocationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<VehicleLocationResponse>> call,
                                   @NonNull Response<List<VehicleLocationResponse>> response) {
                if (!isAdded() || ride == null || ride.getDriverId() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (VehicleLocationResponse v : response.body()) {
                        if (ride.getDriverId().equals(v.getDriverId())) {
                            boolean panic = v.isCurrentPanic() ||
                                    Boolean.TRUE.equals(ride.getPanicPressed());
                            updateVehicleOnMap(v.getLatitude(), v.getLongitude(), panic);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VehicleLocationResponse>> call, @NonNull Throwable t) {
                // silently ignore
            }
        });
    }

    // ===== PERIODIC REFRESH =====

    private void startPeriodicRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            if (isAdded()) {
                refreshRide();
                refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    // ===== HELPERS =====

    private int computeEstimatedTimeLeft() {
        if (ride == null) return 0;
        // Method 1: distance remaining at ~30 km/h
        Double distKm = ride.getDistanceKm();
        Double traveled = ride.getDistanceTraveled();
        if (distKm != null && traveled != null && distKm > traveled) {
            double remaining = distKm - traveled;
            return (int) Math.ceil((remaining / 30.0) * 60);
        }
        // Method 2: estimatedTime minus elapsed
        if (ride.getEstimatedTimeInMinutes() != null && ride.getStartTime() != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(ride.getStartTime(),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                long elapsed = ChronoUnit.MINUTES.between(start, LocalDateTime.now());
                int remaining = ride.getEstimatedTimeInMinutes() - (int) elapsed;
                return Math.max(remaining, 0);
            } catch (Exception e) { /* ignore */ }
        }
        // Method 3: raw estimated time
        if (ride.getEstimatedTimeInMinutes() != null) {
            return ride.getEstimatedTimeInMinutes();
        }
        return 0;
    }

    private String getVehicleIcon(String type) {
        if (type == null) return "\uD83D\uDE97";
        switch (type.toUpperCase()) {
            case "LUXURY": return "\uD83C\uDFC6";
            case "VAN": return "\uD83D\uDE90";
            default: return "\uD83D\uDE97";
        }
    }

    private void loadProfileImage(String profilePicture, ImageView imageView) {
        if (profilePicture == null || profilePicture.isEmpty()) return;
        if (profilePicture.startsWith("data:image") || !profilePicture.startsWith("http")) {
            try {
                String base64Data = profilePicture;
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                }
                byte[] decoded = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) imageView.setImageBitmap(bmp);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode profile image", e);
            }
        } else {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(profilePicture);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
                    if (bmp != null && isAdded()) {
                        requireActivity().runOnUiThread(() -> imageView.setImageBitmap(bmp));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load profile image from URL", e);
                }
            }).start();
        }
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                return sdf2.parse(dateStr);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private void showLoading(boolean show) {
        if (root == null) return;
        View pb = root.findViewById(R.id.progress_bar);
        if (pb != null) pb.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ===== LIFECYCLE =====

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

        // Unsubscribe WebSocket
        WebSocketManager ws = WebSocketManager.getInstance();
        if (vehicleSubId != null) ws.unsubscribe(vehicleSubId);
        if (rideSubId != null) ws.unsubscribe(rideSubId);

        // Stop polling
        if (pollHandler != null) {
            if (vehiclePollRunnable != null) pollHandler.removeCallbacks(vehiclePollRunnable);
            if (ridePollRunnable != null) pollHandler.removeCallbacks(ridePollRunnable);
        }

        // Stop periodic refresh
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }

        if (mapView != null) mapView.onDetach();
        root = null;
    }
}
