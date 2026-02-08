package com.example.mobile.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for email-only endpoints like forgot-password and resend-activation.
 */
public class EmailRequest {

    @SerializedName("email")
    private String email;

    public EmailRequest() {
    }

    public EmailRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
