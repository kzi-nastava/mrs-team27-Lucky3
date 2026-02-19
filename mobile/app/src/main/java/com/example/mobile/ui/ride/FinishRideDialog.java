package com.example.mobile.ui.ride;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.example.mobile.models.EndRideRequest;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dialog for drivers to finish a ride normally.
 * Requires confirmation that passengers have exited and payment is confirmed.
 */
public class FinishRideDialog extends DialogFragment {

    public static final String REQUEST_KEY = "finish_ride_result";
    public static final String KEY_FINISHED = "finished";
    private static final String ARG_RIDE_ID = "ride_id";

    private CheckBox cbPassengersExited;
    private CheckBox cbPaymentConfirmed;
    private LinearLayout errorContainer;
    private TextView tvError;
    private MaterialButton btnCancel;
    private MaterialButton btnConfirmFinish;
    private boolean isFinishing = false;

    public static FinishRideDialog newInstance(long rideId) {
        FinishRideDialog dialog = new FinishRideDialog();
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
                .inflate(R.layout.dialog_finish_ride, null);

        cbPassengersExited = root.findViewById(R.id.cb_passengers_exited);
        cbPaymentConfirmed = root.findViewById(R.id.cb_payment_confirmed);
        errorContainer = root.findViewById(R.id.error_container);
        tvError = root.findViewById(R.id.tv_error);
        btnCancel = root.findViewById(R.id.btn_cancel);
        btnConfirmFinish = root.findViewById(R.id.btn_confirm_finish);

        long rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, -1) : -1;

        // Initially disable confirm until both checkboxes are checked
        updateConfirmButtonState();

        cbPassengersExited.setOnCheckedChangeListener((buttonView, isChecked) -> updateConfirmButtonState());
        cbPaymentConfirmed.setOnCheckedChangeListener((buttonView, isChecked) -> updateConfirmButtonState());

        btnCancel.setOnClickListener(v -> {
            if (!isFinishing) dismiss();
        });

        btnConfirmFinish.setOnClickListener(v -> confirmFinish(rideId));

        builder.setView(root);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void updateConfirmButtonState() {
        boolean enabled = cbPassengersExited.isChecked() && cbPaymentConfirmed.isChecked() && !isFinishing;
        btnConfirmFinish.setEnabled(enabled);
        btnConfirmFinish.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void confirmFinish(long rideId) {
        if (!cbPassengersExited.isChecked() || !cbPaymentConfirmed.isChecked()) {
            showError("Please confirm both checkboxes.");
            return;
        }

        isFinishing = true;
        updateConfirmButtonState();
        btnCancel.setEnabled(false);
        hideError();

        SharedPreferencesManager prefs = new SharedPreferencesManager(requireContext());
        String token = "Bearer " + prefs.getToken();

        EndRideRequest request = new EndRideRequest(true, true);

        ClientUtils.rideService.endRide(rideId, request, token)
                .enqueue(new Callback<RideResponse>() {
                    @Override
                    public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                        isFinishing = false;
                        if (!isAdded()) return;

                        if (response.isSuccessful()) {
                            Bundle result = new Bundle();
                            result.putBoolean(KEY_FINISHED, true);
                            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                            dismiss();
                        } else {
                            updateConfirmButtonState();
                            btnCancel.setEnabled(true);
                            String errorMsg = parseErrorMessage(response);
                            showError(errorMsg != null ? errorMsg : "Failed to finish ride.");
                        }
                    }

                    @Override
                    public void onFailure(Call<RideResponse> call, Throwable t) {
                        isFinishing = false;
                        if (!isAdded()) return;
                        updateConfirmButtonState();
                        btnCancel.setEnabled(true);
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
