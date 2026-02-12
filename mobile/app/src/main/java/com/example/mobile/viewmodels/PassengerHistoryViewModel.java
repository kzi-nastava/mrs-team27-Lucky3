package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PassengerHistoryViewModel extends AndroidViewModel {
    private static final String TAG = "PassengerHistoryVM";
    private final SharedPreferencesManager prefsManager;
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final MutableLiveData<List<RideResponse>> rides = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public PassengerHistoryViewModel(@NonNull Application application) {
        super(application);
        prefsManager = new SharedPreferencesManager(application.getApplicationContext());
    }
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
        loadRides(null, null, null);
    }

    public void loadRides(String status, String fromDate, String toDate) {
        isLoading.setValue(true);
        error.setValue(null);

        String token = prefsManager.getToken();
        Long passengerId = prefsManager.getUserId();

        if (token == null || passengerId == null) {
            error.setValue("Authentication required");
            isLoading.setValue(false);
            return;
        }

        Call<PageResponse<RideResponse>> call = ClientUtils.rideService.getRidesHistory(
                null,                           // driverId - null for passenger
                passengerId,                    // passengerId
                status,                         // status filter (can be null for all)
                fromDate,                       // fromDate (can be null)
                toDate,                         // toDate (can be null)
                0,                              // page - start from first page
                DEFAULT_PAGE_SIZE,              // size - get up to 100 rides
                "startTime,desc",               // sort - newest first
                "Bearer " + token               // Authorization header
        );

        call.enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call, Response<PageResponse<RideResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<RideResponse> pageResponse = response.body();
                    rides.setValue(pageResponse.getContent());
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " rides out of " + pageResponse.getTotalElements() + " total");
                } else {
                    error.setValue("Failed to load rides: " + response.code());
                    Log.e(TAG, "Error loading rides: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }
}


