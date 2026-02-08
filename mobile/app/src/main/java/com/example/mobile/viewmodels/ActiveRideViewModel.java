package com.example.mobile.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.ErrorResponse;
import com.example.mobile.models.RideCancellationRequest;
import com.example.mobile.models.RideResponse;
import com.example.mobile.services.RideService;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActiveRideViewModel extends AndroidViewModel {

    private final SharedPreferencesManager preferencesManager;
    private final RideService rideService;

    private final MutableLiveData<RideResponse> ride = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cancelSuccess = new MutableLiveData<>();

    public ActiveRideViewModel(@NonNull Application application) {
        super(application);
        preferencesManager = new SharedPreferencesManager(application);
        rideService = ClientUtils.getAuthenticatedRideService(preferencesManager);
    }

    public LiveData<RideResponse> getRide() { return ride; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getCancelSuccess() { return cancelSuccess; }

    public void loadRide(long rideId) {
        loading.setValue(true);
        rideService.getRide(rideId).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse> call, @NonNull Response<RideResponse> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    ride.setValue(response.body());
                } else {
                    error.setValue("Failed to load ride");
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse> call, @NonNull Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void cancelRide(long rideId, String reason) {
        loading.setValue(true);
        error.setValue(null);

        RideCancellationRequest request = new RideCancellationRequest(reason);
        rideService.cancelRide(rideId, request).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse> call, @NonNull Response<RideResponse> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    ride.setValue(response.body());
                    cancelSuccess.setValue(true);
                } else {
                    String msg = parseError(response);
                    error.setValue(msg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse> call, @NonNull Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public String getUserRole() {
        return preferencesManager.getUserRole();
    }

    public Long getUserId() {
        return preferencesManager.getUserId();
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                ErrorResponse err = new Gson().fromJson(json, ErrorResponse.class);
                if (err != null && err.getMessage() != null) {
                    return err.getMessage();
                }
            }
        } catch (Exception ignored) {}
        return "Cancellation failed (code " + response.code() + ")";
    }
}
