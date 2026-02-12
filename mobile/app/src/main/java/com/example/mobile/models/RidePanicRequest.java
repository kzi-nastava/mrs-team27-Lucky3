package com.example.mobile.models;

/**
 * Request DTO for triggering the PANIC button on a ride.
 * Matches backend's RidePanicRequest.
 */
public class RidePanicRequest {

    private String reason;

    public RidePanicRequest() {}

    public RidePanicRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
