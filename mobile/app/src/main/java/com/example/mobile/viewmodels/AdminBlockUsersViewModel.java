package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.BlockUserRequest;
import com.example.mobile.models.BlockUserResponse;
import com.example.mobile.models.UserProfile;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminBlockUsersViewModel extends AndroidViewModel {
    private static final String TAG = "AdminBlockUsersVM";
    private final SharedPreferencesManager prefsManager;

    private List<UserProfile> allUsers = new ArrayList<>();
    private final MutableLiveData<List<UserProfile>> displayedUsers = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    private String currentFilter = "All"; // All, Blocked, Unblocked
    private String searchQuery = "";

    public AdminBlockUsersViewModel(@NonNull Application application) {
        super(application);
        prefsManager = new SharedPreferencesManager(application.getApplicationContext());
    }

    public LiveData<List<UserProfile>> getDisplayedUsers() {
        return displayedUsers;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void loadData() {
        isLoading.setValue(true);
        String token = "Bearer " + prefsManager.getToken();

        // We need to fetch both blocked and unblocked to have a full list
        fetchBlockedUsers(token, () -> {
            fetchUnblockedUsers(token, () -> {
                isLoading.setValue(false);
                applyFiltersAndSearch();
            });
        });
    }

    private void fetchBlockedUsers(String token, Runnable onComplete) {
        ClientUtils.adminUserService.getBlockedUsers(token).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(Call<List<UserProfile>> call, Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UserProfile> blocked = response.body();
                    blocked.forEach(u -> u.setBlocked(true));
                    updateLocalUsers(blocked);
                }
                onComplete.run();
            }

            @Override
            public void onFailure(Call<List<UserProfile>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch blocked users", t);
                onComplete.run();
            }
        });
    }

    private void fetchUnblockedUsers(String token, Runnable onComplete) {
        ClientUtils.adminUserService.getUnblockedUsers(token).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(Call<List<UserProfile>> call, Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UserProfile> unblocked = response.body();
                    unblocked.forEach(u -> u.setBlocked(false));
                    updateLocalUsers(unblocked);
                }
                onComplete.run();
            }

            @Override
            public void onFailure(Call<List<UserProfile>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch unblocked users", t);
                onComplete.run();
            }
        });
    }

    private synchronized void updateLocalUsers(List<UserProfile> users) {
        // Simple merge/update logic
        for (UserProfile newUser : users) {
            boolean found = false;
            for (int i = 0; i < allUsers.size(); i++) {
                if (allUsers.get(i).getEmail().equals(newUser.getEmail())) {
                    allUsers.set(i, newUser);
                    found = true;
                    break;
                }
            }
            if (!found) {
                allUsers.add(newUser);
            }
        }
    }

    public void setFilter(String filter) {
        this.currentFilter = filter;
        applyFiltersAndSearch();
    }

    public void search(String query) {
        this.searchQuery = query.toLowerCase().trim();
        applyFiltersAndSearch();
    }

    private void applyFiltersAndSearch() {
        List<UserProfile> filtered = new ArrayList<>(allUsers);

        // Filter by status
        if ("Blocked".equals(currentFilter)) {
            filtered = filtered.stream().filter(UserProfile::isBlocked).collect(Collectors.toList());
        } else if ("Unblocked".equals(currentFilter)) {
            filtered = filtered.stream().filter(u -> !u.isBlocked()).collect(Collectors.toList());
        }

        // Search by email
        if (!searchQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        displayedUsers.setValue(filtered);
    }

    public void blockUser(String email, String reason) {
        isLoading.setValue(true);
        String token = "Bearer " + prefsManager.getToken();
        BlockUserRequest request = new BlockUserRequest(email, reason);

        ClientUtils.adminUserService.blockUser(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    successMessage.setValue("User blocked successfully");
                    updateUserStatus(email, true);
                } else {
                    errorMessage.setValue("Failed to block user: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void unblockUser(String email) {
        isLoading.setValue(true);
        String token = "Bearer " + prefsManager.getToken();

        ClientUtils.adminUserService.unblockUser(token, email).enqueue(new Callback<BlockUserResponse>() {
            @Override
            public void onResponse(Call<BlockUserResponse> call, Response<BlockUserResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    successMessage.setValue("User unblocked successfully");
                    updateUserStatus(email, false);
                } else {
                    errorMessage.setValue("Failed to unblock user: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<BlockUserResponse> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    private void updateUserStatus(String email, boolean isBlocked) {
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getEmail().equals(email)) {
                allUsers.get(i).setBlocked(isBlocked);
                break;
            }
        }
        applyFiltersAndSearch();
    }
}
