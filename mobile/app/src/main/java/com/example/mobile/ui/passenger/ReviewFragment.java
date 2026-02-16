package com.example.mobile.ui.passenger;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.ReviewRequest;
import com.example.mobile.models.ReviewTokenValidationResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Review Fragment — allows passengers to rate a completed ride.
 * Supports two modes:
 * 1. Authenticated: navigated from ride detail with rideId arg (user is logged in).
 * 2. Token-based: opened via deep link from email with reviewToken arg.
 */
public class ReviewFragment extends Fragment {

    private static final String TAG = "ReviewFragment";

    // Arguments
    private long rideId = -1L;
    private String reviewToken = null;

    // Ratings
    private int driverRating = 0;
    private int vehicleRating = 0;

    // Views
    private View loadingContainer;
    private View expiredContainer;
    private View invalidContainer;
    private View successContainer;
    private View reviewFormContainer;
    private LinearLayout driverStarsContainer;
    private LinearLayout vehicleStarsContainer;
    private TextView tvDriverRatingHint;
    private TextView tvVehicleRatingHint;
    private EditText etComment;
    private TextView tvCommentCount;
    private TextView tvErrorMessage;
    private TextView btnSubmit;
    private TextView tvValidationHint;

    // State
    private boolean isSubmitting = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId", -1L);
            reviewToken = getArguments().getString("reviewToken", null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupStars();
        setupCommentCounter();
        setupButtons(view);
        initializeMode();
    }

    private void bindViews(View view) {
        loadingContainer = view.findViewById(R.id.loading_container);
        expiredContainer = view.findViewById(R.id.expired_container);
        invalidContainer = view.findViewById(R.id.invalid_container);
        successContainer = view.findViewById(R.id.success_container);
        reviewFormContainer = view.findViewById(R.id.review_form_container);
        driverStarsContainer = view.findViewById(R.id.driver_stars_container);
        vehicleStarsContainer = view.findViewById(R.id.vehicle_stars_container);
        tvDriverRatingHint = view.findViewById(R.id.tv_driver_rating_hint);
        tvVehicleRatingHint = view.findViewById(R.id.tv_vehicle_rating_hint);
        etComment = view.findViewById(R.id.et_comment);
        tvCommentCount = view.findViewById(R.id.tv_comment_count);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnSubmit = view.findViewById(R.id.btn_submit_review);
        tvValidationHint = view.findViewById(R.id.tv_validation_hint);
    }

    private void setupStars() {
        // Create 5 star TextViews for driver rating
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            TextView starView = createStarView();
            starView.setOnClickListener(v -> {
                driverRating = star;
                updateStars(driverStarsContainer, driverRating);
                tvDriverRatingHint.setText(driverRating + " of 5 stars");
                tvDriverRatingHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
                updateSubmitState();
            });
            driverStarsContainer.addView(starView);
        }

