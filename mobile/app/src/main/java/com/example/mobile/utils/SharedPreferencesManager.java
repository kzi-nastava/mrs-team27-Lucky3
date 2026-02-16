package com.example.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for managing JWT tokens and user data in SharedPreferences.
 * Provides methods to store, retrieve, and clear authentication data.
 */
public class SharedPreferencesManager {

    private static final String PREF_NAME = "RideSharePrefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_SURNAME = "user_surname";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ADDRESS = "user_address";
    private static final String KEY_USER_PROFILE_IMAGE = "user_profile_image";
    private static final String KEY_DRIVER_STATUS = "driver_status";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_FCM_TOKEN_SYNCED = "fcm_token_synced";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ========================== JWT Token Methods ==========================

    /**
     * Saves the JWT access token.
     * @param token The JWT token string
     */
    public void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_JWT_TOKEN, token);
        editor.apply();
    }

    /**
     * Retrieves the saved JWT access token.
     * @return The JWT token or null if not found
     */
    public String getToken() {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null);
    }

    /**
     * Saves the refresh token.
     * @param refreshToken The refresh token string
     */
    public void saveRefreshToken(String refreshToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    /**
     * Retrieves the saved refresh token.
     * @return The refresh token or null if not found
     */
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * Clears only the JWT tokens.
     */
    public void clearTokens() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_JWT_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.apply();
    }

    // ========================== User Data Methods ==========================

    /**
     * Saves user data after successful login.
     */
    public void saveUserData(Long userId, String email, String name, String surname,
                             String role, String phoneNumber, String address, String profileImageUrl) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_SURNAME, surname);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_PHONE, phoneNumber);
        editor.putString(KEY_USER_ADDRESS, address);
        editor.putString(KEY_USER_PROFILE_IMAGE, profileImageUrl);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Gets the user ID.
     */
    public Long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1);
    }

    /**
     * Gets the user email.
     */
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Gets the user's full name.
     */
    public String getUserFullName() {
        String name = sharedPreferences.getString(KEY_USER_NAME, "");
        String surname = sharedPreferences.getString(KEY_USER_SURNAME, "");
        return name + " " + surname;
    }

    /**
     * Gets the user role (PASSENGER, DRIVER, ADMIN).
     */
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    /**
     * Gets the user's phone number.
     */
    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, null);
    }

    /**
     * Gets the user's address.
     */
    public String getUserAddress() {
        return sharedPreferences.getString(KEY_USER_ADDRESS, null);
    }

    /**
     * Gets the user's profile image URL.
     */
    public String getUserProfileImage() {
        return sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null);
    }

    // ========================== Driver Status Methods ==========================

    /**
     * Saves the driver's active status.
     * @param isActive Whether the driver is currently active
     */
    public void saveDriverStatus(boolean isActive) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DRIVER_STATUS, isActive);
        editor.apply();
    }

    /**
     * Gets the driver's active status.
     * @return true if driver is active, false otherwise
     */
    public boolean getDriverStatus() {
        return sharedPreferences.getBoolean(KEY_DRIVER_STATUS, false);
    }

    // ========================== FCM Token Methods ==========================

    /**
     * Saves the FCM device token for push notifications.
     * @param token The FCM registration token
     */
    public void saveFcmToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }

    /**
     * Gets the stored FCM device token.
     * @return The FCM token or null if not stored
     */
    public String getFcmToken() {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Marks the FCM token as synced with the backend.
     * @param synced true if the token has been successfully sent to the backend
     */
    public void setFcmTokenSynced(boolean synced) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_FCM_TOKEN_SYNCED, synced);
        editor.apply();
    }

    /**
     * Checks if the current FCM token has been synced with the backend.
     * @return true if the token was successfully registered with the server
     */
    public boolean isFcmTokenSynced() {
        return sharedPreferences.getBoolean(KEY_FCM_TOKEN_SYNCED, false);
    }

    // ========================== Session Methods ==========================

    /**
     * Checks if user is currently logged in.
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getToken() != null;
    }

    /**
     * Clears all stored data (logout).
     */
    public void clearAll() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Performs logout by clearing all user data.
     */
    public void logout() {
        clearAll();
    }
}
