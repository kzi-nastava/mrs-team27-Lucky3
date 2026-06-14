package com.example.mobile.models;

/**
 * Response DTO after unblocking a user.
 */
public class BlockUserResponse {
    private String email;
    private String message;

    public BlockUserResponse() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
