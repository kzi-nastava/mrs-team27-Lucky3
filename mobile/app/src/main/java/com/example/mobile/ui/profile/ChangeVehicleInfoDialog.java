package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.TextView;
import com.example.mobile.databinding.DialogChangePersonalInfoBinding;
import com.example.mobile.databinding.DialogChangeVehicleInfoBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ChangeVehicleInfoDialog extends BottomSheetDialogFragment {

    private DialogChangeVehicleInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogChangeVehicleInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String model = text(binding.inputModel);
        String plate = text(binding.inputPlate);
        String year = text(binding.inputYear);
        String color = text(binding.inputColor);
        String reason = text(binding.inputReason);

        boolean hasEmpty =
                TextUtils.isEmpty(model) ||
                        TextUtils.isEmpty(plate) ||
                        TextUtils.isEmpty(year) ||
                        TextUtils.isEmpty(color) ||
                        TextUtils.isEmpty(reason);

        // Always show ONE popup based on situation
        if (hasEmpty) {
            showPopup("Error", "Please fill in all fields before submitting.");
            return;
        }

        // Optional: validate year is reasonable (simple)
        int yearInt = parseYear(year);
        if (yearInt == -1) {
            showPopup("Error", "Year must be a valid number.");
            return;
        }
        if (yearInt < 1900 || yearInt > 2100) {
            showPopup("Error", "Year looks incorrect. Please enter a valid year.");
            return;
        }

        // TODO: call backend / ViewModel to submit request

        showPopup("Success", "Your vehicle change request was submitted successfully.");
    }

    private void showPopup(String title, String message) {
        // prevent double taps -> multiple dialogs
        binding.btnSubmit.setEnabled(false);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    clearInputs();
                    dismiss(); // close bottom sheet (delete form)
                })
                .show();
    }

    private void clearInputs() {
        binding.inputModel.setText("");
        binding.inputPlate.setText("");
        binding.inputYear.setText("");
        binding.inputColor.setText("");
        binding.inputReason.setText("");
    }

    private String text(@NonNull TextView tv) {
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private int parseYear(@NonNull String year) {
        try {
            return Integer.parseInt(year);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}