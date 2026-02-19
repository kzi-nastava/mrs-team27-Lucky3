package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.PasswordResetRequest;
import com.example.mobile.utils.ClientUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for ResetPasswordFragment.
 * Manages reset password state and handles API calls.
 * Uses MutableLiveData exposed as LiveData to the UI (MVVM pattern).
 */
public class ResetPasswordViewModel extends AndroidViewModel {

    private static final String TAG = "ResetPasswordViewModel";

    // Token state
    private final MutableLiveData<String> token = new MutableLiveData<>("");

    // Input fields state
    private final MutableLiveData<String> newPassword = new MutableLiveData<>("");
    private final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isValidatingToken = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> passwordResetSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> tokenValidationError = new MutableLiveData<>(null);

    // Validation errors
    private final MutableLiveData<String> newPasswordError = new MutableLiveData<>(null);
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>(null);

    public ResetPasswordViewModel(@NonNull Application application) {
        super(application);
    }

    // ========================== LiveData Getters (Exposed to UI) ==========================

    public LiveData<String> getToken() {
        return token;
    }

    public LiveData<String> getNewPassword() {
        return newPassword;
    }

    public LiveData<String> getConfirmPassword() {
        return confirmPassword;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsValidatingToken() {
        return isValidatingToken;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getPasswordResetSuccess() {
        return passwordResetSuccess;
    }

    public LiveData<String> getTokenValidationError() {
        return tokenValidationError;
    }

    public LiveData<String> getNewPasswordError() {
        return newPasswordError;
    }

    public LiveData<String> getConfirmPasswordError() {
        return confirmPasswordError;
    }

    // ========================== Setters (From UI) ==========================

    public void setToken(String value) {
        token.setValue(value);
    }

    public void setNewPassword(String value) {
        newPassword.setValue(value);
        newPasswordError.setValue(null);
    }

    public void setConfirmPassword(String value) {
        confirmPassword.setValue(value);
        confirmPasswordError.setValue(null);
    }

    // ========================== Validation ==========================

    /**
     * Validates the password inputs.
     * @return true if all inputs are valid
     */
    private boolean validateInputs() {
        boolean isValid = true;
        String newPasswordValue = newPassword.getValue();
        String confirmPasswordValue = confirmPassword.getValue();

        // Validate new password
        if (newPasswordValue == null || newPasswordValue.isEmpty()) {
            newPasswordError.setValue("Password is required");
            isValid = false;
        } else if (newPasswordValue.length() < 8) {
            newPasswordError.setValue("Password must be at least 8 characters");
            isValid = false;
        } else {
            newPasswordError.setValue(null);
        }

        // Validate confirm password
        if (confirmPasswordValue == null || confirmPasswordValue.isEmpty()) {
            confirmPasswordError.setValue("Please confirm your password");
            isValid = false;
        } else if (!confirmPasswordValue.equals(newPasswordValue)) {
            confirmPasswordError.setValue("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordError.setValue(null);
        }

        return isValid;
    }

    // ========================== Token Validation ==========================

    /**
     * Validates the reset token with the server.
     */
    public void validateToken() {
        String tokenValue = token.getValue();
        
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            isValidatingToken.setValue(false);
            tokenValidationError.setValue("missing-token");
            return;
        }

        isValidatingToken.setValue(true);

        ClientUtils.userService.validateResetToken(tokenValue.trim()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                isValidatingToken.postValue(false);

                if (response.isSuccessful()) {
                    Log.d(TAG, "Token is valid");
                    tokenValidationError.postValue(null);
                } else {
                    // Token is invalid or expired
                    String error = response.code() == 410 ? "expired-token" : "invalid-token";
                    Log.e(TAG, "Token validation failed: " + error);
                    tokenValidationError.postValue(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                isValidatingToken.postValue(false);
                Log.e(TAG, "Token validation failed: " + t.getMessage(), t);
                errorMessage.postValue("Unable to validate token. Please check your connection.");
            }
        });
    }

    // ========================== Reset Password Logic ==========================

    /**
     * Resets the password using the token.
     */
    public void resetPassword() {
        // Clear previous states
        errorMessage.setValue(null);
        passwordResetSuccess.setValue(false);

        // Check token
        String tokenValue = token.getValue();
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            errorMessage.setValue("Reset token is missing. Please open the link from your email again.");
            return;
        }

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Set loading state
        isLoading.setValue(true);

        PasswordResetRequest request = new PasswordResetRequest(
                tokenValue.trim(),
                newPassword.getValue()
        );

        // Call API
        ClientUtils.userService.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                isLoading.postValue(false);

                if (response.isSuccessful()) {
                    Log.d(TAG, "Password reset successful");
                    passwordResetSuccess.postValue(true);
                } else {
                    String error = parseErrorMessage(response);
                    Log.e(TAG, "Password reset failed: " + error);
                    
                    // Check if token-related error
                    if (response.code() == 410) {
                        tokenValidationError.postValue("expired-token");
                    } else if (response.code() == 400 || response.code() == 404) {
                        tokenValidationError.postValue("invalid-token");
                    } else {
                        errorMessage.postValue(error);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "Password reset failed: " + error, t);
                errorMessage.postValue("Unable to connect to server. Please check your connection.");
            }
        });
    }

    /**
     * Parses error message from failed API response.
     */
    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                String lowerBody = errorBody.toLowerCase();
                
                // Check for specific error types
                if (lowerBody.contains("expired")) {
                    return "This reset link has expired. Please request a new one.";
                }
                if (lowerBody.contains("invalid") || lowerBody.contains("token")) {
                    return "This reset link is invalid. Please request a new one.";
                }
                
                // Try to extract message from JSON
                if (errorBody.contains("\"message\"")) {
                    int start = errorBody.indexOf("\"message\"") + 11;
                    int end = errorBody.indexOf("\"", start);
                    if (end > start) {
                        return errorBody.substring(start, end);
                    }
                }
                return errorBody;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
        }

        // Return generic message based on status code
        switch (response.code()) {
            case 400:
            case 404:
                return "This reset link is invalid. Please request a new one.";
            case 410:
                return "This reset link has expired. Please request a new one.";
            case 500:
                return "Server error. Please try again later.";
            default:
                return "Failed to reset password. Please try again.";
        }
    }

    /**
     * Clears the error message.
     */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Resets the success state.
     */
    public void resetState() {
        passwordResetSuccess.setValue(false);
    }
}
