package com.example.mobile.models;

public class RideStopRequest {

    private LocationDto stopLocation;

    public RideStopRequest() {
    }

    public RideStopRequest(LocationDto stopLocation) {
        this.stopLocation = stopLocation;
    }

    public LocationDto getStopLocation() {
        return stopLocation;
    }

    public void setStopLocation(LocationDto stopLocation) {
        this.stopLocation = stopLocation;
    }
}
