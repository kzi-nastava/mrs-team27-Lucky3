package com.example.mobile.ui.passenger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;

import java.util.ArrayList;

public class RequestRideFormDialog extends DialogFragment {
    public static final String REQUEST_KEY = "request_stops_result";
    public static final String KEY_LOCATIONS = "locations";
    public static final String KEY_VEHICLE_TYPE = "vehicle_type";
    public static final String KEY_BABY_TRANSPORT = "baby_transport";
    public static final String KEY_PET_TRANSPORT = "pet_transport";

    private final ArrayList<String> locations = new ArrayList<>();

    private EditText etLocation;
    private TextView tvVehicleTypeError;
    private RadioButton rbStandard;
    private RadioButton rbVan;
    private RadioButton rbLuxury;
    private CheckBox cbBabyTransport;
    private CheckBox cbPetTransport;
    private RadioGroup rgVehicleType;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_request_ride_form, null);

        TextView tvHint = root.findViewById(R.id.tvHint);
        etLocation = root.findViewById(R.id.etLocation);
        Button btnNext = root.findViewById(R.id.btnNextLocation);
        Button btnCancel = root.findViewById(R.id.btnCancel);
        Button btnSubmit = root.findViewById(R.id.btnSubmit);

        rgVehicleType = root.findViewById(R.id.rgVehicleType);

        // vehicle type radios
        rbStandard = root.findViewById(R.id.rbStandard);
        rbVan = root.findViewById(R.id.rbVan);
        rbLuxury = root.findViewById(R.id.rbLuxury);

        // transport type checkboxes (updated)
        cbBabyTransport = root.findViewById(R.id.cbBabyTransport);
        cbPetTransport = root.findViewById(R.id.cbPetTransport);

        //error message if no vehicle type is selected
        tvVehicleTypeError = root.findViewById(R.id.tvVehicleTypeError);
        // When dialog opens, hide error
        tvVehicleTypeError.setVisibility(View.GONE);

        // Hide error whenever any radio becomes checked
        rgVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) { // some option selected
                tvVehicleTypeError.setVisibility(View.GONE);
            }
        });

        ColorStateList rbTint = ContextCompat.getColorStateList(
                requireContext(),
                R.color.radio_vehicle_tint
        );

        ColorStateList cbTint = ContextCompat.getColorStateList(
                requireContext(),
                R.color.checkbox_yellow_tint
        );

        if (rbTint != null) {
            rbStandard.setButtonTintList(rbTint);
            rbVan.setButtonTintList(rbTint);
            rbLuxury.setButtonTintList(rbTint);
        }

        if (cbTint != null) {
            cbBabyTransport.setButtonTintList(cbTint);
            cbPetTransport.setButtonTintList(cbTint);
        }

        tvHint.setText("Enter first location");

        btnNext.setOnClickListener(v -> {
            String text = etLocation.getText().toString().trim();
            if (text.isEmpty()) return;

            locations.add(text);
            etLocation.setText("");
            tvHint.setText("Enter next location");
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnSubmit.setOnClickListener(v -> submitForm());

        builder.setView(root);
        return builder.create();
    }

    private String getSelectedVehicleType(RadioButton rbStandard,
                                          RadioButton rbVan,
                                          RadioButton rbLuxury) {
        if (rbStandard.isChecked()) return "STANDARD";
        if (rbVan.isChecked()) return "VAN";
        if (rbLuxury.isChecked()) return "LUXURY";
        return null; // or default
    }

    private void submitForm() {
        String text = etLocation.getText().toString().trim();
        if (!text.isEmpty()) {
            locations.add(text);
        }

        String vehicleType = getSelectedVehicleType(rbStandard, rbVan, rbLuxury);
        boolean babyTransport = cbBabyTransport.isChecked();
        boolean petTransport = cbPetTransport.isChecked();

        //validation message if no vehicle type is selected
        if (vehicleType == null || vehicleType.isEmpty()) {
            tvVehicleTypeError.setVisibility(View.VISIBLE);
            tvVehicleTypeError.setText("You must select vehicle type");
            return;
        } else {
            tvVehicleTypeError.setVisibility(View.GONE);
        }

        Bundle result = new Bundle();
        result.putStringArrayList(KEY_LOCATIONS, locations);
        result.putString(KEY_VEHICLE_TYPE, vehicleType);
        result.putBoolean(KEY_BABY_TRANSPORT, babyTransport);
        result.putBoolean(KEY_PET_TRANSPORT, petTransport);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    public static RequestRideFormDialog newInstance() {
        return new RequestRideFormDialog();
    }
}
