package com.example.mobile.models;

import java.util.List;

public class FavoriteRouteResponse {
    private Long id;
    private String routeName;
    private LocationDto startLocation;
    private LocationDto endLocation;
    private List<LocationDto> stops;
    private Double distance;
    private Double estimatedTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public LocationDto getStartLocation() { return startLocation; }
    public void setStartLocation(LocationDto startLocation) { this.startLocation = startLocation; }

    public LocationDto getEndLocation() { return endLocation; }
    public void setEndLocation(LocationDto endLocation) { this.endLocation = endLocation; }

    public List<LocationDto> getStops() { return stops; }
    public void setStops(List<LocationDto> stops) { this.stops = stops; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public Double getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(Double estimatedTime) { this.estimatedTime = estimatedTime; }
}
