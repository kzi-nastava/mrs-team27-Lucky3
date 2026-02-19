package com.example.mobile.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for password reset endpoint.
 */
public class PasswordResetRequest {

    @SerializedName("token")
    private String token;

    @SerializedName("newPassword")
    private String newPassword;

    public PasswordResetRequest() {
    }

    public PasswordResetRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