        // Create 5 star TextViews for vehicle rating
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            TextView starView = createStarView();
            starView.setOnClickListener(v -> {
                vehicleRating = star;
                updateStars(vehicleStarsContainer, vehicleRating);
                tvVehicleRatingHint.setText(vehicleRating + " of 5 stars");
                tvVehicleRatingHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
                updateSubmitState();
            });
            vehicleStarsContainer.addView(starView);
        }
    }

    private TextView createStarView() {
        TextView tv = new TextView(requireContext());
        tv.setText("☆");
        tv.setTextSize(36);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }

    private void updateStars(LinearLayout container, int rating) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView star = (TextView) container.getChildAt(i);
            if (i < rating) {
                star.setText("★");
                star.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            } else {
                star.setText("☆");
                star.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600));
            }
        }
    }

    private void setupCommentCounter() {
        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                tvCommentCount.setText(s.length() + "/500");
            }
        });
    }

    private void setupButtons(View view) {
        btnSubmit.setOnClickListener(v -> submitReview());

        // "Go Home" buttons for error states
        view.findViewById(R.id.btn_expired_go_home).setOnClickListener(v -> goBack());
        view.findViewById(R.id.btn_invalid_go_home).setOnClickListener(v -> goBack());
        view.findViewById(R.id.btn_success_done).setOnClickListener(v -> goBack());
    }

    private void initializeMode() {
        if (reviewToken != null && !reviewToken.isEmpty()) {
            // Token-based mode — validate the token first
            showState(loadingContainer);
            validateToken();
        } else if (rideId > 0) {
            // Authenticated mode — show the form directly
            showState(reviewFormContainer);
        } else {
            // No valid argument
            showState(invalidContainer);
        }
    }

    // ======================== Token Validation ========================

    private void validateToken() {
        ClientUtils.reviewService.validateReviewToken(reviewToken).enqueue(new Callback<ReviewTokenValidationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewTokenValidationResponse> call,
                                   @NonNull Response<ReviewTokenValidationResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    showState(reviewFormContainer);
                } else if (response.code() == 410) {
                    showState(expiredContainer);
                } else {
                    showState(invalidContainer);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewTokenValidationResponse> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Token validation failed", t);
                showState(invalidContainer);
            }
        });
    }

    // ======================== Submission ========================

    private void submitReview() {
        if (isSubmitting || driverRating == 0 || vehicleRating == 0) return;

        isSubmitting = true;
        tvErrorMessage.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        String comment = etComment.getText().toString().trim();

        if (reviewToken != null && !reviewToken.isEmpty()) {
            submitWithToken(comment);
        } else {
            submitAuthenticated(comment);
        }
    }

    private void submitAuthenticated(String comment) {
        SharedPreferencesManager prefs = new SharedPreferencesManager(requireContext());
        String token = "Bearer " + prefs.getToken();

        ReviewRequest request = new ReviewRequest(rideId, driverRating, vehicleRating,
                comment.isEmpty() ? null : comment);

        ClientUtils.reviewService.createReview(request, token).enqueue(new Callback<RideResponse.ReviewInfo>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse.ReviewInfo> call,
                                   @NonNull Response<RideResponse.ReviewInfo> response) {
                if (!isAdded()) return;
                handleSubmitResponse(response);
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse.ReviewInfo> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                handleSubmitFailure(t);
            }
        });
    }

    private void submitWithToken(String comment) {
        ReviewRequest request = new ReviewRequest(reviewToken, driverRating, vehicleRating,
                comment.isEmpty() ? null : comment);

        ClientUtils.reviewService.createReviewWithToken(request).enqueue(new Callback<RideResponse.ReviewInfo>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse.ReviewInfo> call,
                                   @NonNull Response<RideResponse.ReviewInfo> response) {
                if (!isAdded()) return;
                handleSubmitResponse(response);
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse.ReviewInfo> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                handleSubmitFailure(t);
            }
        });
    }

    private void handleSubmitResponse(Response<RideResponse.ReviewInfo> response) {
        isSubmitting = false;
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Review");

        if (response.isSuccessful()) {
            showState(successContainer);
        } else if (response.code() == 410) {
            showState(expiredContainer);
        } else if (response.code() == 409) {
            showError("You have already submitted a review for this ride.");
        } else {
            String msg = "Failed to submit review. Please try again.";
            try {
                if (response.errorBody() != null) {
                    String errorJson = response.errorBody().string();
                    // Try to extract message from ErrorResponse JSON
                    if (errorJson.contains("\"message\"")) {
                        int start = errorJson.indexOf("\"message\"") + 11;
                        int end = errorJson.indexOf("\"", start);
                        if (end > start) {
                            msg = errorJson.substring(start, end);
                        }
                    }
                }
            } catch (Exception ignored) {}
            showError(msg);
        }
    }

    private void handleSubmitFailure(Throwable t) {
        isSubmitting = false;
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Review");
        Log.e(TAG, "Review submission failed", t);
        showError("Network error. Please check your connection and try again.");
    }

    // ======================== UI Helpers ========================

    private void showState(View visibleContainer) {
        loadingContainer.setVisibility(View.GONE);
        expiredContainer.setVisibility(View.GONE);
        invalidContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.GONE);
        reviewFormContainer.setVisibility(View.GONE);
        visibleContainer.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void updateSubmitState() {
        boolean canSubmit = driverRating > 0 && vehicleRating > 0 && !isSubmitting;
        btnSubmit.setEnabled(canSubmit);
        tvValidationHint.setVisibility(canSubmit ? View.GONE : View.VISIBLE);
    }

    private void goBack() {
        if (isAdded()) {
            try {
                Navigation.findNavController(requireView()).popBackStack();
            } catch (Exception e) {
                // If we can't pop back (e.g., deep link with no back stack),
                // navigate to passenger home or guest home
                try {
                    SharedPreferencesManager prefs = new SharedPreferencesManager(requireContext());
                    String role = prefs.getUserRole();
                    if ("PASSENGER".equals(role)) {
                        Navigation.findNavController(requireView()).navigate(R.id.nav_passenger_home);
                    } else {
                        Navigation.findNavController(requireView()).navigate(R.id.nav_guest_home);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to navigate back", ex);
                }
            }
        }
    }
}
