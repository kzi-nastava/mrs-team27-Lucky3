package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.TextView;

import com.example.mobile.databinding.DialogChangeDriverInfoBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ChangeDriverInfoDialog extends BottomSheetDialogFragment {

    private DialogChangeDriverInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogChangeDriverInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        // Personal Information
        String name = text(binding.inputName);
        String surname = text(binding.inputSurname);
        String email = text(binding.inputEmail);
        String phone = text(binding.inputPhone);
        String address = text(binding.inputAddress);

        // Vehicle Information
        String vehicleType = binding.spinnerVehicleType.getText().toString().trim();
        String licensePlate = text(binding.inputLicensePlate);
        String passengerSeatsStr = text(binding.inputPassengerSeats);

        // Vehicle Features
        boolean babyTransport = binding.switchBabyTransport.isChecked();
        boolean petTransport = binding.switchPetTransport.isChecked();

        // Validate required fields
        boolean hasEmpty =
                TextUtils.isEmpty(name) ||
                        TextUtils.isEmpty(surname) ||
                        TextUtils.isEmpty(email) ||
                        TextUtils.isEmpty(phone) ||
                        TextUtils.isEmpty(address) ||
                        TextUtils.isEmpty(vehicleType) ||
                        TextUtils.isEmpty(licensePlate) ||
                        TextUtils.isEmpty(passengerSeatsStr);

        if (hasEmpty) {
            showPopup("Error", "Please fill in all required fields.");
            return;
        }

        // Parse passenger seats
        int passengerSeats;
        try {
            passengerSeats = Integer.parseInt(passengerSeatsStr);
            if (passengerSeats < 1 || passengerSeats > 10) {
                showPopup("Error", "Passenger seats must be between 1 and 10.");
                return;
            }
        } catch (NumberFormatException e) {
            showPopup("Error", "Please enter a valid number for passenger seats.");
            return;
        }

        // Disable button and call API
        binding.btnSubmit.setEnabled(false);

        // Call API to update driver info
        updateDriverInfo(name, surname, email, phone, address,
                vehicleType, licensePlate, passengerSeats,
                babyTransport, petTransport);
    }

    private void updateDriverInfo(String name, String surname, String email, String phone,
                                  String address, String vehicleType, String licensePlate,
                                  int passengerSeats, boolean babyTransport, boolean petTransport) {

        // Create request object (update this based on your actual request model)
        UpdateDriverInfoRequest request = new UpdateDriverInfoRequest(
                name, surname, email, phone, address,
                vehicleType, licensePlate, passengerSeats,
                babyTransport, petTransport
        );

        Gson gson = new Gson();
        String jsonString = gson.toJson(request);
        RequestBody userData = RequestBody.create(MediaType.parse("application/json"), jsonString);

        MultipartBody.Part imagePart = null;
        String authToken = "Bearer " + token;

        Call<ProfileUserResponse> call = ClientUtils.userService.updateDriverInfo(
                userId, userData, imagePart, authToken
        );

        call.enqueue(new Callback<ProfileUserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileUserResponse> call,
                                   @NonNull Response<ProfileUserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bundle result = new Bundle();
                    result.putBoolean("success", true);
                    getParentFragmentManager().setFragmentResult("updateDriverInfo", result);

                    showPopup("Success", "Your driver information was updated successfully.");
                } else {
                    binding.btnSubmit.setEnabled(true);
                    showPopup("Error", "Failed to update. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileUserResponse> call, @NonNull Throwable t) {
                binding.btnSubmit.setEnabled(true);
                showPopup("Error", "Network error: " + t.getMessage());
            }
        });
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