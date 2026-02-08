package com.example.mobile.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.example.mobile.Domain.DriverInfoCard;
import com.example.mobile.models.CreateDriverRequest;
import com.example.mobile.models.VehicleInformation;
import com.example.mobile.models.VehicleType;

import okhttp3.MultipartBody;

public class AdminAddsDriverDialog extends DialogFragment {
    public interface OnDriverCreatedListener {
        void onDriverCreated(CreateDriverRequest request, MultipartBody.Part imageFile);
    }

    private OnDriverCreatedListener listener;

    public AdminAddsDriverDialog(OnDriverCreatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_admin_adds_driver, container, false);

        EditText etName = root.findViewById(R.id.input_name);
        EditText etSurname = root.findViewById(R.id.input_surname);
        EditText etAdress = root.findViewById(R.id.input_address);
        EditText etPhone = root.findViewById(R.id.input_phone);
        EditText etEmail = root.findViewById(R.id.input_email);
        EditText etVehicleModel = root.findViewById(R.id.input_vehicle_model);
        EditText etLicensePlate = root.findViewById(R.id.input_license_plate);
        EditText etSeats = root.findViewById(R.id.input_seats);

        CheckBox cbBaby = root.findViewById(R.id.input_baby);
        CheckBox cbPet = root.findViewById(R.id.input_pet);

        Spinner spinnerType = root.findViewById(R.id.input_vehicle_type);
        AppCompatButton btnSubmit = root.findViewById(R.id.btn_submit);
        AppCompatButton btnClose = root.findViewById(R.id.btn_close);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"STANDARD", "LUXURY", "VAN"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);


        // In the submit button click listener:
        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String surname = etSurname.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAdress.getText().toString().trim();
            String vehicleModel = etVehicleModel.getText().toString().trim();
            String licensePlate = etLicensePlate.getText().toString().trim();
            String seats = etSeats.getText().toString().trim();
            boolean babyTransport = cbBaby.isChecked();
            boolean petTransport = cbPet.isChecked();
            String vehicleType = spinnerType.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(v.getContext(), "Fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (vehicleModel.isEmpty() || licensePlate.isEmpty()) {
                Toast.makeText(v.getContext(), "Vehicle information is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (seats.isEmpty()) {
                Toast.makeText(v.getContext(), "Number of seats is required", Toast.LENGTH_SHORT).show();
                return;
            }

            int seatsNumber;
            try {
                seatsNumber = Integer.parseInt(seats);
                if (seatsNumber < 1 || seatsNumber > 10) {
                    Toast.makeText(v.getContext(), "Seats must be between 1 and 10", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(v.getContext(), "Invalid number of seats", Toast.LENGTH_SHORT).show();
                return;
            }

            VehicleType vehicleTypeEnum;
            try {
                // Convert "Standard" -> "STANDARD" to match enum
                vehicleTypeEnum = VehicleType.valueOf(vehicleType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Fallback to STANDARD if conversion fails
                vehicleTypeEnum = VehicleType.STANDARD;
            }

            // Create request object
            CreateDriverRequest request = new CreateDriverRequest();
            request.setName(name);
            request.setSurname(surname);
            request.setEmail(email);
            request.setPhone(phone);
            request.setAddress(address);

            VehicleInformation vehicle = new VehicleInformation(
                    vehicleModel,
                    vehicleTypeEnum,  // Use the converted enum
                    licensePlate,
                    seatsNumber,      // Use parsed integer
                    babyTransport,
                    petTransport,
                    1
            );
            request.setVehicle(vehicle);

            MultipartBody.Part imagePart = null;
            if (listener != null) {
                listener.onDriverCreated(request, imagePart);
                dismiss();
            }
        });


        btnClose.setOnClickListener(v -> {
            dismiss();
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
