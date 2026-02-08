package com.example.mobile.ui.passenger;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

/**
 * Modern dialog for ordering rides matching the web version design.
 * Features pickup, destination, intermediate stops, vehicle type selection,
 * and additional options for pet/baby transport.
 */
public class OrderRideDialog extends DialogFragment {

    public static final String REQUEST_KEY = "order_ride_result";
    public static final String KEY_PICKUP = "pickup";
    public static final String KEY_DESTINATION = "destination";
    public static final String KEY_STOPS = "stops";
    public static final String KEY_VEHICLE_TYPE = "vehicle_type";
    public static final String KEY_BABY_TRANSPORT = "baby_transport";
    public static final String KEY_PET_TRANSPORT = "pet_transport";

    private EditText etPickup;
    private EditText etDestination;
    private LinearLayout stopsContainer;
    private LinearLayout btnAddStop;
    private RadioGroup rgVehicleType;
    private RadioButton rbStandard;
    private RadioButton rbLuxury;
    private RadioButton rbVan;
    private CheckBox cbPetTransport;
    private CheckBox cbBabyTransport;
    private LinearLayout errorContainer;
    private TextView tvError;
    private TextView tvVehicleError;
    private MaterialButton btnCancel;
    private MaterialButton btnOrder;
    private ImageButton btnClose;

    private final ArrayList<EditText> stopEditTexts = new ArrayList<>();

    public static OrderRideDialog newInstance() {
        return new OrderRideDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_Mobile_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_order_ride, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        etPickup = view.findViewById(R.id.et_pickup);
        etDestination = view.findViewById(R.id.et_destination);
        stopsContainer = view.findViewById(R.id.stops_container);
        btnAddStop = view.findViewById(R.id.btn_add_stop);
        rgVehicleType = view.findViewById(R.id.rg_vehicle_type);
        rbStandard = view.findViewById(R.id.rb_standard);
        rbLuxury = view.findViewById(R.id.rb_luxury);
        rbVan = view.findViewById(R.id.rb_van);
        cbPetTransport = view.findViewById(R.id.cb_pet_transport);
        cbBabyTransport = view.findViewById(R.id.cb_baby_transport);
        errorContainer = view.findViewById(R.id.error_container);
        tvError = view.findViewById(R.id.tv_error);
        tvVehicleError = view.findViewById(R.id.tv_vehicle_error);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnOrder = view.findViewById(R.id.btn_order);
        btnClose = view.findViewById(R.id.btn_close);

        // Setup listeners
        setupListeners();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setupListeners() {
        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Cancel button
        btnCancel.setOnClickListener(v -> dismiss());

        // Add stop button
        btnAddStop.setOnClickListener(v -> addStopField());

        // Order button
        btnOrder.setOnClickListener(v -> submitOrder());

        // Clear vehicle error when selection changes
        rgVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                tvVehicleError.setVisibility(View.GONE);
            }
        });

        // Clear general error when user types
        etPickup.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) hideError();
        });
        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) hideError();
        });
    }

    private void addStopField() {
        View stopView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_stop_input, stopsContainer, false);

        EditText etStop = stopView.findViewById(R.id.et_stop);
        ImageButton btnRemove = stopView.findViewById(R.id.btn_remove_stop);

        int stopNumber = stopEditTexts.size() + 1;
        etStop.setHint("Stop " + stopNumber);

        btnRemove.setOnClickListener(v -> {
            stopsContainer.removeView(stopView);
            stopEditTexts.remove(etStop);
            updateStopHints();
        });

        stopEditTexts.add(etStop);
        stopsContainer.addView(stopView);
    }

    private void updateStopHints() {
        for (int i = 0; i < stopEditTexts.size(); i++) {
            stopEditTexts.get(i).setHint("Stop " + (i + 1));
        }
    }

    private void submitOrder() {
        // Validate inputs
        String pickup = etPickup.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();

        if (pickup.isEmpty()) {
            showError("Please enter a pickup address");
            etPickup.requestFocus();
            return;
        }

        if (destination.isEmpty()) {
            showError("Please enter a destination address");
            etDestination.requestFocus();
            return;
        }

        // Validate vehicle type
        String vehicleType = getSelectedVehicleType();
        if (vehicleType == null) {
            tvVehicleError.setVisibility(View.VISIBLE);
            return;
        }

        // Collect stops
        ArrayList<String> stops = new ArrayList<>();
        stops.add(pickup); // First location is pickup
        for (EditText etStop : stopEditTexts) {
            String stop = etStop.getText().toString().trim();
            if (!stop.isEmpty()) {
                stops.add(stop);
            }
        }
        stops.add(destination); // Last location is destination

        // Collect additional options
        boolean petTransport = cbPetTransport.isChecked();
        boolean babyTransport = cbBabyTransport.isChecked();

        // Build result bundle
        Bundle result = new Bundle();
        result.putString(KEY_PICKUP, pickup);
        result.putString(KEY_DESTINATION, destination);
        result.putStringArrayList(KEY_STOPS, stops);
        result.putString(KEY_VEHICLE_TYPE, vehicleType);
        result.putBoolean(KEY_PET_TRANSPORT, petTransport);
        result.putBoolean(KEY_BABY_TRANSPORT, babyTransport);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private String getSelectedVehicleType() {
        if (rbStandard.isChecked()) return "STANDARD";
        if (rbLuxury.isChecked()) return "LUXURY";
        if (rbVan.isChecked()) return "VAN";
        return null;
    }

    private void showError(String message) {
        tvError.setText(message);
        errorContainer.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorContainer.setVisibility(View.GONE);
    }
}
