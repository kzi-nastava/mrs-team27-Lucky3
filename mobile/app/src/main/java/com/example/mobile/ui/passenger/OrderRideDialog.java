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
    public static final String KEY_SCHEDULED = "scheduled";
    public static final String KEY_SCHEDULE_OFFSET_MIN = "schedule_offset_min";
    public static final String KEY_SCHEDULE_TIME_MILLIS = "schedule_time_millis";


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

    private RadioGroup rgWhen;
    private RadioButton rbNow;
    private RadioButton rbLater;
    private LinearLayout containerLaterOptions;
    private TextView tvWhenSummary;

    private Integer selectedOffsetMinutes = null;
    private final ArrayList<EditText> stopEditTexts = new ArrayList<>();

    private String prefilledPickup = null;
    private String prefilledDestination = null;


    public static OrderRideDialog newInstanceWithData(String pickup, String destination) {
        OrderRideDialog dialog = new OrderRideDialog();
        Bundle args = new Bundle();
        args.putString("prefilled_pickup", pickup);
        args.putString("prefilled_destination", destination);
        dialog.setArguments(args);
        return dialog;
    }

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
        rgWhen = view.findViewById(R.id.rg_when);
        rbNow = view.findViewById(R.id.rb_now);
        rbLater = view.findViewById(R.id.rb_later);
        containerLaterOptions = view.findViewById(R.id.container_later_options);
        tvWhenSummary = view.findViewById(R.id.tv_when_summary);

        // Check for prefilled data from arguments
        Bundle args = getArguments();
        if (args != null) {
            prefilledPickup = args.getString("prefilled_pickup");
            prefilledDestination = args.getString("prefilled_destination");

            if (prefilledPickup != null) {
                etPickup.setText(prefilledPickup);
            }
            if (prefilledDestination != null) {
                etDestination.setText(prefilledDestination);
            }
        }

        // Initialize card containers for vehicle selection
        LinearLayout cardStandard = view.findViewById(R.id.card_standard);
        LinearLayout cardLuxury = view.findViewById(R.id.card_luxury);
        LinearLayout cardVan = view.findViewById(R.id.card_van);

        // Make entire card clickable and ensure only one radio button is selected
        cardStandard.setOnClickListener(v -> {
            rbStandard.setChecked(true);
            rbLuxury.setChecked(false);
            rbVan.setChecked(false);
        });

        cardLuxury.setOnClickListener(v -> {
            rbStandard.setChecked(false);
            rbLuxury.setChecked(true);
            rbVan.setChecked(false);
        });

        cardVan.setOnClickListener(v -> {
            rbStandard.setChecked(false);
            rbLuxury.setChecked(false);
            rbVan.setChecked(true);
        });

        // Setup listeners
        setupListeners();

        // When: Now vs Later toggle
        rgWhen.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_now) {
                containerLaterOptions.setVisibility(View.GONE);
                selectedOffsetMinutes = null;
                tvWhenSummary.setText("Ride will start immediately");
            } else if (checkedId == R.id.rb_later) {
                containerLaterOptions.setVisibility(View.VISIBLE);
                if (selectedOffsetMinutes == null) {
                    tvWhenSummary.setText("Select when the ride should start");
                }
            }
        });

        // Time preset chips
        View.OnClickListener chipClickListener = v -> {
            int minutes = 0;
            int id = v.getId();

            if (id == R.id.chip_15) minutes = 15;
            else if (id == R.id.chip_30) minutes = 30;
            else if (id == R.id.chip_45) minutes = 45;
            else if (id == R.id.chip_60) minutes = 60;
            else if (id == R.id.chip_120) minutes = 120;
            else if (id == R.id.chip_180) minutes = 180;
            else if (id == R.id.chip_240) minutes = 240;
            else if (id == R.id.chip_300) minutes = 300;

            selectedOffsetMinutes = minutes;
            highlightSelectedChip(v);
            updateWhenSummary();
        };

        view.findViewById(R.id.chip_15).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_30).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_45).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_60).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_120).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_180).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_240).setOnClickListener(chipClickListener);
        view.findViewById(R.id.chip_300).setOnClickListener(chipClickListener);
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

        // Validate scheduling
        if (rbLater.isChecked() && selectedOffsetMinutes == null) {
            showError("Please select when the ride should start");
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
        // Add scheduling info
        result.putBoolean(KEY_SCHEDULED, rbLater.isChecked());
        if (rbLater.isChecked() && selectedOffsetMinutes != null) {
            result.putInt(KEY_SCHEDULE_OFFSET_MIN, selectedOffsetMinutes);

            long scheduledTimeMillis = System.currentTimeMillis() + (selectedOffsetMinutes * 60L * 1000L);
            result.putLong(KEY_SCHEDULE_TIME_MILLIS, scheduledTimeMillis);

        }

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private void highlightSelectedChip(View selectedView) {
        // Reset all chips
        resetChipBackground(R.id.chip_15);
        resetChipBackground(R.id.chip_30);
        resetChipBackground(R.id.chip_45);
        resetChipBackground(R.id.chip_60);
        resetChipBackground(R.id.chip_120);
        resetChipBackground(R.id.chip_180);
        resetChipBackground(R.id.chip_240);
        resetChipBackground(R.id.chip_300);

        // Highlight selected
        selectedView.setBackgroundColor(Color.parseColor("#FFC107")); // yellow_500
        ((TextView) selectedView).setTextColor(Color.BLACK);
    }

    private void resetChipBackground(int chipId) {
        View chip = getView().findViewById(chipId);
        if (chip != null) {
            chip.setBackgroundResource(R.drawable.bg_card2);
            ((TextView) chip).setTextColor(getResources().getColor(R.color.gray_300));
        }
    }

    private void updateWhenSummary() {
        if (selectedOffsetMinutes == null) {
            tvWhenSummary.setText("Select when the ride should start");
            return;
        }

        if (selectedOffsetMinutes < 60) {
            tvWhenSummary.setText("Ride will start in " + selectedOffsetMinutes + " minutes");
        } else {
            int hours = selectedOffsetMinutes / 60;
            tvWhenSummary.setText("Ride will start in " + hours + (hours == 1 ? " hour" : " hours"));
        }
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
