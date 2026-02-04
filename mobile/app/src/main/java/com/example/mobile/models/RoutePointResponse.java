package com.example.mobile.models;

public class RoutePointResponse {
    private LocationDto location;
    private int order;

    public RoutePointResponse() {
    }

    public RoutePointResponse(LocationDto location, int order) {
        this.location = location;
        this.order = order;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
