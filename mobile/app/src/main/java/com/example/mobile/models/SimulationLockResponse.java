package com.example.mobile.models;

/**
 * Response from the simulation lock acquisition endpoint.
 * Matches backend response from PUT /api/vehicles/{id}/simulation-lock.
 */
public class SimulationLockResponse {
    private boolean acquired;
    private String reason;

    public SimulationLockResponse() {}

    public boolean isAcquired() {
        return acquired;
    }

    public void setAcquired(boolean acquired) {
        this.acquired = acquired;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
