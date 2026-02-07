package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.CreateDriverRequest;
import com.example.mobile.models.DriverResponse;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDriversViewModel extends AndroidViewModel {
    private static final String TAG = "AdminDriversViewModel";
    private final SharedPreferencesManager prefsManager;

    // Source of truth
    private List<DriverResponse> allDrivers = new ArrayList<>();
    private Map<Long, DriverStatsResponse> driverStatsMap = new HashMap<>();

    // What the UI observes
    private MutableLiveData<List<DriverResponse>> displayedDrivers = new MutableLiveData<>();
    private MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // Current filter state
    private String currentFilter = "All";
    private String searchQuery = "";

    public AdminDriversViewModel(@NonNull Application application) {
        super(application);
        prefsManager = new SharedPreferencesManager(application.getApplicationContext());
    }

    // Getters for LiveData
    public LiveData<List<DriverResponse>> getDisplayedDrivers() {
        return displayedDrivers;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public SharedPreferencesManager getPrefsManager() {
        return prefsManager;
    }

    // 1. API Call - Fetch all drivers
    public void loadAllDrivers() {
        loadingLiveData.setValue(true);

        String token = "Bearer " + prefsManager.getToken();
        ClientUtils.driverService.getAllDrivers(token).enqueue(new Callback<List<DriverResponse>>() {
            @Override
            public void onResponse(Call<List<DriverResponse>> call, Response<List<DriverResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allDrivers = response.body();

                    Log.d(TAG, "Loaded " + allDrivers.size() + " drivers");
                    loadAllDriverStats();
                } else {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to load drivers: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<DriverResponse>> call, Throwable t) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "loadAllDrivers failed", t);
            }
        });
    }

    // NEW: Load statistics for all drivers
    private void loadAllDriverStats() {
        if (allDrivers.isEmpty()) {
            loadingLiveData.setValue(false);
            applyFiltersAndSearch();
            return;
        }

        final AtomicInteger completedRequests = new AtomicInteger(0);
        final int totalDrivers = allDrivers.size();

        for (DriverResponse driver : allDrivers) {
            Long driverId = driver.getId(); // Assuming DriverResponse has getId()

            if (driverId == null) {
                completedRequests.incrementAndGet();
                continue;
            }

            Log.d(TAG, "Loading driver stats for driver " + driverId);
            String token = "Bearer " + prefsManager.getToken();
            ClientUtils.driverService.getStats(driverId, token).enqueue(new Callback<DriverStatsResponse>() {
                @Override
                public void onResponse(Call<DriverStatsResponse> call, Response<DriverStatsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DriverStatsResponse stats = response.body();
                        driverStatsMap.put(driverId, stats);
                        Log.d(TAG, "Driver stats loaded: driverId=" + driverId +
                                " rating=" + stats.getAverageRating() +
                                " rides=" + stats.getCompletedRides());
                    } else {
                        Log.e(TAG, "Failed to load driver stats for " + driverId + ": HTTP " + response.code());
                    }

                    // Check if all requests completed
                    if (completedRequests.incrementAndGet() == totalDrivers) {
                        loadingLiveData.postValue(false);
                        applyFiltersAndSearch(); // Now apply filters with stats loaded
                    }
                }

                @Override
                public void onFailure(Call<DriverStatsResponse> call, Throwable t) {
                    Log.e(TAG, "Failed to load driver stats for " + driverId, t);

                    // Still count as completed even on failure
                    if (completedRequests.incrementAndGet() == totalDrivers) {
                        loadingLiveData.postValue(false);
                        applyFiltersAndSearch();
                    }
                }
            });
        }
    }

    // ✅ NEW: Get stats for a specific driver
    public DriverStatsResponse getDriverStats(Long driverId) {
        return driverStatsMap.get(driverId);
    }

    // 2. Filter by status
    public void filterByStatus(String status) {
        this.currentFilter = status;
        applyFiltersAndSearch();
    }

    // 3. Search by name or email
    public void search(String query) {
        this.searchQuery = query.toLowerCase().trim();
        applyFiltersAndSearch();
    }

    // 4. Combined filter + search logic
    private void applyFiltersAndSearch() {
        List<DriverResponse> result = new ArrayList<>();

        for (DriverResponse driver : allDrivers) {
            // Step 1: Filter by status
            if (!"All".equals(currentFilter)) {
                boolean matchesStatus = false;

                if ("Active".equals(currentFilter) && driver.isActive()) {
                    matchesStatus = true;
                } else if ("Inactive".equals(currentFilter) && !driver.isActive()) {
                    matchesStatus = true;
                } else if ("Suspended".equals(currentFilter) && driver.isBlocked()) {
                    matchesStatus = true;
                }

                if (!matchesStatus) continue;
            }

            // Step 2: Search by name or email (if query exists)
            if (!searchQuery.isEmpty()) {
                // Assuming UserResponse has getName() and getEmail() methods
                String name = driver.getName() != null ? driver.getName().toLowerCase() : "";
                String email = driver.getEmail() != null ? driver.getEmail().toLowerCase() : "";

                if (!name.contains(searchQuery) && !email.contains(searchQuery)) {
                    continue;
                }
            }

            result.add(driver);
        }

        displayedDrivers.setValue(result);
    }

    // In AdminDriversViewModel.java
    public void createDriver(CreateDriverRequest request, MultipartBody.Part imageFile) {
        loadingLiveData.setValue(true);

        String token = "Bearer " + prefsManager.getToken();

        // Convert request object to JSON RequestBody
        Gson gson = new Gson();
        String json = gson.toJson(request);
        RequestBody requestBody = RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                json
        );

        ClientUtils.driverService.createDriver(requestBody, imageFile, token)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(Call<DriverResponse> call, Response<DriverResponse> response) {
                        loadingLiveData.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            //addDriverToList(response.body());
                            toastMessage.setValue("Driver created successfully");
                        } else {
                            errorLiveData.setValue("Failed to create driver: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<DriverResponse> call, Throwable t) {
                        loadingLiveData.setValue(false);
                        errorLiveData.setValue("Network error: " + t.getMessage());
                    }
                });
    }
}
