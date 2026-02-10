package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.FavoriteRouteResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerFavoritesViewModel extends AndroidViewModel {

    private static final String TAG = "PassengerFavoritesVM";
    private final SharedPreferencesManager prefsManager;

    private final MutableLiveData<List<FavoriteRouteResponse>> favoriteRoutes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public PassengerFavoritesViewModel(@NonNull Application application) {
        super(application);
        prefsManager = new SharedPreferencesManager(application.getApplicationContext());
    }

    public LiveData<List<FavoriteRouteResponse>> getFavoriteRoutes() {
        return favoriteRoutes;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void loadFavoriteRoutes() {
        isLoading.setValue(true);
        error.setValue(null);

        String token = prefsManager.getToken();
        Long passengerId = prefsManager.getUserId();

        if (token == null || passengerId == null) {
            error.setValue("Authentication required");
            isLoading.setValue(false);
            return;
        }

        Call<List<FavoriteRouteResponse>> call = ClientUtils.rideService.getFavoriteRoutes(
                passengerId,
                "Bearer " + token
        );

        call.enqueue(new Callback<List<FavoriteRouteResponse>>() {
            @Override
            public void onResponse(Call<List<FavoriteRouteResponse>> call,
                                   Response<List<FavoriteRouteResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    favoriteRoutes.setValue(response.body());
                    Log.d(TAG, "Loaded " + response.body().size() + " favorite routes");
                } else {
                    error.setValue("Failed to load favorite routes: " + response.code());
                    Log.e(TAG, "Error loading favorite routes: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<FavoriteRouteResponse>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }

    public void removeFavoriteRoute(Long favouriteRouteId) {
        isLoading.setValue(true);
        error.setValue(null);

        String token = prefsManager.getToken();
        Long passengerId = prefsManager.getUserId();

        if (token == null || passengerId == null) {
            error.setValue("Authentication required");
            isLoading.setValue(false);
            return;
        }

        Call<Void> call = ClientUtils.rideService.removeFavouriteRoute(
                passengerId,
                favouriteRouteId,
                "Bearer " + token
        );

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    successMessage.setValue("Favorite route removed");
                    Log.d(TAG, "Favorite route removed successfully");
                    // Reload the list
                    loadFavoriteRoutes();
                } else {
                    error.setValue("Failed to remove favorite: " + response.code());
                    Log.e(TAG, "Error removing favorite: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }
}
