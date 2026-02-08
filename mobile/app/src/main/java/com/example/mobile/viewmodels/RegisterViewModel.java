package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.RegistrationRequest;
import com.example.mobile.models.UserResponse;
import com.example.mobile.services.UserService;
import com.example.mobile.utils.ClientUtils;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for RegisterFragment.
 * Manages registration state and handles registration logic.
 * Uses MutableLiveData exposed as LiveData to the UI (MVVM pattern).
 */
public class RegisterViewModel extends AndroidViewModel {

    private static final String TAG = "RegisterViewModel";

    // Input fields state
    private final MutableLiveData<String> firstName = new MutableLiveData<>("");
    private final MutableLiveData<String> lastName = new MutableLiveData<>("");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<String> phoneNumber = new MutableLiveData<>("");
    private final MutableLiveData<String> address = new MutableLiveData<>("");
    private final MutableLiveData<String> password = new MutableLiveData<>("");
    private final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");
    private final MutableLiveData<String> profilePhotoUri = new MutableLiveData<>(null);

    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>(false);

    // Validation errors
    private final MutableLiveData<String> firstNameError = new MutableLiveData<>(null);
    private final MutableLiveData<String> lastNameError = new MutableLiveData<>(null);
    private final MutableLiveData<String> emailError = new MutableLiveData<>(null);
    private final MutableLiveData<String> phoneError = new MutableLiveData<>(null);
    private final MutableLiveData<String> addressError = new MutableLiveData<>(null);
    private final MutableLiveData<String> passwordError = new MutableLiveData<>(null);
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>(null);

    public RegisterViewModel(@NonNull Application application) {
        super(application);
    }

    // ========================== LiveData Getters (Exposed to UI) ==========================

    public LiveData<String> getFirstName() {
        return firstName;
    }

