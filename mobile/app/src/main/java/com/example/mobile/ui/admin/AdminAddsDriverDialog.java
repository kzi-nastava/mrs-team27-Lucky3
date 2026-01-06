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
import com.example.mobile.model.DriverInfoCard;
import com.google.android.material.button.MaterialButton;

public class AdminAddsDriverDialog extends DialogFragment {
    public interface OnDriverCreatedListener {
        void onDriverCreated(DriverInfoCard driver);
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
                new String[]{"Standard", "Luxury", "Van"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);


        // Set up the submit button
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
            int vehicleType = spinnerType.getSelectedItemPosition();

            //TODO: more validation needed
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(v.getContext(), "Fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }


            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext());

            //TODO: send real email...
            builder.setTitle("Driver Account Created");
            builder.setMessage("Confirmation link to set up driver's account\nhas been sent to email:\n\n" + email);
            builder.setPositiveButton("OK", (dialog, which) -> {

                dismiss(); // Close the main dialog
            });

            builder.setCancelable(false); // User must click OK
            builder.show();
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
