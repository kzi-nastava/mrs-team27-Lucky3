package com.example.mobile.ui.passenger;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerHistoryViewModel extends ViewModel {

    private static final String TAG = "PassengerHistoryVM";

    private final MutableLiveData<List<RideResponse>> rides = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<List<RideResponse>> getRides() {
        return rides;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadRides() {
        isLoading.setValue(true);
        error.setValue(null);

        String token = SharedPreferencesManager.getInstance().getToken();
        Long passengerId = SharedPreferencesManager.getInstance().getUserId();

        if (token == null || passengerId == null) {
            error.setValue("Authentication required");
            isLoading.setValue(false);
            return;
        }

        Call<List<RideResponse>> call = ClientUtils.rideService.getRidesForPassenger(
                "Bearer " + token,
                passengerId
        );

        call.enqueue(new Callback<List<RideResponse>>() {
            @Override
            public void onResponse(Call<List<RideResponse>> call, Response<List<RideResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    rides.setValue(response.body());
                } else {
                    error.setValue("Failed to load rides: " + response.code());
                    Log.e(TAG, "Error loading rides: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<RideResponse>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }
}

