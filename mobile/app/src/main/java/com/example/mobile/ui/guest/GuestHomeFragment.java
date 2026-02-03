package com.example.mobile.ui.guest;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
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

        btnEstimateRide.setOnClickListener(v -> {
            showEstimateRideDialog();
        });

        setupVehicleRefresh();

        return binding.getRoot();
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

    private void showEstimateRideDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        // Inflate custom layout
        View dialogView =  LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ride_estimation, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background for rounded corners
        }

        EditText etPickup = dialogView.findViewById(R.id.et_pickup_address);
        EditText etDestination = dialogView.findViewById(R.id.et_destination_address);
        MaterialButton btnEstimate = dialogView.findViewById(R.id.btn_estimate_confirm);
        ImageView btnClose = dialogView.findViewById(R.id.btn_close);

        // New Views
        LinearLayout llResults = dialogView.findViewById(R.id.ll_estimation_results);
        TextView tvDistance = dialogView.findViewById(R.id.tv_distance_value);
        TextView tvDuration = dialogView.findViewById(R.id.tv_duration_value);
        TextView tvPrice = dialogView.findViewById(R.id.tv_price_value);
        LinearLayout llActions = dialogView.findViewById(R.id.ll_action_buttons);
        MaterialButton btnClear = dialogView.findViewById(R.id.btn_clear);
        MaterialButton btnRecalculate = dialogView.findViewById(R.id.btn_recalculate);
        TextView tvLoginHint = dialogView.findViewById(R.id.tv_login_hint);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        View.OnClickListener estimateAction = v -> {
            String startAddress = etPickup.getText().toString().trim();
            String destAddress = etDestination.getText().toString().trim();

            if (!startAddress.isEmpty() && !destAddress.isEmpty()) {
                performEstimation(startAddress, destAddress, 
                    est -> {
                        // Success callback
                        requireActivity().runOnUiThread(() -> {
                             tvDistance.setText(String.format("%.2f km", est.getEstimatedDistance()));
                             tvDuration.setText(String.format("%d min", est.getEstimatedTimeInMinutes()));
                             tvPrice.setText(String.format("%.0f RSD", est.getEstimatedCost()));
                             
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
            if (mapRenderer != null) mapRenderer.clearMap();
        });

        dialog.show();
    }

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
