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
     * Decodes the JWT token to extract the user role.
     * Note: This is a simplified implementation. In production, use a proper JWT library.
     */
    private String decodeRoleFromToken(String token) {
        try {
            // JWT format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = parts[1];
                // Decode Base64
                byte[] decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
                String decodedPayload = new String(decodedBytes);
                
                // Simple JSON parsing for role
                // Expected format: {"role":"DRIVER",...} or {"authorities":[{"authority":"ROLE_DRIVER"}]}
                if (decodedPayload.contains("\"role\"")) {
                    int roleStart = decodedPayload.indexOf("\"role\"");
                    int valueStart = decodedPayload.indexOf(":", roleStart) + 1;
                    int valueEnd = decodedPayload.indexOf(",", valueStart);
                    if (valueEnd == -1) valueEnd = decodedPayload.indexOf("}", valueStart);
                    String role = decodedPayload.substring(valueStart, valueEnd).trim()
                            .replace("\"", "").replace("}", "");
                    return role.toUpperCase();
                }
                
                // Check for authorities array
                if (decodedPayload.contains("ROLE_DRIVER") || decodedPayload.contains("\"DRIVER\"")) {
                    return "DRIVER";
                } else if (decodedPayload.contains("ROLE_ADMIN") || decodedPayload.contains("\"ADMIN\"")) {
                    return "ADMIN";
                } else if (decodedPayload.contains("ROLE_PASSENGER") || decodedPayload.contains("\"PASSENGER\"")) {
                    return "PASSENGER";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding JWT token", e);
        }
        
        // Default to PASSENGER if unable to determine
        return "PASSENGER";
    }

    /**
     * Decodes the JWT token to extract the user ID.
     * @param token The JWT token string
     * @return The user ID from the token, or -1L if not found
     */
    private Long decodeUserIdFromToken(String token) {
        try {
            // JWT format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = parts[1];
                // Decode Base64
                byte[] decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
                String decodedPayload = new String(decodedBytes);
                
                Log.d(TAG, "JWT Payload: " + decodedPayload);
                
                // Simple JSON parsing for id
                // Expected format: {"id":1,...}
                if (decodedPayload.contains("\"id\"")) {
                    int idStart = decodedPayload.indexOf("\"id\"");
                    int valueStart = decodedPayload.indexOf(":", idStart) + 1;
                    int valueEnd = decodedPayload.indexOf(",", valueStart);
                    if (valueEnd == -1) valueEnd = decodedPayload.indexOf("}", valueStart);
                    String idStr = decodedPayload.substring(valueStart, valueEnd).trim();
                    Long userId = Long.parseLong(idStr);
                    Log.d(TAG, "Extracted user ID from JWT: " + userId);
                    return userId;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding user ID from JWT token", e);
        }
        
        Log.w(TAG, "Could not extract user ID from JWT token, returning -1");
        return -1L;
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
