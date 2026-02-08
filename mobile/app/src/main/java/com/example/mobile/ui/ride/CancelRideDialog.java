package com.example.mobile.ui.ride;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;

public class CancelRideDialog extends DialogFragment {

    public static final String REQUEST_KEY = "cancel_ride_request";
    public static final String KEY_REASON = "reason";

    private static final String ARG_IS_DRIVER = "is_driver";

    private EditText etReason;
    private TextView tvError;
    private Button btnConfirm;
    private Button btnKeep;
    private ProgressBar progressBar;
    private boolean isDriver;

    public static CancelRideDialog newInstance(boolean isDriver) {
        CancelRideDialog dialog = new CancelRideDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DRIVER, isDriver);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        isDriver = getArguments() != null && getArguments().getBoolean(ARG_IS_DRIVER, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancel_ride, null);

        etReason = root.findViewById(R.id.et_reason);
        tvError = root.findViewById(R.id.tv_error);
        btnConfirm = root.findViewById(R.id.btn_confirm_cancel);
        btnKeep = root.findViewById(R.id.btn_keep_ride);
        progressBar = root.findViewById(R.id.progress_bar);

        TextView tvTitle = root.findViewById(R.id.tv_dialog_title);
        TextView tvReasonLabel = root.findViewById(R.id.tv_reason_label);
        TextView tvRequiredHint = root.findViewById(R.id.tv_required_hint);

        if (isDriver) {
            tvTitle.setText("Cancel Ride");
            tvReasonLabel.setText("Reason for cancellation *");
            tvRequiredHint.setVisibility(View.VISIBLE);
            tvRequiredHint.setText("Required — visible to the passenger and admins.");
        } else {
            tvTitle.setText("Cancel Ride");
            tvReasonLabel.setText("Reason for cancellation (optional)");
            tvRequiredHint.setVisibility(View.VISIBLE);
            tvRequiredHint.setText("You can cancel scheduled rides up to 10 minutes before start.");
        }

        btnKeep.setOnClickListener(v -> dismiss());

        btnConfirm.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (isDriver && reason.isEmpty()) {
                tvError.setText("Please provide a reason for cancellation.");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            tvError.setVisibility(View.GONE);
            Bundle result = new Bundle();
            result.putString(KEY_REASON, reason);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });

        builder.setView(root);
        return builder.create();
    }
}
