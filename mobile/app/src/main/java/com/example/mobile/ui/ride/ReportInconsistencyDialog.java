package com.example.mobile.ui.ride;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.example.mobile.models.InconsistencyRequest;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportInconsistencyDialog extends DialogFragment {

    private static final String ARG_RIDE_ID = "ride_id";

    private EditText etRemark;
    private LinearLayout errorContainer;
    private TextView tvError;
    private MaterialButton btnCancel;
    private MaterialButton btnSubmit;
    private boolean isSubmitting = false;

    public static ReportInconsistencyDialog newInstance(long rideId) {
        ReportInconsistencyDialog dialog = new ReportInconsistencyDialog();
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
                .inflate(R.layout.dialog_report_inconsistency, null);

        etRemark = root.findViewById(R.id.et_remark);
        errorContainer = root.findViewById(R.id.error_container);
        tvError = root.findViewById(R.id.tv_error);
        btnCancel = root.findViewById(R.id.btn_cancel);
        btnSubmit = root.findViewById(R.id.btn_submit);

        long rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, -1) : -1;

        btnCancel.setOnClickListener(v -> {
            if (!isSubmitting) dismiss();
        });

        btnSubmit.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (remark.isEmpty()) {
                showError("Please describe the inconsistency.");
                return;
            }
            submitReport(rideId, remark);
        });

        builder.setView(root);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void submitReport(long rideId, String remark) {
        if (isSubmitting) return;
        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");
        hideError();

        String token = "Bearer " + new SharedPreferencesManager(requireContext()).getToken();
        InconsistencyRequest request = new InconsistencyRequest(remark);

        ClientUtils.rideService.reportInconsistency(rideId, request, token)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        isSubmitting = false;
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Inconsistency reported successfully", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Submit");
                            if (response.code() == 409) {
                                showError("Cannot report inconsistency for this ride state.");
                            } else {
                                showError("Failed to submit report. Please try again.");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        isSubmitting = false;
                        if (!isAdded()) return;
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                        showError("Network error. Please try again.");
                    }
                });
    }

    private void showError(String message) {
        errorContainer.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

    private void hideError() {
        errorContainer.setVisibility(View.GONE);
    }
}
