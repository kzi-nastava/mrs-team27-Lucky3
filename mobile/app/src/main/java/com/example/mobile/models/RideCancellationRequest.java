package com.example.mobile.models;

public class RideCancellationRequest {

    private String reason;

    public RideCancellationRequest() {
    }

    public RideCancellationRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
