package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.LoginRequest;
import com.example.mobile.models.TokenResponse;
import com.example.mobile.services.UserService;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for LoginFragment.
 * Manages login state and handles authentication logic.
 * Uses MutableLiveData exposed as LiveData to the UI (MVVM pattern).
 */
public class LoginViewModel extends AndroidViewModel {

    private static final String TAG = "LoginViewModel";

    // Input fields state
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<String> password = new MutableLiveData<>("");

    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> userRole = new MutableLiveData<>(null);

    // Validation errors
    private final MutableLiveData<String> emailError = new MutableLiveData<>(null);
    private final MutableLiveData<String> passwordError = new MutableLiveData<>(null);

    // SharedPreferences manager for token storage
    private final SharedPreferencesManager preferencesManager;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        preferencesManager = new SharedPreferencesManager(application.getApplicationContext());
    }

    // ========================== LiveData Getters (Exposed to UI) ==========================

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<String> getPassword() {
        return password;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getUserRole() {
        return userRole;
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    public LiveData<String> getPasswordError() {
        return passwordError;
    }

    // ========================== Setters (From UI) ==========================

    public void setEmail(String value) {
        email.setValue(value);
        emailError.setValue(null); // Clear error when user types
    }

    public void setPassword(String value) {
        password.setValue(value);
        passwordError.setValue(null); // Clear error when user types
    }

    // ========================== Validation ==========================

    /**
     * Validates the login form inputs.
     * @return true if all inputs are valid
     */
    private boolean validateInputs() {
        boolean isValid = true;
        String emailValue = email.getValue();
        String passwordValue = password.getValue();

        // Validate email
        if (emailValue == null || emailValue.trim().isEmpty()) {
            emailError.setValue("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailValue.trim()).matches()) {
            emailError.setValue("Invalid email format");
            isValid = false;
        } else {
            emailError.setValue(null);
        }

        // Validate password
        if (passwordValue == null || passwordValue.isEmpty()) {
            passwordError.setValue("Password is required");
            isValid = false;
        } else if (passwordValue.length() < 6) {
            passwordError.setValue("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordError.setValue(null);
        }

        return isValid;
    }

    // ========================== Login Logic ==========================

    /**
     * Performs the login operation.
     * Validates inputs, calls the API, and handles the response.
     */
    public void login() {
        // Clear previous states
        errorMessage.setValue(null);
        loginSuccess.setValue(false);

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Set loading state
        isLoading.setValue(true);

        // Create login request
        LoginRequest loginRequest = new LoginRequest(
                email.getValue().trim(),
                password.getValue()
        );

        // Call API
        ClientUtils.userService.login(loginRequest).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                isLoading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();
                    
                    // Save tokens to SharedPreferences
                    preferencesManager.saveToken(tokenResponse.getAccessToken());
                    preferencesManager.saveRefreshToken(tokenResponse.getRefreshToken());

                    // Decode JWT to get user role and ID
                    String role = decodeRoleFromToken(tokenResponse.getAccessToken());
                    Long userId = decodeUserIdFromToken(tokenResponse.getAccessToken());
                    userRole.postValue(role);

                    // Save basic user info
                    preferencesManager.saveUserData(
                            userId,
                            email.getValue().trim(),
                            "", "",
                            role,
                            "", "", ""
                    );

                    Log.d(TAG, "Login successful. Role: " + role + ", User ID: " + userId);
                    loginSuccess.postValue(true);
                } else {
                    // Handle error responses
                    String error = parseErrorMessage(response);
                    Log.e(TAG, "Login failed: " + error);
                    errorMessage.postValue(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "Login failed: " + error, t);
                errorMessage.postValue("Unable to connect to server. Please check your connection.");
            }
        });
    }

    /**
     * Decodes the JWT token to extract the user role using Gson.
     */
    private String decodeRoleFromToken(String token) {
        try {
            String payloadJson = decodeTokenPayload(token);
            if (payloadJson == null) return "PASSENGER";

            JwtPayload payload = ClientUtils.getGson().fromJson(payloadJson, JwtPayload.class);
            
            if (payload.role != null) return payload.role.toUpperCase();
            
            // Fallback for authorities array
            if (payloadJson.contains("ROLE_DRIVER") || payloadJson.contains("\"DRIVER\"")) {
                return "DRIVER";
            } else if (payloadJson.contains("ROLE_ADMIN") || payloadJson.contains("\"ADMIN\"")) {
                return "ADMIN";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding JWT token", e);
        }
        return "PASSENGER";
    }

    /**
     * Decodes the JWT token to extract the user ID using Gson.
     */
    private Long decodeUserIdFromToken(String token) {
        try {
            String payloadJson = decodeTokenPayload(token);
            if (payloadJson == null) return -1L;
            
            JwtPayload payload = ClientUtils.getGson().fromJson(payloadJson, JwtPayload.class);
            return payload.id != null ? payload.id : -1L;

        } catch (Exception e) {
            Log.e(TAG, "Error decoding user ID from JWT token", e);
        }
        return -1L;
    }

    private String decodeTokenPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length >= 2) {
            byte[] decodedBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE);
            return new String(decodedBytes);
        }
        return null;
    }

    // Helper class for Gson parsing
    private static class JwtPayload {
        Long id;
        String role;
        String sub; // email
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
            case 401:
                return "Invalid email or password";
            case 403:
                return "Account is blocked or not activated";
            case 404:
                return "User not found";
            case 500:
                return "Server error. Please try again later.";
            default:
                return "Login failed. Please try again.";
        }
    }

    /**
     * Clears the error message.
     */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Resets the login success state.
     */
    public void resetLoginSuccess() {
        loginSuccess.setValue(false);
    }

    /**
     * Gets the SharedPreferencesManager instance.
     */
    public SharedPreferencesManager getPreferencesManager() {
        return preferencesManager;
    }
}
