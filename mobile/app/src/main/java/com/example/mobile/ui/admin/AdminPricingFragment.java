package com.example.mobile.ui.admin;

import com.example.mobile.R;
import com.example.mobile.models.UpdateVehiclePriceRequest;
import com.example.mobile.models.VehiclePriceResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPricingFragment extends Fragment {

    private static final String TAG = "AdminPricingFragment";

    // Views
    private TextView tvLoading;
    private LinearLayout layoutError;
    private TextView tvError;
    private Button btnRetry;
    private LinearLayout layoutContent;

    // Standard card
    private TextView tvStandardBaseFare;
    private TextView tvStandardPerKm;
    private TextView tvStandardEstimate;
    private ImageButton btnEditStandard;

    // Luxury card
    private TextView tvLuxuryBaseFare;
    private TextView tvLuxuryPerKm;
    private TextView tvLuxuryEstimate;
    private ImageButton btnEditLuxury;

    // Van card
    private TextView tvVanBaseFare;
    private TextView tvVanPerKm;
    private TextView tvVanEstimate;
    private ImageButton btnEditVan;

    // Data
    private final Map<String, VehiclePriceResponse> prices = new HashMap<>();
    private SharedPreferencesManager prefsManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_pricing, container, false);

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Pricing");
        }

        prefsManager = new SharedPreferencesManager(requireContext());

        bindViews(root);
        setupListeners();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPrices();
    }

    private void bindViews(View root) {
        tvLoading = root.findViewById(R.id.tv_loading);
        layoutError = root.findViewById(R.id.layout_error);
        tvError = root.findViewById(R.id.tv_error);
        btnRetry = root.findViewById(R.id.btn_retry);
        layoutContent = root.findViewById(R.id.layout_content);

        tvStandardBaseFare = root.findViewById(R.id.tv_standard_base_fare);
        tvStandardPerKm = root.findViewById(R.id.tv_standard_per_km);
        tvStandardEstimate = root.findViewById(R.id.tv_standard_estimate);
        btnEditStandard = root.findViewById(R.id.btn_edit_standard);

        tvLuxuryBaseFare = root.findViewById(R.id.tv_luxury_base_fare);
        tvLuxuryPerKm = root.findViewById(R.id.tv_luxury_per_km);
        tvLuxuryEstimate = root.findViewById(R.id.tv_luxury_estimate);
        btnEditLuxury = root.findViewById(R.id.btn_edit_luxury);

        tvVanBaseFare = root.findViewById(R.id.tv_van_base_fare);
        tvVanPerKm = root.findViewById(R.id.tv_van_per_km);
        tvVanEstimate = root.findViewById(R.id.tv_van_estimate);
        btnEditVan = root.findViewById(R.id.btn_edit_van);
    }

    private void setupListeners() {
        btnRetry.setOnClickListener(v -> loadPrices());
        btnEditStandard.setOnClickListener(v -> openEditDialog("STANDARD", "Standard"));
        btnEditLuxury.setOnClickListener(v -> openEditDialog("LUXURY", "Luxury"));
        btnEditVan.setOnClickListener(v -> openEditDialog("VAN", "Van"));
    }

    // ===================== DATA LOADING =====================

    private void loadPrices() {
        showLoading();

        String token = "Bearer " + prefsManager.getToken();

        ClientUtils.adminService.getAllVehiclePrices(token).enqueue(new Callback<List<VehiclePriceResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<VehiclePriceResponse>> call,
                                   @NonNull Response<List<VehiclePriceResponse>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    prices.clear();
                    for (VehiclePriceResponse p : response.body()) {
                        prices.put(p.getVehicleType(), p);
                    }
                    updateCardValues();
                    showContent();
                } else {
                    showError("Failed to load pricing data (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VehiclePriceResponse>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load prices", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void updateCardValues() {
        updateCard("STANDARD", tvStandardBaseFare, tvStandardPerKm, tvStandardEstimate);
        updateCard("LUXURY", tvLuxuryBaseFare, tvLuxuryPerKm, tvLuxuryEstimate);
        updateCard("VAN", tvVanBaseFare, tvVanPerKm, tvVanEstimate);
    }

    private void updateCard(String type, TextView baseFareView, TextView perKmView, TextView estimateView) {
        VehiclePriceResponse price = prices.get(type);
        if (price == null) {
            baseFareView.setText("N/A");
            perKmView.setText("N/A");
            estimateView.setText("N/A");
            return;
        }

        baseFareView.setText(formatPrice(price.getBaseFare()) + " RSD");
        perKmView.setText(formatPrice(price.getPricePerKm()) + " RSD");

        double estimate = price.getBaseFare() + price.getPricePerKm() * 5;
        estimateView.setText(formatPrice(estimate) + " RSD");
    }

    private String formatPrice(double value) {
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    // ===================== VISIBILITY HELPERS =====================

    private void showLoading() {
        tvLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showError(String message) {
        tvLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        tvError.setText(message);
    }

    private void showContent() {
        tvLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }

    // ===================== EDIT DIALOG =====================

    private void openEditDialog(String vehicleType, String label) {
        VehiclePriceResponse current = prices.get(vehicleType);
        double currentBaseFare = current != null ? current.getBaseFare() : 0;
        double currentPerKm = current != null ? current.getPricePerKm() : 0;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_pricing, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogSubtitle = dialogView.findViewById(R.id.dialog_subtitle);
        EditText editBaseFare = dialogView.findViewById(R.id.edit_base_fare);
        EditText editPricePerKm = dialogView.findViewById(R.id.edit_price_per_km);
        TextView previewTotal = dialogView.findViewById(R.id.preview_total);
        TextView previewBreakdown = dialogView.findViewById(R.id.preview_breakdown);
        TextView dialogError = dialogView.findViewById(R.id.dialog_error);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        dialogTitle.setText("Edit " + label + " Pricing");
        dialogSubtitle.setText("Update the pricing rates for " + label + " vehicles");

        // Set current values
        editBaseFare.setText(formatPrice(currentBaseFare));
        editPricePerKm.setText(formatPrice(currentPerKm));

        // Live preview calculation
        Runnable updatePreview = () -> {
            double baseFare = parseDouble(editBaseFare.getText().toString());
            double perKm = parseDouble(editPricePerKm.getText().toString());
            double total = baseFare + perKm * 5;
            previewTotal.setText(formatPrice(total) + " RSD");
            previewBreakdown.setText(formatPrice(baseFare) + " base + " + formatPrice(perKm) + " × 5 km");
        };

        // Initial preview
        updatePreview.run();

        // Auto-calculate on text change
        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview.run();
            }
        };
        editBaseFare.addTextChangedListener(previewWatcher);
        editPricePerKm.addTextChangedListener(previewWatcher);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            double baseFare = parseDouble(editBaseFare.getText().toString());
            double perKm = parseDouble(editPricePerKm.getText().toString());

            if (baseFare < 0 || perKm < 0) {
                dialogError.setText("Values must be zero or positive");
                dialogError.setVisibility(View.VISIBLE);
                return;
            }

            dialogError.setVisibility(View.GONE);
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            savePrice(vehicleType, baseFare, perKm, dialog, btnSave, dialogError);
        });

        dialog.show();
    }

    private void savePrice(String vehicleType, double baseFare, double perKm,
                           AlertDialog dialog, Button btnSave, TextView dialogError) {
        String token = "Bearer " + prefsManager.getToken();
        UpdateVehiclePriceRequest request = new UpdateVehiclePriceRequest(vehicleType, baseFare, perKm);

        ClientUtils.adminService.updateVehiclePrice(token, request).enqueue(new Callback<VehiclePriceResponse>() {
            @Override
            public void onResponse(@NonNull Call<VehiclePriceResponse> call,
                                   @NonNull Response<VehiclePriceResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    prices.put(response.body().getVehicleType(), response.body());
                    updateCardValues();
                    Toast.makeText(requireContext(), "Price updated successfully", Toast.LENGTH_SHORT).show();

                    // Auto-close after brief delay (matches web behavior)
                    new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 800);
                } else {
                    dialogError.setText("Failed to update price (" + response.code() + ")");
                    dialogError.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                }
            }

            @Override
            public void onFailure(@NonNull Call<VehiclePriceResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to update price", t);
                dialogError.setText("Network error: " + t.getMessage());
                dialogError.setVisibility(View.VISIBLE);
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
            }
        });
    }

    private double parseDouble(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
