package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.EmailRequest;
import com.example.mobile.utils.ClientUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for ForgotPasswordFragment.
 * Manages forgot password state and handles API calls.
 * Uses MutableLiveData exposed as LiveData to the UI (MVVM pattern).
 */
public class ForgotPasswordViewModel extends AndroidViewModel {

    private static final String TAG = "ForgotPasswordViewModel";

    // Input fields state
    private final MutableLiveData<String> email = new MutableLiveData<>("");

    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<String> noticeMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> resetLinkSent = new MutableLiveData<>(false);

    // Validation errors
    private final MutableLiveData<String> emailError = new MutableLiveData<>(null);

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
    }

    // ========================== LiveData Getters (Exposed to UI) ==========================

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getNoticeMessage() {
        return noticeMessage;
    }

    public LiveData<Boolean> getResetLinkSent() {
        return resetLinkSent;
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    // ========================== Setters (From UI) ==========================

    public void setEmail(String value) {
        email.setValue(value);
        emailError.setValue(null);
    }

    public void setNotice(String notice) {
        noticeMessage.setValue(notice);
    }

    // ========================== Validation ==========================

    /**
     * Validates the email input.
     * @return true if email is valid
     */
    private boolean validateInputs() {
        boolean isValid = true;
        String emailValue = email.getValue();

        // Validate email
        if (emailValue == null || emailValue.trim().isEmpty()) {
            emailError.setValue("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailValue.trim()).matches()) {
            emailError.setValue("Invalid email address");
            isValid = false;
        } else {
            emailError.setValue(null);
        }

        return isValid;
    }

    // ========================== Forgot Password Logic ==========================

    /**
     * Requests a password reset link to be sent to the email.
     */
    public void requestPasswordReset() {
        // Clear previous states
        errorMessage.setValue(null);
        resetLinkSent.setValue(false);

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Set loading state
        isLoading.setValue(true);

        String emailValue = email.getValue().trim();
        EmailRequest request = new EmailRequest(emailValue);

        // Call API
        ClientUtils.userService.forgotPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                isLoading.postValue(false);

                if (response.isSuccessful()) {
                    Log.d(TAG, "Password reset link sent successfully");
                    resetLinkSent.postValue(true);
                } else {
                    // Handle specific error codes
                    String error = parseErrorMessage(response);
                    
                    // For 404 (user not found) and 409 (already sent), 
                    // we still show success for security reasons (same as web)
                    if (response.code() == 404 || response.code() == 409) {
                        Log.d(TAG, "Password reset - showing success for security (code: " + response.code() + ")");
                        resetLinkSent.postValue(true);
                    } else {
                        Log.e(TAG, "Forgot password failed: " + error);
                        errorMessage.postValue(error);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "Forgot password failed: " + error, t);
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
                return "Please enter a valid email address.";
            case 429:
                return "Too many requests. Please try again later.";
            case 500:
                return "Server error. Please try again later.";
            default:
                return "Failed to send reset link. Please try again.";
        }
    }

    /**
     * Clears the error message.
     */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Resets the reset link sent state.
     */
    public void resetState() {
        resetLinkSent.setValue(false);
    }

    /**
     * Gets the email value for passing to next screen.
     */
    public String getEmailValue() {
        return email.getValue() != null ? email.getValue().trim() : "";
    }
}
