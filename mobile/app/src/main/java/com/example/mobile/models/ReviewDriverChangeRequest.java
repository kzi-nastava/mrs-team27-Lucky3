package com.example.mobile.models;

public class ReviewDriverChangeRequest {
    private boolean approve;

    public ReviewDriverChangeRequest() {}

    public ReviewDriverChangeRequest(boolean approve) {
        this.approve = approve;
    }

    public boolean isApprove() { return approve; }
    public void setApprove(boolean approve) { this.approve = approve; }
}