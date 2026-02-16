package com.example.mobile.ui.ride;

import android.app.Dialog;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dialog for drivers to stop (end early) a ride at the vehicle's current location.
 * The backend recalculates cost based on distance traveled so far.
 */
public class StopRideDialog extends DialogFragment {

    public static final String REQUEST_KEY = "stop_ride_result";
    public static final String KEY_STOPPED = "stopped";
    private static final String ARG_RIDE_ID = "ride_id";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_ADDRESS = "address";

    private LinearLayout errorContainer;
    private TextView tvError;
    private TextView tvCurrentLocation;
    private MaterialButton btnKeep;
    private MaterialButton btnConfirmStop;
    private boolean isStopping = false;

    /**
     * Create a new StopRideDialog with the ride ID and current vehicle coordinates.
     */
    public static StopRideDialog newInstance(long rideId, double latitude, double longitude, String address) {
        StopRideDialog dialog = new StopRideDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_RIDE_ID, rideId);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putString(ARG_ADDRESS, address != null ? address : "");
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_stop_ride, null);

        tvCurrentLocation = root.findViewById(R.id.tv_current_location);
        errorContainer = root.findViewById(R.id.error_container);
        tvError = root.findViewById(R.id.tv_error);
        btnKeep = root.findViewById(R.id.btn_keep_ride);
        btnConfirmStop = root.findViewById(R.id.btn_confirm_stop);

        long rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, -1) : -1;
        double latitude = getArguments() != null ? getArguments().getDouble(ARG_LATITUDE, 0) : 0;
        double longitude = getArguments() != null ? getArguments().getDouble(ARG_LONGITUDE, 0) : 0;
        String address = getArguments() != null ? getArguments().getString(ARG_ADDRESS, "") : "";

        // Display the current location coordinates
        if (address != null && !address.isEmpty()) {
            tvCurrentLocation.setText(address);
        } else {
            tvCurrentLocation.setText(String.format("%.5f, %.5f", latitude, longitude));
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
