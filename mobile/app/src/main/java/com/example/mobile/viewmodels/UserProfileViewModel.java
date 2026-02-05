package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileViewModel extends AndroidViewModel {
    private static final String TAG = "ProfileViewModel";
    private final SharedPreferencesManager prefsManager;

    // LiveData for observing profile data
    private MutableLiveData<ProfileUserResponse> userProfileLiveData;
    private MutableLiveData<Boolean> loadingLiveData;
    private MutableLiveData<String> errorLiveData;

    public UserProfileViewModel(@NonNull Application application) {
        super(application);
        prefsManager = new SharedPreferencesManager(application.getApplicationContext());

        userProfileLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
    }

    // Getters for LiveData
    public MutableLiveData<ProfileUserResponse> getUserProfileLiveData() {
        return userProfileLiveData;
    }

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    /**
     * Fetches user profile from backend
     */
    public void loadUserProfile() {
        loadingLiveData.setValue(true);

        Long userId = prefsManager.getUserId();
        String token = prefsManager.getToken();  // Get token

        if (userId == null || userId == -1) {
            errorLiveData.setValue("User ID not found");
            loadingLiveData.setValue(false);
            return;
        }

        if (token == null) {  // Check token
            errorLiveData.setValue("Not authenticated");
            loadingLiveData.setValue(false);
            return;
        }

        String authHeader = "Bearer " + token;  // Format token

        ClientUtils.userService.getUserById(userId, authHeader).enqueue(new Callback<ProfileUserResponse>() {
            @Override
            public void onResponse(Call<ProfileUserResponse> call, Response<ProfileUserResponse> response) {
                loadingLiveData.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ProfileUserResponse profile = response.body();
                    userProfileLiveData.postValue(profile);
                    saveToPreferences(profile);
                    Log.d(TAG, "Profile loaded successfully for user: " + userId);
                } else {
                    String errorMsg = "Failed to load profile. Code: " + response.code();
                    errorLiveData.postValue(errorMsg);
                    Log.e(TAG, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ProfileUserResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                String errorMsg = "Network error: " + t.getMessage();
                errorLiveData.postValue(errorMsg);
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Updates user profile on backend
     */
    public void updateUserProfile(ProfileUserResponse updatedProfile) {
        /*loadingLiveData.setValue(true);

        String token = prefsManager.getToken();
        String authHeader = "Bearer " + token;

        apiService.updateUserProfile(authHeader, updatedProfile).enqueue(new Callback<ProfileUserResponse>() {
            @Override
            public void onResponse(Call<ProfileUserResponse> call, Response<ProfileUserResponse> response) {
                loadingLiveData.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ProfileUserResponse profile = response.body();
                    userProfileLiveData.postValue(profile);
                    saveToPreferences(profile);
                } else {
                    errorLiveData.postValue("Failed to update profile: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ProfileUserResponse> call, Throwable t) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue("Update failed: " + t.getMessage());
            }
        });*/
    }

    private void saveToPreferences(ProfileUserResponse profile) {
        prefsManager.saveUserData(
                prefsManager.getUserId(), // Keep existing ID
                profile.getEmail(),
                profile.getName(),
                profile.getSurname(),
                prefsManager.getUserRole(), // Keep existing role
                profile.getPhoneNumber(),
                profile.getAddress(),
                profile.getImageUrl()
        );
    }
}
