package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile.databinding.DialogChangePersonalInfoBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ChangePersonalInfoDialog extends BottomSheetDialogFragment {

    private DialogChangePersonalInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogChangePersonalInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String name = text(binding.inputName);
        String surname = text(binding.inputSurname);
        String email = text(binding.inputEmail);
        String phone = text(binding.inputPhone);
        String address = text(binding.inputAddress);

        boolean hasEmpty =
                TextUtils.isEmpty(name) ||
                        TextUtils.isEmpty(surname) ||
                        TextUtils.isEmpty(email) ||
                        TextUtils.isEmpty(phone) ||
                        TextUtils.isEmpty(address);

        // Always show ONE popup based on situation
        if (hasEmpty) {
            showPopup(
                    "Error",
                    "Please fill in all fields before submitting."
            );
        } else {
            // TODO: Here you can call your backend / ViewModel to submit request
            showPopup(
                    "Success",
                    "Your request was submitted successfully."
            );
        }
    }

    private void showPopup(String title, String message) {
        // prevent multiple dialogs by double click
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
        binding.inputName.setText("");
        binding.inputSurname.setText("");
        binding.inputEmail.setText("");
        binding.inputPhone.setText("");
        binding.inputAddress.setText("");
    }

    private String text(@NonNull android.widget.EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
