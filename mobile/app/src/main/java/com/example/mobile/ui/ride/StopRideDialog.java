package com.example.mobile.ui.ride;

import android.app.Dialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.RideStopRequest;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dialog for drivers to stop (end early) a ride at the vehicle's current location.
 * Shows cost summary, route info, and savings — similar to the web stop-early modal.
 * The backend recalculates cost based on distance traveled so far.
 */
public class StopRideDialog extends DialogFragment {

    public static final String REQUEST_KEY = "stop_ride_result";
    public static final String KEY_STOPPED = "stopped";
    private static final String ARG_RIDE_ID = "ride_id";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_CURRENT_COST = "current_cost";
    private static final String ARG_ESTIMATED_COST = "estimated_cost";
    private static final String ARG_VEHICLE_TYPE = "vehicle_type";
    private static final String ARG_PICKUP_ADDRESS = "pickup_address";
    private static final String ARG_DROPOFF_ADDRESS = "dropoff_address";
    private static final String ARG_DISTANCE_LEFT = "distance_left";
    private static final String ARG_COMPLETED_STOPS = "completed_stops";

    private LinearLayout errorContainer;
    private TextView tvError;
    private TextView tvCurrentLocation;
    private MaterialButton btnKeep;
    private MaterialButton btnConfirmStop;
    private boolean isStopping = false;

