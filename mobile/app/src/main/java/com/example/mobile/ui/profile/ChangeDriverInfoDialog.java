package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.mobile.databinding.DialogChangeDriverInfoBinding;
import com.example.mobile.models.CreateDriverRequest;
import com.example.mobile.models.DriverChangeRequestCreated;
import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.models.VehicleInformation;
import com.example.mobile.models.VehicleType;
import com.example.mobile.utils.ClientUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangeDriverInfoDialog extends BottomSheetDialogFragment {

    private DialogChangeDriverInfoBinding binding;
    private Long driverId;
    private String token;

    // Factory method to create instance with userId AND token
    public static ChangeDriverInfoDialog newInstance(Long driverId, String token) {
        ChangeDriverInfoDialog dialog = new ChangeDriverInfoDialog();
        Bundle args = new Bundle();
        args.putLong("driverId", driverId);
        args.putString("token", token);
        dialog.setArguments(args);
        return dialog;
    }

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
        if (getArguments() != null) {
            this.driverId = getArguments().getLong("driverId");
            this.token = getArguments().getString("token");
        }
        setupVehicleTypeSpinner();
        binding.btnSubmit.setOnClickListener(v -> handleSubmit());

    }
    private void setupVehicleTypeSpinner() {
        // Define the three vehicle types you want
        String[] vehicleTypes = {"STANDARD", "VAN", "LUXURY"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                vehicleTypes
        );

        binding.spinnerVehicleType.setAdapter(adapter);

        // Set default to STANDARD - false prevents filtering
        binding.spinnerVehicleType.setText("STANDARD", false);
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
        String model = text(binding.inputVehicleModel);

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
                        TextUtils.isEmpty(passengerSeatsStr) ||
                        TextUtils.isEmpty(model);

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
                model,
                vehicleType, licensePlate, passengerSeats,
                babyTransport, petTransport);
    }

    private void updateDriverInfo(String name, String surname, String email, String phone,
                                  String address, String model, String vehicleType, String licensePlate,
                                  int passengerSeats, boolean babyTransport, boolean petTransport) {

        VehicleInformation vehicle = new VehicleInformation(model, VehicleType.valueOf(vehicleType), licensePlate, passengerSeats, babyTransport, petTransport, driverId);
        // Create request object (update this based on your actual request model)
        CreateDriverRequest request = new CreateDriverRequest(name, surname, email, phone, address, vehicle);


        Gson gson = new Gson();
        String jsonString = gson.toJson(request);
        RequestBody driverData = RequestBody.create(MediaType.parse("application/json"), jsonString);

        MultipartBody.Part imagePart = null;
        String authToken = "Bearer " + token;

        Call<DriverChangeRequestCreated> call = ClientUtils.driverService.updateDriverInfo(
                driverId, driverData, imagePart, authToken
        );

        call.enqueue(new Callback<DriverChangeRequestCreated>() {
            @Override
            public void onResponse(@NonNull Call<DriverChangeRequestCreated> call,
                                   @NonNull Response<DriverChangeRequestCreated> response) {
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
            public void onFailure(@NonNull Call<DriverChangeRequestCreated> call, @NonNull Throwable t) {
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
        binding.inputName.setText("");
        binding.inputName.setError(null);

        binding.inputSurname.setText("");
        binding.inputSurname.setError(null);

        binding.inputEmail.setText("");
        binding.inputEmail.setError(null);

        binding.inputAddress.setText("");
        binding.inputAddress.setError(null);

        binding.inputPhone.setText("");
        binding.inputPhone.setError(null);

        binding.inputVehicleModel.setText("");
        binding.inputVehicleModel.setError(null);

        binding.inputLicensePlate.setText("");
        binding.inputLicensePlate.setError(null);

        binding.spinnerVehicleType.setText("STANDARD", false);  // Reset to default
        binding.inputPassengerSeats.setText("");
        binding.inputPassengerSeats.setError(null);

        binding.switchBabyTransport.setChecked(false);
        binding.switchPetTransport.setChecked(false);


        binding.inputReason.setText("");
        binding.inputReason.setError(null);
    }


    private String text(@NonNull TextView tv) {
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}