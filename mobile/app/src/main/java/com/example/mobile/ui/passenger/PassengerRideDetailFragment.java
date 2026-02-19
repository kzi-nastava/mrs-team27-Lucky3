package com.example.mobile.ui.passenger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.FavoriteRouteRequest;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.RoutePointResponse;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import org.osmdroid.views.MapView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Passenger Ride Detail Fragment.
 * Shows full ride details including map with route, driver info,
 * inconsistency reports, reviews, price breakdown, and reorder options.
 */
public class PassengerRideDetailFragment extends Fragment {

    private static final String TAG = "PassengerRideDetail";
    private SharedPreferencesManager preferencesManager;

    private RideResponse ride;
    private long rideId = -1;

    // Map
    private MapView mapView;
    private RideMapRenderer mapRenderer;

    // Date formatters
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_passenger_ride_detail, container, false);

        preferencesManager = new SharedPreferencesManager(requireContext());

        // Initialize map
        mapView = root.findViewById(R.id.map_view);
        if (mapView != null) {
            mapRenderer = new RideMapRenderer(requireContext(), mapView);
            mapRenderer.initMap();
        }

        // Back button
        root.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Get ride ID from arguments
        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId", -1);
        }

        if (rideId > 0) {
            loadRideDetails(root);
        } else {
            Toast.makeText(getContext(), "Invalid ride ID", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    private void loadRideDetails(View root) {
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ride = response.body();
                    updateUI(root);
                } else {
                    Log.e(TAG, "Failed to load ride: " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load ride details", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<RideResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load ride", t);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(View root) {
        if (root == null || ride == null) return;

        updateMap();
        updateRideSummary(root);
        updateCancellationInfo(root);
        updatePanicInfo(root);
        updateDriverInfo(root);
        updateInconsistencyReports(root);
        updateReviews(root);
        updatePriceBreakdown(root);
        setupActionButtons(root);
    }

    // ==================== MAP ====================

    private void updateMap() {
        if (mapRenderer == null || ride == null) return;

        try {
            List<RoutePointResponse> routePoints = ride.getRoutePoints();
            if (routePoints != null && !routePoints.isEmpty()) {
                mapRenderer.showRoute(routePoints);
                return;
            }

            // Fallback: build waypoints from departure/stops/destination
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
        } catch (Exception e) {
            Log.e(TAG, "Error updating map", e);
        }
    }

    private RoutePointResponse makeRoutePoint(LocationDto loc, int order) {
        RoutePointResponse rp = new RoutePointResponse();
        rp.setLocation(loc);
        rp.setOrder(order);
        return rp;
    }

    // ==================== RIDE SUMMARY ====================

    private void updateRideSummary(View root) {
        try {
            // Ride number
            TextView rideNumber = root.findViewById(R.id.ride_number);
            rideNumber.setText("Ride #" + ride.getId());

            // Status badge
            TextView rideStatus = root.findViewById(R.id.ride_status);
            rideStatus.setText(ride.getDisplayStatus().toUpperCase());
            if (ride.isFinished()) {
                rideStatus.setBackgroundResource(R.drawable.bg_badge_green);
                rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
            } else if (ride.isCancelled()) {
                rideStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
                rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
            } else if (ride.isInProgress()) {
                rideStatus.setBackgroundResource(R.drawable.bg_badge_blue);
                rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500));
            } else if (ride.isScheduled()) {
                rideStatus.setBackgroundResource(R.drawable.bg_badge_gray);
                rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
            } else if ("PANIC".equals(ride.getStatus())) {
                rideStatus.setBackgroundResource(R.drawable.bg_badge_panic);
                rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
            } else {
                rideStatus.setBackgroundResource(R.drawable.bg_badge_active);
                rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            }

            // Price
            double totalCost = ride.getEffectiveCost();
            TextView ridePrice = root.findViewById(R.id.ride_price);
            ridePrice.setText(String.format(Locale.US, "%.0f RSD", totalCost));

            // Date range
            TextView rideDateRange = root.findViewById(R.id.ride_date_range);
            Date startDate = parseDate(ride.getStartTime());
            if (startDate == null) startDate = parseDate(ride.getScheduledTime());
            Date endDate = parseDate(ride.getEndTime());
            if (startDate != null && endDate != null) {
                rideDateRange.setText(dateFormat.format(startDate) + ", " +
                        timeFormat.format(startDate) + " — " + timeFormat.format(endDate));
            } else if (startDate != null) {
                rideDateRange.setText(dateFormat.format(startDate) + ", " + timeFormat.format(startDate));
            } else {
                rideDateRange.setText("—");
            }

            // Pickup/Destination
            LocationDto pickupLoc = ride.getEffectiveStartLocation();
            LocationDto destLoc = ride.getEffectiveEndLocation();

            TextView tvPickup = root.findViewById(R.id.tv_pickup_address);
            tvPickup.setText(pickupLoc != null && pickupLoc.getAddress() != null
                    ? pickupLoc.getAddress() : "—");

            TextView tvDest = root.findViewById(R.id.tv_dest_address);
            tvDest.setText(destLoc != null && destLoc.getAddress() != null
                    ? destLoc.getAddress() : "—");

            // Pickup/Arrival times
            TextView tvPickupTime = root.findViewById(R.id.tv_pickup_time);
            tvPickupTime.setText(startDate != null
                    ? "Picked up at " + timeFormat.format(startDate) : "");

            TextView tvDestTime = root.findViewById(R.id.tv_dest_time);
            if (endDate != null) {
                tvDestTime.setText("Arrived at " + timeFormat.format(endDate));
            } else if (ride.isCancelled()) {
                tvDestTime.setText("Cancelled");
                tvDestTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
            } else {
                tvDestTime.setText("");
            }

            // Stats: Distance, Duration, Vehicle Type
            double distance = ride.getEffectiveDistance();
            TextView tvDistance = root.findViewById(R.id.tv_distance);
            tvDistance.setText(String.format(Locale.US, "%.1f km", distance));

            TextView tvDuration = root.findViewById(R.id.tv_duration);
            Integer estimatedMin = ride.getEstimatedTimeInMinutes();
            if (startDate != null && endDate != null) {
                long mins = (endDate.getTime() - startDate.getTime()) / 60000;
                tvDuration.setText(mins + " min");
            } else if (estimatedMin != null) {
                tvDuration.setText(estimatedMin + " min");
            } else {
                tvDuration.setText("—");
            }

            TextView tvVehicleType = root.findViewById(R.id.tv_vehicle_type);
            String vt = ride.getVehicleType();
            if (vt != null) {
                tvVehicleType.setText(vt.substring(0, 1).toUpperCase() + vt.substring(1).toLowerCase());
            } else {
                tvVehicleType.setText("—");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating ride summary", e);
        }
    }

    // ==================== CANCELLATION ====================

    private void updateCancellationInfo(View root) {
        LinearLayout cancellationCard = root.findViewById(R.id.cancellation_card);
        if (ride.isCancelled()) {
            cancellationCard.setVisibility(View.VISIBLE);
            TextView label = root.findViewById(R.id.tv_cancelled_label);
            String cancelledBy = ride.getCancelledBy();
            label.setText("Cancelled by " + cancelledBy);

            TextView reason = root.findViewById(R.id.tv_cancelled_reason);
            String rejectionReason = ride.getRejectionReason();
            if (rejectionReason != null && !rejectionReason.isEmpty()) {
                reason.setVisibility(View.VISIBLE);
                reason.setText("Reason: " + rejectionReason);
            } else {
                reason.setVisibility(View.GONE);
            }
        } else {
            cancellationCard.setVisibility(View.GONE);
        }
    }

    // ==================== PANIC ====================

    private void updatePanicInfo(View root) {
        LinearLayout panicCard = root.findViewById(R.id.panic_card);
        if (Boolean.TRUE.equals(ride.getPanicPressed())) {
            panicCard.setVisibility(View.VISIBLE);
            TextView panicReason = root.findViewById(R.id.tv_panic_reason);
            String reason = ride.getPanicReason();
            if (reason != null && !reason.isEmpty()) {
                panicReason.setVisibility(View.VISIBLE);
                panicReason.setText("Reason: " + reason);
            } else {
                panicReason.setVisibility(View.GONE);
            }
        } else {
            panicCard.setVisibility(View.GONE);
        }
    }

    // ==================== DRIVER INFO ====================

    private void updateDriverInfo(View root) {
        RideResponse.DriverInfo driver = ride.getDriver();
        LinearLayout driverCard = root.findViewById(R.id.driver_card);

        if (driver == null) {
            driverCard.setVisibility(View.GONE);
            return;
        }

        driverCard.setVisibility(View.VISIBLE);

        // Driver name
        TextView tvDriverName = root.findViewById(R.id.tv_driver_name);
        String name = (driver.getName() != null ? driver.getName() : "") + " " +
                (driver.getSurname() != null ? driver.getSurname() : "");
        tvDriverName.setText(name.trim().isEmpty() ? "Unknown" : name.trim());

        // Email
        TextView tvDriverEmail = root.findViewById(R.id.tv_driver_email);
        tvDriverEmail.setText(driver.getEmail() != null ? driver.getEmail() : "—");

        // Phone
        TextView tvDriverPhone = root.findViewById(R.id.tv_driver_phone);
        tvDriverPhone.setText(driver.getPhoneNumber() != null ? driver.getPhoneNumber() : "—");

        // Profile picture
        ImageView ivDriverPhoto = root.findViewById(R.id.iv_driver_photo);
        loadProfileImage(driver.getProfilePicture(), ivDriverPhoto);

        // Vehicle details
        RideResponse.VehicleInfo vehicle = driver.getVehicle();
        TextView tvModel = root.findViewById(R.id.tv_vehicle_model);
        TextView tvPlates = root.findViewById(R.id.tv_vehicle_plates);
        TextView tvFeatures = root.findViewById(R.id.tv_vehicle_features);

        if (vehicle != null) {
            tvModel.setText(vehicle.getModel() != null ? vehicle.getModel() : "—");
            tvPlates.setText(vehicle.getLicensePlates() != null ? vehicle.getLicensePlates() : "—");

            List<String> features = new ArrayList<>();
            if (Boolean.TRUE.equals(vehicle.getBabyTransport())) features.add("Baby");
            if (Boolean.TRUE.equals(vehicle.getPetTransport())) features.add("Pet");
            tvFeatures.setText(features.isEmpty() ? "None" : String.join(", ", features));
        } else {
            tvModel.setText(ride.getModel() != null ? ride.getModel() : "—");
            tvPlates.setText(ride.getLicensePlates() != null ? ride.getLicensePlates() : "—");

            List<String> features = new ArrayList<>();
            if (Boolean.TRUE.equals(ride.getBabyTransport())) features.add("Baby");
            if (Boolean.TRUE.equals(ride.getPetTransport())) features.add("Pet");
            tvFeatures.setText(features.isEmpty() ? "None" : String.join(", ", features));
        }
    }

    private void loadProfileImage(String profilePicture, ImageView imageView) {
        if (profilePicture == null || profilePicture.isEmpty()) return;

        // Check if it's base64 encoded
        if (profilePicture.startsWith("data:image") || !profilePicture.startsWith("http")) {
            try {
                String base64Data = profilePicture;
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                }
                byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode base64 image", e);
            }
        } else {
            // Load from URL on background thread
            new Thread(() -> {
                try {
                    URL url = new URL(profilePicture);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load profile image from URL", e);
                }
            }).start();
        }
    }

    // ==================== INCONSISTENCY REPORTS ====================

    private void updateInconsistencyReports(View root) {
        LinearLayout card = root.findViewById(R.id.inconsistency_card);
        LinearLayout container = root.findViewById(R.id.inconsistency_container);
        container.removeAllViews();

        List<RideResponse.InconsistencyInfo> reports = ride.getInconsistencyReports();
        if (reports == null || reports.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);

        for (int i = 0; i < reports.size(); i++) {
            RideResponse.InconsistencyInfo report = reports.get(i);

            LinearLayout reportRow = new LinearLayout(requireContext());
            reportRow.setOrientation(LinearLayout.VERTICAL);
            reportRow.setPadding(0, 0, 0, i < reports.size() - 1 ? 12 : 0);

            // Description
            TextView descText = createInfoText(report.getDescription() != null
                    ? report.getDescription() : "No description");
            descText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            reportRow.addView(descText);

            // Timestamp
            Date timestamp = parseDate(report.getTimestamp());
            if (timestamp != null) {
                TextView timeText = createInfoText(dateTimeFormat.format(timestamp));
                reportRow.addView(timeText);
            }

            container.addView(reportRow);

            // Divider
            if (i < reports.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_800));
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) divider.getLayoutParams();
                params.topMargin = 8;
                divider.setLayoutParams(params);
                container.addView(divider);
            }
        }
    }

    // ==================== REVIEWS ====================

    private void updateReviews(View root) {
        LinearLayout card = root.findViewById(R.id.reviews_card);
        LinearLayout container = root.findViewById(R.id.reviews_container);
        container.removeAllViews();

        List<RideResponse.ReviewInfo> reviews = ride.getReviews();
        if (reviews == null || reviews.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);

        for (int i = 0; i < reviews.size(); i++) {
            RideResponse.ReviewInfo review = reviews.get(i);

            LinearLayout reviewRow = new LinearLayout(requireContext());
            reviewRow.setOrientation(LinearLayout.VERTICAL);
            reviewRow.setPadding(0, 0, 0, i < reviews.size() - 1 ? 12 : 0);

            // Ratings row
            LinearLayout ratingsRow = new LinearLayout(requireContext());
            ratingsRow.setOrientation(LinearLayout.HORIZONTAL);

            // Driver rating
            Integer driverRating = review.getDriverRating();
            if (driverRating != null) {
                TextView driverRatingText = createLabelText("Driver: " + getStars(driverRating));
                driverRatingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
                driverRatingText.setTextSize(13);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                driverRatingText.setLayoutParams(p);
                ratingsRow.addView(driverRatingText);
            }

            // Vehicle rating
            Integer vehicleRating = review.getVehicleRating();
            if (vehicleRating != null) {
                TextView vehicleRatingText = createLabelText("Vehicle: " + getStars(vehicleRating));
                vehicleRatingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
                vehicleRatingText.setTextSize(13);
                ratingsRow.addView(vehicleRatingText);
            }

            reviewRow.addView(ratingsRow);

            // Comment
            String comment = review.getComment();
            if (comment != null && !comment.isEmpty()) {
                TextView commentText = createInfoText("\"" + comment + "\"");
                commentText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                p.topMargin = 4;
                commentText.setLayoutParams(p);
                reviewRow.addView(commentText);
            }

            // Date
            Date createdAt = parseDate(review.getCreatedAt());
            if (createdAt != null) {
                TextView dateText = createInfoText(dateTimeFormat.format(createdAt));
                reviewRow.addView(dateText);
            }

            container.addView(reviewRow);

            // Divider
            if (i < reviews.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_800));
                LinearLayout.LayoutParams dp = (LinearLayout.LayoutParams) divider.getLayoutParams();
                dp.topMargin = 8;
                divider.setLayoutParams(dp);
                container.addView(divider);
            }
        }
    }

    private String getStars(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < rating ? "★" : "☆");
        }
        return sb.toString();
    }

    // ==================== PRICE BREAKDOWN ====================

    private void updatePriceBreakdown(View root) {
        double distance = ride.getEffectiveDistance();
        Double baseFare = ride.getRateBaseFare();
        Double perKm = ride.getRatePricePerKm();
        double totalCost = ride.getEffectiveCost();

        TextView tvBaseFare = root.findViewById(R.id.tv_base_fare);
        tvBaseFare.setText(baseFare != null
                ? String.format(Locale.US, "%.0f RSD", baseFare) : "—");

        TextView tvDistLabel = root.findViewById(R.id.tv_distance_label);
        tvDistLabel.setText(String.format(Locale.US, "Distance (%.1f km)", distance));

        TextView tvDistCost = root.findViewById(R.id.tv_distance_cost);
        double distCost = (perKm != null ? perKm : 0) * distance;
        tvDistCost.setText(String.format(Locale.US, "%.0f RSD", distCost));

        TextView tvTotal = root.findViewById(R.id.tv_total_cost);
        tvTotal.setText(String.format(Locale.US, "%.0f RSD", totalCost));
    }

    // ==================== ACTION BUTTONS ====================

    private void setupActionButtons(View root) {
        TextView btnAddToFavorites = root.findViewById(R.id.btn_add_to_favorites);
        TextView btnOrderAgain = root.findViewById(R.id.btn_order_again);
        TextView btnScheduleLater = root.findViewById(R.id.btn_schedule_later);
        TextView btnLeaveReview = root.findViewById(R.id.btn_leave_review);

        // Show/hide review button based on ride status and existing reviews
        setupReviewButton(btnLeaveReview);

        // Add to Favorites button
        btnAddToFavorites.setOnClickListener(v -> {
            if (ride != null && rideId > 0) {
                addRideToFavorites();
            }
        });

        btnOrderAgain.setOnClickListener(v -> {
            if (ride != null) {
                // Navigate to passenger home with route info
                Bundle args = new Bundle();
                LocationDto start = ride.getEffectiveStartLocation();
                LocationDto end = ride.getEffectiveEndLocation();
                if (start != null) {
                    args.putString("startAddress", start.getAddress());
                    args.putDouble("startLat", start.getLatitude());
                    args.putDouble("startLng", start.getLongitude());
                }
                if (end != null) {
                    args.putString("endAddress", end.getAddress());
                    args.putDouble("endLat", end.getLatitude());
                    args.putDouble("endLng", end.getLongitude());
                }
                args.putBoolean("fromHistory", true);

                Toast.makeText(getContext(),
                        "Reordering: " + getRouteDescription(), Toast.LENGTH_SHORT).show();

                Navigation.findNavController(requireView())
                        .navigate(R.id.nav_passenger_home, args);
            }
        });

        btnScheduleLater.setOnClickListener(v -> {
            if (ride != null) {
                Bundle args = new Bundle();
                LocationDto start = ride.getEffectiveStartLocation();
                LocationDto end = ride.getEffectiveEndLocation();
                if (start != null) {
                    args.putString("startAddress", start.getAddress());
                    args.putDouble("startLat", start.getLatitude());
                    args.putDouble("startLng", start.getLongitude());
                }
                if (end != null) {
                    args.putString("endAddress", end.getAddress());
                    args.putDouble("endLat", end.getLatitude());
                    args.putDouble("endLng", end.getLongitude());
                }
                args.putBoolean("fromHistory", true);
                args.putBoolean("scheduleLater", true);

                Toast.makeText(getContext(),
                        "Schedule: " + getRouteDescription(), Toast.LENGTH_SHORT).show();

                Navigation.findNavController(requireView())
                        .navigate(R.id.nav_passenger_home, args);
            }
        });
    }

    private void setupReviewButton(TextView btnLeaveReview) {
        if (ride == null || !ride.isFinished()) {
            btnLeaveReview.setVisibility(View.GONE);
            return;
        }

        // Check if this passenger has already reviewed this ride
        Long userId = preferencesManager.getUserId();
        boolean hasReviewed = false;
        List<RideResponse.ReviewInfo> reviews = ride.getReviews();
        if (reviews != null && userId != null) {
            for (RideResponse.ReviewInfo review : reviews) {
                if (userId.equals(review.getPassengerId())) {
                    hasReviewed = true;
                    break;
                }
            }
        }

        btnLeaveReview.setVisibility(View.VISIBLE);
        if (hasReviewed) {
            btnLeaveReview.setEnabled(false);
            btnLeaveReview.setText("Already Reviewed");
            btnLeaveReview.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
            btnLeaveReview.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_800));
        } else {
            btnLeaveReview.setEnabled(true);
            btnLeaveReview.setText("Leave a Review");
            btnLeaveReview.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("rideId", rideId);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_passenger_ride_detail_to_review, args);
            });
        }
    }

    private void addRideToFavorites() {
        // Get location details from the ride
        LocationDto start = ride.getEffectiveStartLocation();
        LocationDto end = ride.getEffectiveEndLocation();

        if (start == null || end == null) {
            Toast.makeText(getContext(), "Invalid route information", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show dialog to name the route
        showNameRouteDialog(start, end);
    }

    private void showNameRouteDialog(LocationDto start, LocationDto end) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme);
        builder.setTitle("Name This Favorite Route");

        // Create input field with proper styling
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("e.g., Home to Work");
        input.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        input.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
        input.setBackgroundResource(R.drawable.bg_edit_text);

        // Add padding to the input
        int paddingDp = 16;
        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int) (paddingDp * density);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Set default name
        String defaultName;
        if (start.getAddress() != null && end.getAddress() != null) {
            String startShort = getShortAddress(start.getAddress());
            String endShort = getShortAddress(end.getAddress());
            defaultName = startShort + " → " + endShort;
        } else {
            defaultName = "Ride #" + ride.getId();
        }
        input.setText(defaultName);
        input.selectAll();

        // Add margin around the input
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(paddingPx, paddingPx / 2, paddingPx, 0);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Save", null); // Set null initially
        builder.setNegativeButton("Cancel", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Style the buttons after showing
        android.widget.Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
            positiveButton.setOnClickListener(v -> {
                String routeName = input.getText().toString().trim();
                if (routeName.isEmpty()) {
                    routeName = defaultName;
                }
                saveFavoriteRoute(start, end, routeName);
                dialog.dismiss();
            });
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
        }
    }


    private String getShortAddress(String fullAddress) {
        if (fullAddress == null) return "";
        // Take first part before comma or first 30 chars
        String[] parts = fullAddress.split(",");
        String short_addr = parts[0].trim();
        if (short_addr.length() > 30) {
            return short_addr.substring(0, 27) + "...";
        }
        return short_addr;
    }

    private void saveFavoriteRoute(LocationDto start, LocationDto end, String routeName) {
        String token = "Bearer " + preferencesManager.getToken();
        Long passengerId = preferencesManager.getUserId(); // Make sure you have this method

        // Create the request body
        FavoriteRouteRequest request = new FavoriteRouteRequest(
                start.getAddress(),
                end.getAddress(),
                routeName
        );

        ClientUtils.rideService.addFavouriteRoute(passengerId, request, token)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "\"" + routeName + "\" added to favorites!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Failed to add to favorites: " + response.code());
                            if (getContext() != null) {
                                String errorMsg = "Failed to add to favorites";
                                if (response.code() == 409) {
                                    errorMsg = "Route already in favorites";
                                }
                                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Failed to add to favorites", t);
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Network error",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private String getRouteDescription() {
        String from = "?";
        String to = "?";
        LocationDto start = ride.getEffectiveStartLocation();
        LocationDto end = ride.getEffectiveEndLocation();
        if (start != null && start.getAddress() != null) from = start.getAddress();
        if (end != null && end.getAddress() != null) to = end.getAddress();
        return from + " → " + to;
    }

    // ==================== HELPERS ====================

    private TextView createLabelText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tv.setTextSize(14);
        return tv;
    }

    private TextView createInfoText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
        tv.setTextSize(12);
        return tv;
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                return sdf.parse(dateStr);
            } catch (Exception e2) {
                return null;
            }
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
