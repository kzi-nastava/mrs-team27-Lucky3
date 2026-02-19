package com.example.mobile.ui.guest;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentGuestHomeBinding;
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.services.VehicleService;
import com.example.mobile.ui.maps.RideMapRenderer;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;
import com.example.mobile.models.CreateRideRequest;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideEstimationResponse;
import com.example.mobile.services.RideService;
import com.example.mobile.utils.ClientUtils;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuestHomeFragment extends Fragment {
    private static final String TAG = "GuestHomeFragment";
    private static final int REFRESH_INTERVAL_MS = 10000;

    // State keys for surviving configuration changes (rotation)
    private static final String STATE_PANEL_VISIBLE = "panel_visible";
    private static final String STATE_RESULTS_VISIBLE = "results_visible";
    private static final String STATE_PICKUP = "pickup_address";
    private static final String STATE_DESTINATION = "dest_address";
    private static final String STATE_DISTANCE = "est_distance";
    private static final String STATE_DURATION = "est_duration";
    private static final String STATE_PRICE = "est_price";

    private FragmentGuestHomeBinding binding;
    private MapView map;
    private TextView tvAvailableCount;
    private TextView tvOccupiedCount;
    private TextView btnSignIn;
    private MaterialButton btnCreateAccount;
    private MaterialButton btnEstimateRide;

    private RideMapRenderer mapRenderer;
    private final List<Marker> vehicleMarkers = new ArrayList<>();
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    private int availableCount = 0;
    private int occupiedCount = 0;

    // Cached estimation results for state restoration
    private String lastDistance = null;
    private String lastDuration = null;
    private String lastPrice = null;
    private boolean panelVisible = false;
    private boolean resultsVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGuestHomeBinding.inflate(inflater, container, false);

        tvAvailableCount = binding.tvAvailableCount;
        tvOccupiedCount = binding.tvOccupiedCount;
        btnSignIn = binding.btnSignIn;
        btnCreateAccount = binding.btnCreateAccount;
        btnEstimateRide = binding.btnEstimateRide;

        map = binding.map;
        mapRenderer = new RideMapRenderer(requireActivity(), map);
        mapRenderer.initMap();

        btnSignIn.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_login);
        });

        btnCreateAccount.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_register);
        });

        // Initialize estimation panel logic
        initEstimationPanel(binding.getRoot());

        btnEstimateRide.setOnClickListener(v -> {
             // Show panel, hide button
             binding.estimationContainer.setVisibility(View.VISIBLE);
             panelVisible = true;
             btnEstimateRide.setVisibility(View.GONE);
        });

        // Restore state after rotation
        if (savedInstanceState != null) {
            restoreEstimationState(savedInstanceState, binding.getRoot());
        }

        setupVehicleRefresh();

        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding == null) return;

        outState.putBoolean(STATE_PANEL_VISIBLE, panelVisible);
        outState.putBoolean(STATE_RESULTS_VISIBLE, resultsVisible);

        EditText etPickup = binding.getRoot().findViewById(R.id.et_pickup_address);
        EditText etDest = binding.getRoot().findViewById(R.id.et_destination_address);
        if (etPickup != null) outState.putString(STATE_PICKUP, etPickup.getText().toString());
        if (etDest != null) outState.putString(STATE_DESTINATION, etDest.getText().toString());

        if (lastDistance != null) outState.putString(STATE_DISTANCE, lastDistance);
        if (lastDuration != null) outState.putString(STATE_DURATION, lastDuration);
        if (lastPrice != null) outState.putString(STATE_PRICE, lastPrice);
    }

    private void restoreEstimationState(Bundle state, View root) {
        panelVisible = state.getBoolean(STATE_PANEL_VISIBLE, false);
        resultsVisible = state.getBoolean(STATE_RESULTS_VISIBLE, false);

        if (panelVisible) {
            binding.estimationContainer.setVisibility(View.VISIBLE);
            btnEstimateRide.setVisibility(View.GONE);

            EditText etPickup = root.findViewById(R.id.et_pickup_address);
            EditText etDest = root.findViewById(R.id.et_destination_address);
            String pickup = state.getString(STATE_PICKUP, "");
            String dest = state.getString(STATE_DESTINATION, "");
            if (etPickup != null) etPickup.setText(pickup);
            if (etDest != null) etDest.setText(dest);

            if (resultsVisible) {
                lastDistance = state.getString(STATE_DISTANCE);
                lastDuration = state.getString(STATE_DURATION);
                lastPrice = state.getString(STATE_PRICE);

                LinearLayout llResults = root.findViewById(R.id.ll_estimation_results);
                LinearLayout llActions = root.findViewById(R.id.ll_action_buttons);
                TextView tvLoginHint = root.findViewById(R.id.tv_login_hint);
                MaterialButton btnEstimate = root.findViewById(R.id.btn_confirm_estimate);
                TextView tvDistance = root.findViewById(R.id.tv_distance_value);
                TextView tvDuration = root.findViewById(R.id.tv_duration_value);
                TextView tvPrice = root.findViewById(R.id.tv_price_value);

                if (lastDistance != null) tvDistance.setText(lastDistance);
                if (lastDuration != null) tvDuration.setText(lastDuration);
                if (lastPrice != null) tvPrice.setText(lastPrice);

                llResults.setVisibility(View.VISIBLE);
                llActions.setVisibility(View.VISIBLE);
                tvLoginHint.setVisibility(View.VISIBLE);
                btnEstimate.setVisibility(View.GONE);

                // Re-draw the route on the map
                if (!pickup.isEmpty() && !dest.isEmpty()) {
                    performEstimation(pickup, dest,
                        est -> requireActivity().runOnUiThread(() ->
                            mapRenderer.showRoute(est.getRoutePoints())),
                        error -> { /* ignore — results already shown from saved state */ }
                    );
                }
            }
        }
    }

    private void initEstimationPanel(View rootView) {
        // Find views included in fragment layout now
        LinearLayout panel = rootView.findViewById(R.id.estimation_container);
        if(panel == null) return; 

        ImageView btnClose = panel.findViewById(R.id.btn_close_estimation);
        EditText etPickup = panel.findViewById(R.id.et_pickup_address);
        EditText etDestination = panel.findViewById(R.id.et_destination_address);
        MaterialButton btnEstimate = panel.findViewById(R.id.btn_confirm_estimate);
        
        LinearLayout llResults = panel.findViewById(R.id.ll_estimation_results);
        TextView tvDistance = panel.findViewById(R.id.tv_distance_value);
        TextView tvDuration = panel.findViewById(R.id.tv_duration_value);
        TextView tvPrice = panel.findViewById(R.id.tv_price_value);
        
        LinearLayout llActions = panel.findViewById(R.id.ll_action_buttons);
        MaterialButton btnClear = panel.findViewById(R.id.btn_clear);
        MaterialButton btnRecalculate = panel.findViewById(R.id.btn_recalculate);
        TextView tvLoginHint = panel.findViewById(R.id.tv_login_hint);

        // Setup Login Hint clickable spans
        setupLoginHint(tvLoginHint);

        btnClose.setOnClickListener(v -> {
             panel.setVisibility(View.GONE);
             panelVisible = false;
             resultsVisible = false;
             lastDistance = null;
             lastDuration = null;
             lastPrice = null;
             btnEstimateRide.setVisibility(View.VISIBLE);
             if (mapRenderer != null) mapRenderer.clearMap();
             
             // Reset form
             etPickup.setText("");
             etDestination.setText("");
             llResults.setVisibility(View.GONE);
             llActions.setVisibility(View.GONE);
             tvLoginHint.setVisibility(View.GONE);
             btnEstimate.setVisibility(View.VISIBLE);
        });

        View.OnClickListener estimateAction = v -> {
            String startAddress = etPickup.getText().toString().trim();
            String destAddress = etDestination.getText().toString().trim();

            if (!startAddress.isEmpty() && !destAddress.isEmpty()) {
                performEstimation(startAddress, destAddress, 
                    est -> {
                        requireActivity().runOnUiThread(() -> {
                             String distStr = String.format("%.2f km", est.getEstimatedDistance());
                             String durStr = String.format("%d min", est.getEstimatedTimeInMinutes());
                             String priceStr = String.format("%.0f RSD", est.getEstimatedCost());

                             tvDistance.setText(distStr);
                             tvDuration.setText(durStr);
                             tvPrice.setText(priceStr);

                             // Cache results for rotation
                             lastDistance = distStr;
                             lastDuration = durStr;
                             lastPrice = priceStr;
                             resultsVisible = true;
                             
                             llResults.setVisibility(View.VISIBLE);
                             llActions.setVisibility(View.VISIBLE);
                             tvLoginHint.setVisibility(View.VISIBLE);
                             btnEstimate.setVisibility(View.GONE);
                             
                             mapRenderer.showRoute(est.getRoutePoints());
                        });
                    },
                    error -> {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());
                    }
                );
            } else {
                Toast.makeText(requireContext(), "Please enter both addresses", Toast.LENGTH_SHORT).show();
            }
        };

        btnEstimate.setOnClickListener(estimateAction);
        btnRecalculate.setOnClickListener(estimateAction);

        btnClear.setOnClickListener(v -> {
            etPickup.setText("");
            etDestination.setText("");
            llResults.setVisibility(View.GONE);
            llActions.setVisibility(View.GONE);
            tvLoginHint.setVisibility(View.GONE);
            btnEstimate.setVisibility(View.VISIBLE);
            resultsVisible = false;
            lastDistance = null;
            lastDuration = null;
            lastPrice = null;
            if (mapRenderer != null) mapRenderer.clearMap();
        });
    }

    private void setupLoginHint(TextView tv) {
        String text = "Sign in or create an account to book this ride";
        SpannableString ss = new SpannableString(text);

        // "Sign in"
        ClickableSpan signInSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Navigation.findNavController(widget).navigate(R.id.nav_login);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            }
        };
        int signInStart = text.indexOf("Sign in");
        ss.setSpan(signInSpan, signInStart, signInStart + 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // "create an account"
        ClickableSpan createAccountSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Navigation.findNavController(widget).navigate(R.id.nav_register);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            }
        };
        int createAccountStart = text.indexOf("create an account");
        ss.setSpan(createAccountSpan, createAccountStart, createAccountStart + 17, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        tv.setText(ss);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setHighlightColor(Color.TRANSPARENT);
    }


    private void setupVehicleRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchActiveVehicles();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }

    private void fetchActiveVehicles() {
        VehicleService vehicleService = ClientUtils.vehicleService;
        vehicleService.getActiveVehicles().enqueue(new Callback<List<VehicleLocationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<VehicleLocationResponse>> call,
                                   @NonNull Response<List<VehicleLocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VehicleLocationResponse> vehicles = response.body();
                    updateVehicleMarkers(vehicles);
                    updateVehicleCounts(vehicles);
                } else {
                    Log.e(TAG, "Failed to fetch vehicles: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VehicleLocationResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching vehicles", t);
            }
        });
    }

    private void updateVehicleMarkers(List<VehicleLocationResponse> vehicles) {
        for (Marker marker : vehicleMarkers) {
            map.getOverlays().remove(marker);
        }
        vehicleMarkers.clear();

        for (VehicleLocationResponse vehicle : vehicles) {
            Marker marker = new Marker(map);
            GeoPoint position = new GeoPoint(vehicle.getLatitude(), vehicle.getLongitude());
            marker.setPosition(position);
            marker.setTitle(vehicle.getVehicleType());
            marker.setSnippet(vehicle.isAvailable() ? "Available" : "Occupied");

            Drawable icon = vehicle.isAvailable()
                    ? ContextCompat.getDrawable(requireContext(), R.drawable.ic_vehicle_available)
                    : ContextCompat.getDrawable(requireContext(), R.drawable.ic_vehicle_occupied);

            if (icon != null) {
                marker.setIcon(icon);
            }

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            map.getOverlays().add(marker);
            vehicleMarkers.add(marker);
        }

        map.invalidate();
    }

    private void updateVehicleCounts(List<VehicleLocationResponse> vehicles) {
        availableCount = 0;
        occupiedCount = 0;

        for (VehicleLocationResponse vehicle : vehicles) {
            if (vehicle.isAvailable()) {
                availableCount++;
            } else {
                occupiedCount++;
            }
        }

        if (tvAvailableCount != null) {
            tvAvailableCount.setText(String.format("Available (%d)", availableCount));
        }
        if (tvOccupiedCount != null) {
            tvOccupiedCount.setText(String.format("Occupied (%d)", occupiedCount));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        fetchActiveVehicles();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
        binding = null;
    }
    
    // showEstimateRideDialog removed as it is now integrated into the layout


    private void performEstimation(String startAddress, String destAddress, 
                                   final Consumer<RideEstimationResponse> onSuccess, 
                                   final Consumer<String> onError) {
        new Thread(() -> {
            GeoPoint startPoint = mapRenderer.geocodeLocation(startAddress);
            GeoPoint destPoint = mapRenderer.geocodeLocation(destAddress);

            if (startPoint != null && destPoint != null) {
                // Prepare request
                LocationDto start = new LocationDto(startAddress, startPoint.getLatitude(), startPoint.getLongitude());
                LocationDto dest = new LocationDto(destAddress, destPoint.getLatitude(), destPoint.getLongitude());
                CreateRideRequest request = new CreateRideRequest(start, dest);

                RideService rideService = ClientUtils.rideService;
                rideService.estimateRide(request).enqueue(new Callback<RideEstimationResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RideEstimationResponse> call, @NonNull Response<RideEstimationResponse> response) {
                         if (response.isSuccessful() && response.body() != null) {
                             if (onSuccess != null) onSuccess.accept(response.body());
                         } else {
                             Log.e(TAG, "Estimation failed: " + response.code() + " " + response.message());
                             String errMsg = "Estimation failed";
                             try {
                                 if (response.errorBody() != null) {
                                    errMsg += ": " + response.errorBody().string();
                                 }
                             } catch (Exception e) {}
                             if (onError != null) onError.accept(errMsg);
                         }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RideEstimationResponse> call, @NonNull Throwable t) {
                         Log.e(TAG, "Error estimating ride", t);
                         if (onError != null) onError.accept("Error connecting to server");
                    }
                });
            } else {
                 if (onError != null) onError.accept("Could not find address locations");
            }
        }).start();
    }
}
