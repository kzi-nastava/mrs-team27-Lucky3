package com.example.mobile.ui.ride;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.example.mobile.models.RideCancellationRequest;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverCancelRideDialog extends DialogFragment {

    public static final String REQUEST_KEY = "driver_cancel_ride_result";
    public static final String KEY_CANCELLED = "cancelled";
    private static final String ARG_RIDE_ID = "ride_id";

    private EditText etReason;
    private LinearLayout errorContainer;
    private TextView tvError;
    private MaterialButton btnKeep;
    private MaterialButton btnConfirm;
    private boolean isCancelling = false;

    public static DriverCancelRideDialog newInstance(long rideId) {
        DriverCancelRideDialog dialog = new DriverCancelRideDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_RIDE_ID, rideId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_cancel_ride_driver, null);

        etReason = root.findViewById(R.id.et_cancel_reason);
        errorContainer = root.findViewById(R.id.error_container);
        tvError = root.findViewById(R.id.tv_error);
        btnKeep = root.findViewById(R.id.btn_keep_ride);
        btnConfirm = root.findViewById(R.id.btn_confirm_cancel);

        long rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, -1) : -1;

        btnKeep.setOnClickListener(v -> {
            if (!isCancelling) dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                showError("You must provide a reason for cancellation.");
                return;
            }
            confirmCancel(rideId, reason);
        });

        builder.setView(root);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void confirmCancel(long rideId, String reason) {
        isCancelling = true;
        btnConfirm.setEnabled(false);
        btnKeep.setEnabled(false);
        hideError();

        SharedPreferencesManager prefs = new SharedPreferencesManager(requireContext());
        String token = "Bearer " + prefs.getToken();
        RideCancellationRequest request = new RideCancellationRequest(reason);

        ClientUtils.rideService.cancelRide(rideId, request, token)
                .enqueue(new Callback<RideResponse>() {
                    @Override
                    public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                        isCancelling = false;
                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            Bundle result = new Bundle();
                            result.putBoolean(KEY_CANCELLED, true);
                            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                            dismiss();
                        } else {
                            btnConfirm.setEnabled(true);
                            btnKeep.setEnabled(true);
                            String errorMsg = parseErrorMessage(response);
                            showError(errorMsg != null ? errorMsg : "Failed to cancel ride.");
                        }
                    }

                    @Override
                    public void onFailure(Call<RideResponse> call, Throwable t) {
                        isCancelling = false;
                        if (!isAdded()) return;
                        btnConfirm.setEnabled(true);
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
