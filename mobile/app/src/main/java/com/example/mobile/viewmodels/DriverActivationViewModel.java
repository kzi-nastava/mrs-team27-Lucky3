package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.SetInitialPassword;
import com.example.mobile.utils.ClientUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for DriverActivationFragment.
 * Manages driver account activation (initial password set) state and handles API calls.
 */
public class DriverActivationViewModel extends AndroidViewModel {

    private static final String TAG = "DriverActivationViewModel";

    // Token state
    private final MutableLiveData<String> token = new MutableLiveData<>("");

    // Input fields state
    private final MutableLiveData<String> password = new MutableLiveData<>("");
    private final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> activationSuccess = new MutableLiveData<>(false);

    // Validation errors
    private final MutableLiveData<String> passwordError = new MutableLiveData<>(null);
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>(null);

    public DriverActivationViewModel(@NonNull Application application) {
        super(application);
    }

    // ========================== LiveData Getters ==========================

    public LiveData<String> getToken() { return token; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getActivationSuccess() { return activationSuccess; }
    public LiveData<String> getPasswordError() { return passwordError; }
    public LiveData<String> getConfirmPasswordError() { return confirmPasswordError; }

    // ========================== Setters ==========================

    public void setToken(String value) { token.setValue(value); }

    public void setPassword(String value) {
        password.setValue(value);
        passwordError.setValue(null);
    }

    public void setConfirmPassword(String value) {
        confirmPassword.setValue(value);
        confirmPasswordError.setValue(null);
    }

    // ========================== Logic ==========================

    private boolean validateInputs() {
        boolean isValid = true;
        String pass = password.getValue();
        String conf = confirmPassword.getValue();

        if (pass == null || pass.isEmpty()) {
            passwordError.setValue("Password is required");
            isValid = false;
        } else if (pass.length() < 8) {
            passwordError.setValue("Password must be at least 8 characters");
            isValid = false;
        }

        if (conf == null || conf.isEmpty()) {
            confirmPasswordError.setValue("Please confirm your password");
            isValid = false;
        } else if (!conf.equals(pass)) {
            confirmPasswordError.setValue("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    public void activateAccount() {
        errorMessage.setValue(null);
        activationSuccess.setValue(false);

        String tokenValue = token.getValue();
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            errorMessage.setValue("Activation token is missing. Please open the link from your email again.");
            return;
        }

        if (!validateInputs()) {
            return;
        }

        isLoading.setValue(true);

        SetInitialPassword request = new SetInitialPassword(
                tokenValue.trim(),
                password.getValue(),
                confirmPassword.getValue()
        );

        ClientUtils.driverService.activateDriver(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    Log.d(TAG, "Account activation successful");
                    activationSuccess.postValue(true);
                } else {
                    errorMessage.postValue("Activation failed. The link may be expired or invalid.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                errorMessage.postValue("Connection error. Please try again.");
            }
        });
    }

    public void clearError() { errorMessage.setValue(null); }
}
