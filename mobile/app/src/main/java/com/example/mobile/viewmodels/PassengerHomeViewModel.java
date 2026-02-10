package com.example.mobile.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobile.models.CreateRideRequest;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.VehicleLocationResponse;
import com.example.mobile.services.RideService;
import com.example.mobile.services.VehicleService;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerHomeViewModel extends ViewModel {
    private static final String TAG = "PassengerHomeViewModel";

    private final RideService rideService;
    private final VehicleService vehicleService;
    private final SharedPreferencesManager preferencesManager;

    // LiveData for vehicles
    private final MutableLiveData<List<VehicleLocationResponse>> vehiclesLiveData = new MutableLiveData<>();
    private final MutableLiveData<VehicleCounts> vehicleCountsLiveData = new MutableLiveData<>();

    // LiveData for active ride
    private final MutableLiveData<RideResponse> activeRideLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasActiveRideLiveData = new MutableLiveData<>(false);

    // LiveData for ride creation
    private final MutableLiveData<Resource<RideResponse>> rideCreationStateLiveData = new MutableLiveData<>();

    // LiveData for loading and errors
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> rideRejected = new MutableLiveData<>();

    public PassengerHomeViewModel(SharedPreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
        this.rideService = ClientUtils.rideService;
        this.vehicleService = ClientUtils.vehicleService;
    }

    // Getters for LiveData
    public LiveData<List<VehicleLocationResponse>> getVehicles() {
        return vehiclesLiveData;
    }

    public LiveData<VehicleCounts> getVehicleCounts() {
        return vehicleCountsLiveData;
    }

    public LiveData<RideResponse> getActiveRide() {
        return activeRideLiveData;
    }

    public LiveData<Boolean> getHasActiveRide() {
        return hasActiveRideLiveData;
    }

    public LiveData<Resource<RideResponse>> getRideCreationState() {
        return rideCreationStateLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<Boolean> getRideRejected() {
        return rideRejected;
    }

    // Fetch active vehicles
    public void fetchActiveVehicles() {
        vehicleService.getActiveVehicles().enqueue(new Callback<List<VehicleLocationResponse>>() {
            @Override
            public void onResponse(Call<List<VehicleLocationResponse>> call,
                                   Response<List<VehicleLocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VehicleLocationResponse> vehicles = response.body();
                    vehiclesLiveData.postValue(vehicles);
                    updateVehicleCounts(vehicles);
                } else {
                    Log.e(TAG, "Failed to fetch vehicles: " + response.code());
                    errorMessageLiveData.postValue("Failed to fetch vehicles");
                }
            }

            @Override
            public void onFailure(Call<List<VehicleLocationResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching vehicles", t);
                errorMessageLiveData.postValue("Network error: " + t.getMessage());
            }
        });
    }

    private void updateVehicleCounts(List<VehicleLocationResponse> vehicles) {
        int available = 0;
        int occupied = 0;

        for (VehicleLocationResponse vehicle : vehicles) {
            if (vehicle.isAvailable()) {
                available++;
            } else {
                occupied++;
            }
        }

        vehicleCountsLiveData.postValue(new VehicleCounts(available, occupied));
    }

    // Check for active or pending rides
    public void checkForActiveRide() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) {
            hasActiveRideLiveData.postValue(false);
            return;
        }

        String token = "Bearer " + preferencesManager.getToken();

        // First check for ACCEPTED rides
        rideService.getActiveRides(null, userId, "ACCEPTED", 0, 1, token)
                .enqueue(new Callback<PageResponse<RideResponse>>() {
                    @Override
                    public void onResponse(Call<PageResponse<RideResponse>> call,
                                           Response<PageResponse<RideResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getContent() != null
                                && !response.body().getContent().isEmpty()) {
                            RideResponse ride = response.body().getContent().get(0);
                            activeRideLiveData.postValue(ride);
                            hasActiveRideLiveData.postValue(true);
                        } else {
                            // If no ACCEPTED ride, check for PENDING
                            checkForPendingRide(userId, token);
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to check active rides", t);
                        errorMessageLiveData.postValue("Failed to check active rides");
                        hasActiveRideLiveData.postValue(false);
                    }
                });
    }

    private void checkForPendingRide(Long userId, String token) {
        rideService.getActiveRides(null, userId, "PENDING", 0, 1, token)
                .enqueue(new Callback<PageResponse<RideResponse>>() {
                    @Override
                    public void onResponse(Call<PageResponse<RideResponse>> call,
                                           Response<PageResponse<RideResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getContent() != null
                                && !response.body().getContent().isEmpty()) {
                            RideResponse ride = response.body().getContent().get(0);
                            activeRideLiveData.postValue(ride);
                            hasActiveRideLiveData.postValue(true);
                        } else {
                            hasActiveRideLiveData.postValue(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to check pending rides", t);
                        hasActiveRideLiveData.postValue(false);
                    }
                });
    }

    // Create a new ride
    public void createRide(CreateRideRequest rideRequest) {
        isLoadingLiveData.setValue(true);
        rideCreationStateLiveData.setValue(Resource.loading(null));

        String token = "Bearer " + preferencesManager.getToken();
        Log.d("OrderRideDialog", String.valueOf(rideRequest.getScheduledTime()));
        rideService.createRide(rideRequest, token).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                isLoadingLiveData.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    RideResponse ride = response.body();
                    if ("REJECTED".equalsIgnoreCase(ride.getStatus())) {
                        rideRejected.postValue(true);
                    } else {
                        rideCreationStateLiveData.postValue(Resource.success(ride));
                        activeRideLiveData.postValue(ride);
                        hasActiveRideLiveData.postValue(true);
                    }
                } else {
                    String error = "Failed to create ride: " + response.code();
                    rideCreationStateLiveData.postValue(Resource.error(error, null));
                    errorMessageLiveData.postValue(error);
                }
            }

            @Override
            public void onFailure(Call<RideResponse> call, Throwable t) {
                isLoadingLiveData.postValue(false);
                String error = "Network error: " + t.getMessage();
                rideCreationStateLiveData.postValue(Resource.error(error, null));
                errorMessageLiveData.postValue(error);
            }
        });
    }

    // Data classes
    public static class VehicleCounts {
        private final int available;
        private final int occupied;

        public VehicleCounts(int available, int occupied) {
            this.available = available;
            this.occupied = occupied;
        }

        public int getAvailable() {
            return available;
        }

        public int getOccupied() {
            return occupied;
        }
    }

    public static class Resource<T> {
        public enum Status {SUCCESS, ERROR, LOADING}

        private final Status status;
        private final T data;
        private final String message;

        private Resource(Status status, T data, String message) {
            this.status = status;
            this.data = data;
            this.message = message;
        }

        public static <T> Resource<T> success(T data) {
            return new Resource<>(Status.SUCCESS, data, null);
        }

        public static <T> Resource<T> error(String message, T data) {
            return new Resource<>(Status.ERROR, data, message);
        }

        public static <T> Resource<T> loading(T data) {
            return new Resource<>(Status.LOADING, data, null);
        }

        public Status getStatus() {
            return status;
        }

        public T getData() {
            return data;
        }

        public String getMessage() {
            return message;
        }
    }
}