    public LiveData<String> getLastName() {
        return lastName;
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<String> getPhoneNumber() {
        return phoneNumber;
    }

    public LiveData<String> getAddress() {
        return address;
    }

    public LiveData<String> getPassword() {
        return password;
    }

    public LiveData<String> getConfirmPassword() {
        return confirmPassword;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getRegistrationSuccess() {
        return registrationSuccess;
    }

    // Validation error getters
    public LiveData<String> getFirstNameError() {
        return firstNameError;
    }

    public LiveData<String> getLastNameError() {
        return lastNameError;
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    public LiveData<String> getPhoneError() {
        return phoneError;
    }

    public LiveData<String> getAddressError() {
        return addressError;
    }

    public LiveData<String> getPasswordError() {
        return passwordError;
    }

    public LiveData<String> getConfirmPasswordError() {
        return confirmPasswordError;
    }

    // ========================== Setters (From UI) ==========================

    public void setFirstName(String value) {
        firstName.setValue(value);
        firstNameError.setValue(null);
    }

    public void setLastName(String value) {
        lastName.setValue(value);
        lastNameError.setValue(null);
    }

    public void setEmail(String value) {
        email.setValue(value);
        emailError.setValue(null);
    }

    public void setPhoneNumber(String value) {
        phoneNumber.setValue(value);
        phoneError.setValue(null);
    }

    public void setAddress(String value) {
        address.setValue(value);
        addressError.setValue(null);
    }

    public void setPassword(String value) {
        password.setValue(value);
        passwordError.setValue(null);
    }

    public void setConfirmPassword(String value) {
        confirmPassword.setValue(value);
        confirmPasswordError.setValue(null);
    }

    public void setProfilePhotoUri(String value) {
        profilePhotoUri.setValue(value);
    }

    public LiveData<String> getProfilePhotoUri() {
        return profilePhotoUri;
    }

    // ========================== Validation ==========================

    /**
     * Validates all registration form inputs.
     * @return true if all inputs are valid
     */
    private boolean validateInputs() {
        boolean isValid = true;

        // Validate first name
        String firstNameValue = firstName.getValue();
        if (firstNameValue == null || firstNameValue.trim().isEmpty()) {
            firstNameError.setValue("First name is required");
            isValid = false;
        } else if (firstNameValue.trim().length() < 2) {
            firstNameError.setValue("First name must be at least 2 characters");
            isValid = false;
        } else {
            firstNameError.setValue(null);
        }

        // Validate last name
        String lastNameValue = lastName.getValue();
        if (lastNameValue == null || lastNameValue.trim().isEmpty()) {
            lastNameError.setValue("Last name is required");
            isValid = false;
        } else if (lastNameValue.trim().length() < 2) {
            lastNameError.setValue("Last name must be at least 2 characters");
            isValid = false;
        } else {
            lastNameError.setValue(null);
        }

        // Validate email
        String emailValue = email.getValue();
        if (emailValue == null || emailValue.trim().isEmpty()) {
            emailError.setValue("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailValue.trim()).matches()) {
            emailError.setValue("Invalid email format");
            isValid = false;
        } else {
            emailError.setValue(null);
        }

        // Validate phone number
        String phoneValue = phoneNumber.getValue();
        if (phoneValue == null || phoneValue.trim().isEmpty()) {
            phoneError.setValue("Phone number is required");
            isValid = false;
        } else if (!isValidPhoneNumber(phoneValue.trim())) {
            phoneError.setValue("Invalid phone number format");
            isValid = false;
        } else {
            phoneError.setValue(null);
        }

        // Validate address (city)
        String addressValue = address.getValue();
        if (addressValue == null || addressValue.trim().isEmpty()) {
            addressError.setValue("Address is required");
            isValid = false;
        } else if (addressValue.trim().length() < 5) {
            addressError.setValue("Address must be at least 5 characters");
            isValid = false;
        } else {
            addressError.setValue(null);
        }

        // Validate password
        String passwordValue = password.getValue();
        if (passwordValue == null || passwordValue.isEmpty()) {
            passwordError.setValue("Password is required");
            isValid = false;
        } else if (passwordValue.length() < 8) {
            passwordError.setValue("Password must be at least 8 characters");
            isValid = false;
        } else {
            passwordError.setValue(null);
        }

        // Validate confirm password
        String confirmPasswordValue = confirmPassword.getValue();
        if (confirmPasswordValue == null || confirmPasswordValue.isEmpty()) {
            confirmPasswordError.setValue("Please confirm your password");
            isValid = false;
        } else if (!confirmPasswordValue.equals(passwordValue)) {
            confirmPasswordError.setValue("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordError.setValue(null);
        }

        return isValid;
    }

    /**
     * Phone number validation matching web pattern: /^\+?[\d\s-()]{10,}$/
     * Allows optional + prefix, then at least 10 digits/spaces/hyphens/parentheses
     */
    private boolean isValidPhoneNumber(String phone) {
        // Match web regex: ^\+?[\d\s\-()]{10,}$
        return phone.matches("^\\+?[\\d\\s\\-()]{10,}$");
    }

    // ========================== Registration Logic ==========================

    /**
     * Performs the registration operation.
     * Validates inputs, calls the API, and handles the response.
     */
    public void register() {
        // Clear previous states
        errorMessage.setValue(null);
        registrationSuccess.setValue(false);

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Set loading state
        isLoading.setValue(true);

        // Create registration request
        RegistrationRequest registrationRequest = new RegistrationRequest(
                firstName.getValue().trim(),
                lastName.getValue().trim(),
                email.getValue().trim(),
                password.getValue(),
                confirmPassword.getValue(),
                phoneNumber.getValue().trim(),
                address.getValue().trim()
        );

        // Convert to JSON for multipart request
        Gson gson = ClientUtils.getGson();
        String jsonData = gson.toJson(registrationRequest);
        RequestBody dataBody = RequestBody.create(jsonData, MediaType.parse("application/json"));

        // Call API (without profile image for now)
        ClientUtils.userService.registerWithoutImage(dataBody).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                isLoading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse userResponse = response.body();
                    Log.d(TAG, "Registration successful for user: " + userResponse.getEmail());
                    registrationSuccess.postValue(true);
                } else {
                    // Handle error responses
                    String error = parseErrorMessage(response);
                    Log.e(TAG, "Registration failed: " + error);
                    errorMessage.postValue(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "Registration failed: " + error, t);
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
                // Check for email already used error
                if (errorBody.toLowerCase().contains("email") && errorBody.toLowerCase().contains("already")) {
                    return "This email is already registered";
                }
                return errorBody;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
        }

        // Return generic message based on status code
        switch (response.code()) {
            case 400:
                return "Invalid registration data. Please check your inputs.";
            case 409:
                return "This email is already registered";
            case 500:
                return "Server error. Please try again later.";
            default:
                return "Registration failed. Please try again.";
        }
    }

    /**
     * Clears the error message.
     */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Resets the registration success state.
     */
    public void resetRegistrationSuccess() {
        registrationSuccess.setValue(false);
    }

    /**
     * Clears all form fields.
     */
    public void clearForm() {
        firstName.setValue("");
        lastName.setValue("");
        email.setValue("");
        phoneNumber.setValue("");
        address.setValue("");
        password.setValue("");
        confirmPassword.setValue("");
        
        // Clear all errors
        firstNameError.setValue(null);
        lastNameError.setValue(null);
        emailError.setValue(null);
        phoneError.setValue(null);
        addressError.setValue(null);
        passwordError.setValue(null);
        confirmPasswordError.setValue(null);
    }
}
