package com.example.mobile.utils;

import android.util.Patterns;

/**
 * Utility class for input validation.
 * Provides static methods for validating common input fields.
 */
public class ValidationUtils {

    /**
     * Validates an email address.
     * 
     * @param email The email to validate
     * @return null if valid, error message if invalid
     */
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Invalid email format";
        }
        return null;
    }

    /**
     * Validates a password.
     * 
     * @param password The password to validate
     * @param minLength Minimum required length
     * @return null if valid, error message if invalid
     */
    public static String validatePassword(String password, int minLength) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < minLength) {
            return "Password must be at least " + minLength + " characters";
        }
        return null;
    }

    /**
     * Validates that passwords match.
     * 
     * @param password The password
     * @param confirmPassword The confirmation password
     * @return null if they match, error message if they don't
     */
    public static String validatePasswordsMatch(String password, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return "Please confirm your password";
        }
        if (!confirmPassword.equals(password)) {
            return "Passwords do not match";
        }
        return null;
    }

    /**
     * Validates a required text field.
     * 
     * @param value The value to validate
     * @param fieldName The name of the field (for error message)
     * @return null if valid, error message if invalid
     */
    public static String validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required";
        }
        return null;
    }

    /**
     * Validates a required text field with minimum length.
     * 
     * @param value The value to validate
     * @param fieldName The name of the field (for error message)
     * @param minLength Minimum required length
     * @return null if valid, error message if invalid
     */
    public static String validateMinLength(String value, String fieldName, int minLength) {
        String requiredError = validateRequired(value, fieldName);
        if (requiredError != null) {
            return requiredError;
        }
        if (value.trim().length() < minLength) {
            return fieldName + " must be at least " + minLength + " characters";
        }
        return null;
    }

    /**
     * Validates a phone number.
     * 
     * @param phone The phone number to validate
     * @return null if valid, error message if invalid
     */
    public static String validatePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number is required";
        }
        // Remove common formatting characters
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)\\+]", "");
        // Check if it's all digits and reasonable length
        if (!cleaned.matches("\\d{6,15}")) {
            return "Invalid phone number format";
        }
        return null;
    }

    /**
     * Checks if a string is empty or null.
     * 
     * @param value The value to check
     * @return true if empty or null
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Checks if a string is a valid email.
     * 
     * @param email The email to check
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }
}