    /**
     * Create a new StopRideDialog with ride data for the stop-early summary.
     */
    public static StopRideDialog newInstance(long rideId, double latitude, double longitude,
                                             String address, double currentCost, double estimatedCost,
                                             String vehicleType, String pickupAddress,
                                             String dropoffAddress, double distanceLeftKm,
                                             ArrayList<String> completedStops) {
        StopRideDialog dialog = new StopRideDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_RIDE_ID, rideId);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putString(ARG_ADDRESS, address != null ? address : "");
        args.putDouble(ARG_CURRENT_COST, currentCost);
        args.putDouble(ARG_ESTIMATED_COST, estimatedCost);
        args.putString(ARG_VEHICLE_TYPE, vehicleType != null ? vehicleType : "");
        args.putString(ARG_PICKUP_ADDRESS, pickupAddress != null ? pickupAddress : "");
        args.putString(ARG_DROPOFF_ADDRESS, dropoffAddress != null ? dropoffAddress : "");
        args.putDouble(ARG_DISTANCE_LEFT, distanceLeftKm);
        args.putStringArrayList(ARG_COMPLETED_STOPS, completedStops);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_stop_ride, null);

        // Bind views
        tvCurrentLocation = root.findViewById(R.id.tv_current_location);
        errorContainer = root.findViewById(R.id.error_container);
        tvError = root.findViewById(R.id.tv_error);
        btnKeep = root.findViewById(R.id.btn_keep_ride);
        btnConfirmStop = root.findViewById(R.id.btn_confirm_stop);

        TextView tvCurrentCost = root.findViewById(R.id.tv_current_cost);
        TextView tvEstimatedCost = root.findViewById(R.id.tv_estimated_cost);
        TextView tvSavings = root.findViewById(R.id.tv_savings);
        TextView tvRideType = root.findViewById(R.id.tv_ride_type);
        TextView tvDistanceLeft = root.findViewById(R.id.tv_distance_left);
        TextView tvPickupAddress = root.findViewById(R.id.tv_pickup_address);
        TextView tvDropoffAddress = root.findViewById(R.id.tv_dropoff_address);
        LinearLayout completedStopsContainer = root.findViewById(R.id.completed_stops_container);

        // Extract arguments
        Bundle args = getArguments();
        long rideId = args != null ? args.getLong(ARG_RIDE_ID, -1) : -1;
        double latitude = args != null ? args.getDouble(ARG_LATITUDE, 0) : 0;
        double longitude = args != null ? args.getDouble(ARG_LONGITUDE, 0) : 0;
        String address = args != null ? args.getString(ARG_ADDRESS, "") : "";
        double currentCost = args != null ? args.getDouble(ARG_CURRENT_COST, 0) : 0;
        double estimatedCost = args != null ? args.getDouble(ARG_ESTIMATED_COST, 0) : 0;
        String vehicleType = args != null ? args.getString(ARG_VEHICLE_TYPE, "") : "";
        String pickupAddr = args != null ? args.getString(ARG_PICKUP_ADDRESS, "") : "";
        String dropoffAddr = args != null ? args.getString(ARG_DROPOFF_ADDRESS, "") : "";
        double distanceLeftKm = args != null ? args.getDouble(ARG_DISTANCE_LEFT, -1) : -1;
        ArrayList<String> completedStops = args != null ? args.getStringArrayList(ARG_COMPLETED_STOPS) : null;

        // Current location
        if (address != null && !address.isEmpty()) {
            tvCurrentLocation.setText(address);
        } else {
            tvCurrentLocation.setText(String.format(Locale.US, "%.5f, %.5f", latitude, longitude));
        }

        // Cost summary
        if (currentCost > 0) {
            tvCurrentCost.setText(String.format(Locale.US, "RSD %.0f", currentCost));
        }
        if (estimatedCost > 0) {
            tvEstimatedCost.setText(String.format(Locale.US, "RSD %.0f", estimatedCost));
        }
        double savings = estimatedCost - currentCost;
        if (estimatedCost > 0 && savings > 0) {
            tvSavings.setText(String.format(Locale.US, "RSD %.0f", savings));
        } else {
            tvSavings.setText("—");
        }

        // Ride type
        if (vehicleType != null && !vehicleType.isEmpty()) {
            tvRideType.setText(vehicleType.toUpperCase(Locale.US));
        }

        // Distance left
        if (distanceLeftKm >= 0) {
            if (distanceLeftKm >= 1.0) {
                tvDistanceLeft.setText(String.format(Locale.US, "%.1f km", distanceLeftKm));
            } else {
                tvDistanceLeft.setText(String.format(Locale.US, "%d m", Math.round(distanceLeftKm * 1000)));
            }
        }

        // Route: pickup
        if (pickupAddr != null && !pickupAddr.isEmpty()) {
            tvPickupAddress.setText(pickupAddr);
        }

        // Route: dropoff (strikethrough already set in XML via paintFlags)
        if (dropoffAddr != null && !dropoffAddr.isEmpty()) {
            tvDropoffAddress.setText(dropoffAddr);
        }
        tvDropoffAddress.setPaintFlags(tvDropoffAddress.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Completed stops
        if (completedStops != null && !completedStops.isEmpty()) {
            completedStopsContainer.setVisibility(View.VISIBLE);
            for (String stop : completedStops) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);

                TextView checkmark = new TextView(requireContext());
                checkmark.setText("✓ ");
                checkmark.setTextColor(requireContext().getResources().getColor(R.color.green_500, null));
                checkmark.setTextSize(13);
                row.addView(checkmark);

                TextView stopLabel = new TextView(requireContext());
                stopLabel.setText(stop);
                stopLabel.setTextColor(requireContext().getResources().getColor(R.color.white, null));
                stopLabel.setTextSize(13);
                row.addView(stopLabel);

                completedStopsContainer.addView(row);
            }
        }

        btnKeep.setOnClickListener(v -> {
            if (!isStopping) dismiss();
        });

        btnConfirmStop.setOnClickListener(v -> confirmStop(rideId, latitude, longitude, address));

        builder.setView(root);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void confirmStop(long rideId, double latitude, double longitude, String address) {
        isStopping = true;
        btnConfirmStop.setEnabled(false);
        btnKeep.setEnabled(false);
        hideError();

        SharedPreferencesManager prefs = new SharedPreferencesManager(requireContext());
        String token = "Bearer " + prefs.getToken();

        String locationAddress = (address != null && !address.isEmpty())
                ? address
                : String.format("%.5f, %.5f", latitude, longitude);
        LocationDto stopLocation = new LocationDto(locationAddress, latitude, longitude);
        RideStopRequest request = new RideStopRequest(stopLocation);

        ClientUtils.rideService.stopRide(rideId, request, token)
                .enqueue(new Callback<RideResponse>() {
                    @Override
                    public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                        isStopping = false;
                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            Bundle result = new Bundle();
                            result.putBoolean(KEY_STOPPED, true);
                            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                            dismiss();
                        } else {
                            btnConfirmStop.setEnabled(true);
                            btnKeep.setEnabled(true);
                            String errorMsg = parseErrorMessage(response);
                            showError(errorMsg != null ? errorMsg : "Failed to stop ride.");
                        }
                    }

                    @Override
                    public void onFailure(Call<RideResponse> call, Throwable t) {
                        isStopping = false;
                        if (!isAdded()) return;
                        btnConfirmStop.setEnabled(true);
                        btnKeep.setEnabled(true);
                        showError("Network error. Please try again.");
                    }
                });
    }

    private void showError(String message) {
        if (errorContainer != null && tvError != null) {
            tvError.setText(message);
            errorContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (errorContainer != null) {
            errorContainer.setVisibility(View.GONE);
        }
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                com.example.mobile.models.ErrorResponse err =
                        new Gson().fromJson(json, com.example.mobile.models.ErrorResponse.class);
                if (err != null && err.getMessage() != null) return err.getMessage();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
