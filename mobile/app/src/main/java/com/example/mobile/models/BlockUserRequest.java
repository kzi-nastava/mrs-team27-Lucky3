package com.example.mobile.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request DTO for blocking a user.
 * Contains the email of the user to block and the reason for blocking.
 */
public class BlockUserRequest {
    private String email;
    private String reason;

    public BlockUserRequest() {
    }

    public BlockUserRequest(String email, String reason) {
        this.email = email;
        this.reason = reason;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
