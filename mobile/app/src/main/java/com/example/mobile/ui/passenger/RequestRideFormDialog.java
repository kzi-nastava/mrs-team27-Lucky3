package com.example.mobile.ui.passenger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;

import java.util.ArrayList;

public class RequestRideFormDialog extends DialogFragment {
    public static final String REQUEST_KEY = "request_stops_result";
    public static final String KEY_LOCATIONS = "locations";
    public static final String KEY_VEHICLE_TYPE = "vehicle_type";
    public static final String KEY_TRANSPORT_TYPE = "transport_type";

    private final ArrayList<String> locations = new ArrayList<>();

    private EditText etLocation;
    private RadioButton rbStandard;
    private RadioButton rbVan;
    private RadioButton rbLuxury;
    private CheckBox cbBabyTransport;
    private CheckBox cbPetTransport;

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

        // vehicle type radios
        rbStandard = root.findViewById(R.id.rbStandard);
        rbVan = root.findViewById(R.id.rbVan);
        rbLuxury = root.findViewById(R.id.rbLuxury);

        // transport type checkboxes (updated)
        cbBabyTransport = root.findViewById(R.id.cbBabyTransport);
        cbPetTransport = root.findViewById(R.id.cbPetTransport);

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

    private void submitForm(){
        String text = etLocation.getText().toString().trim();
        if (!text.isEmpty()) {
            locations.add(text);
        }

        String vehicleType = getSelectedVehicleType(rbStandard, rbVan, rbLuxury);
        String transportType = getSelectedTransportType(cbBabyTransport, cbPetTransport);

        Bundle result = new Bundle();
        result.putString(KEY_TRANSPORT_TYPE, transportType);
        result.putStringArrayList(KEY_LOCATIONS, locations);
        result.putString(KEY_VEHICLE_TYPE, vehicleType);
        result.putString(KEY_TRANSPORT_TYPE, transportType);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private String getSelectedTransportType(CheckBox cbBaby,
                                            CheckBox cbPet) {
        boolean baby = cbBaby.isChecked();
        boolean pet = cbPet.isChecked();

        if (baby && pet) return "BABY_AND_PET";
        if (baby) return "BABY";
        if (pet) return "PET";
        return "NONE"; // no checkbox selected
    }

    public static RequestRideFormDialog newInstance() {
        return new RequestRideFormDialog();
    }
}
