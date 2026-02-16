package com.example.mobile.models;

/**
 * Request model for registering/updating the FCM device token with the backend.
 * Sent via PUT /api/users/{id}/fcm-token
 */
public class FcmTokenRequest {
    private String token;

    public FcmTokenRequest() {}

    public FcmTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
