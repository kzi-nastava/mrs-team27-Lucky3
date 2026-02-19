package com.example.mobile.ui.ride;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.mobile.models.RidePanicRequest;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dialog for triggering the PANIC button on a ride.
 * Available to both drivers and passengers during an IN_PROGRESS ride.
 * Sends PUT /api/rides/{id}/panic with an optional reason.
 */
public class PanicDialog extends DialogFragment {

    public static final String REQUEST_KEY = "panic_result";
    public static final String KEY_PANIC_ACTIVATED = "panic_activated";
    private static final String ARG_RIDE_ID = "ride_id";

    private EditText etReason;
    private LinearLayout errorContainer;
    private TextView tvError;
    private LinearLayout successContainer;
    private LinearLayout buttonContainer;
    private MaterialButton btnCancel;
    private MaterialButton btnConfirm;
    private boolean isSending = false;

    public static PanicDialog newInstance(long rideId) {
        PanicDialog dialog = new PanicDialog();
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
                .inflate(R.layout.dialog_panic, null);

        etReason = root.findViewById(R.id.et_panic_reason);
        errorContainer = root.findViewById(R.id.error_container);
        tvError = root.findViewById(R.id.tv_error);
        successContainer = root.findViewById(R.id.success_container);
        buttonContainer = root.findViewById(R.id.button_container);
        btnCancel = root.findViewById(R.id.btn_cancel_panic);
        btnConfirm = root.findViewById(R.id.btn_confirm_panic);

        long rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, -1) : -1;

        btnCancel.setOnClickListener(v -> {
            if (!isSending) dismiss();
        });

        btnConfirm.setOnClickListener(v -> sendPanic(rideId));

        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void sendPanic(long rideId) {
        if (isSending) return;
        isSending = true;
        btnConfirm.setEnabled(false);
        btnCancel.setEnabled(false);
        hideError();

        SharedPreferencesManager prefs = new SharedPreferencesManager(requireContext());
        String token = "Bearer " + prefs.getToken();
        String reason = etReason.getText().toString().trim();
        RidePanicRequest request = new RidePanicRequest(reason.isEmpty() ? null : reason);

        ClientUtils.rideService.panicRide(rideId, request, token)
                .enqueue(new Callback<RideResponse>() {
                    @Override
                    public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                        isSending = false;
                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            showSuccess();
                            // Auto-close after 2 seconds
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (isAdded()) {
                                    Bundle result = new Bundle();
                                    result.putBoolean(KEY_PANIC_ACTIVATED, true);
                                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                                    dismiss();
                                }
                            }, 2000);
                        } else {
                            btnConfirm.setEnabled(true);
                            btnCancel.setEnabled(true);
                            String errorMsg = parseErrorMessage(response);
                            showError(errorMsg != null ? errorMsg : "Failed to send panic alert.");
                        }
                    }

                    @Override
                    public void onFailure(Call<RideResponse> call, Throwable t) {
                        isSending = false;
                        if (!isAdded()) return;
                        btnConfirm.setEnabled(true);
                        btnCancel.setEnabled(true);
                        showError("Network error. Please try again.");
                    }
                });
    }

    private void showSuccess() {
        etReason.setVisibility(View.GONE);
        buttonContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.VISIBLE);
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
                String errorJson = response.errorBody().string();
                com.example.mobile.models.ErrorResponse error =
                        new Gson().fromJson(errorJson, com.example.mobile.models.ErrorResponse.class);
                if (error != null && error.getMessage() != null) {
                    return error.getMessage();
                }
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return null;
    }
}
