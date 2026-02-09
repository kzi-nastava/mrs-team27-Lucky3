package com.example.mobile.models;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DTO for ride response from the backend.
 * Matches the web app's RideResponse model.
 */
public class RideResponse {
    
    private Long id;
    private String status;
    
    // Driver info
    private DriverInfo driver;
    private Long driverId;
    
    // Passengers
    private List<PassengerInfo> passengers;
    
    // Locations
    private LocationDto departure;
    private LocationDto destination;
    private LocationDto start;
    private LocationDto startLocation;
    private LocationDto endLocation;
    private List<LocationDto> stops;
    
    // Times
    private String scheduledTime;
    private String startTime;
    private String endTime;
    
    // Pricing and distance
    private Double estimatedCost;
    private Double totalCost;
    private Double distanceKm;
    private Double estimatedDistance;
    private Integer estimatedTimeInMinutes;
    private Double distanceTraveled;
    
    // Rate snapshot: locked at ride creation
    private Double rateBaseFare;
    private Double ratePricePerKm;
    
    // Vehicle info
    private String vehicleType;
    private String model;
    private String licensePlates;
    private Boolean babyTransport;
    private Boolean petTransport;
    
    // Status flags
    private Boolean panicPressed;
    private String panicReason;
    private String rejectionReason;
    private Boolean passengersExited;
    private Boolean paid;
    
    // Stop tracking
    private Set<Integer> completedStopIndexes;
    
    // Route points from backend
    private List<RoutePointResponse> routePoints;
    
    // Reviews and inconsistency reports
    private List<ReviewInfo> reviews;
    private List<InconsistencyInfo> inconsistencyReports;
    
    // Nested classes
    
    public static class ReviewInfo {
        private Long id;
        private Long rideId;
        private Long passengerId;
        private Integer driverRating;
        private Integer vehicleRating;
        private String comment;
        private String createdAt;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getRideId() { return rideId; }
        public void setRideId(Long rideId) { this.rideId = rideId; }
        public Long getPassengerId() { return passengerId; }
        public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }
        public Integer getDriverRating() { return driverRating; }
        public void setDriverRating(Integer driverRating) { this.driverRating = driverRating; }
        public Integer getVehicleRating() { return vehicleRating; }
        public void setVehicleRating(Integer vehicleRating) { this.vehicleRating = vehicleRating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    public static class InconsistencyInfo {
        private String description;
        private String timestamp;
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
    public static class DriverInfo {
        private Long id;
        private String name;
        private String surname;
        private String email;
        private String profilePicture;
        private String phoneNumber;
        private String address;
        private VehicleInfo vehicle;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSurname() { return surname; }
        public void setSurname(String surname) { this.surname = surname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public VehicleInfo getVehicle() { return vehicle; }
        public void setVehicle(VehicleInfo vehicle) { this.vehicle = vehicle; }
    }
    
    public static class VehicleInfo {
        private String model;
        private String vehicleType;
        @SerializedName("licenseNumber")
        private String licensePlates;
        @SerializedName("passengerSeats")
        private Integer seatCount;
        private Boolean babyTransport;
        private Boolean petTransport;
        private String color;
        private Integer year;
        
        // Getters and setters
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
        public String getLicensePlates() { return licensePlates; }
        public void setLicensePlates(String licensePlates) { this.licensePlates = licensePlates; }
        public Integer getSeatCount() { return seatCount; }
        public void setSeatCount(Integer seatCount) { this.seatCount = seatCount; }
        public Boolean getBabyTransport() { return babyTransport; }
        public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }
        public Boolean getPetTransport() { return petTransport; }
        public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
    }
    
    public static class PassengerInfo {
        private Long id;
        private String name;
        private String surname;
        private String email;
        private String phoneNumber;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSurname() { return surname; }
        public void setSurname(String surname) { this.surname = surname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getFullName() {
            StringBuilder sb = new StringBuilder();
            if (name != null) sb.append(name);
            if (surname != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(surname);
            }
            return sb.length() > 0 ? sb.toString() : "Unknown";
        }
    }
    
    // Main class getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public DriverInfo getDriver() { return driver; }
    public void setDriver(DriverInfo driver) { this.driver = driver; }
    
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    
    public List<PassengerInfo> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerInfo> passengers) { this.passengers = passengers; }
    
    public LocationDto getDeparture() { return departure; }
    public void setDeparture(LocationDto departure) { this.departure = departure; }
    
    public LocationDto getDestination() { return destination; }
    public void setDestination(LocationDto destination) { this.destination = destination; }
    
    public LocationDto getStart() { return start; }
    public void setStart(LocationDto start) { this.start = start; }
    
    public LocationDto getStartLocation() { return startLocation; }
    public void setStartLocation(LocationDto startLocation) { this.startLocation = startLocation; }
    
    public LocationDto getEndLocation() { return endLocation; }
    public void setEndLocation(LocationDto endLocation) { this.endLocation = endLocation; }
    
    public List<LocationDto> getStops() { return stops; }
    public void setStops(List<LocationDto> stops) { this.stops = stops; }
    
    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }
    
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    
    public Double getEstimatedDistance() { return estimatedDistance; }
    public void setEstimatedDistance(Double estimatedDistance) { this.estimatedDistance = estimatedDistance; }
    
    public Integer getEstimatedTimeInMinutes() { return estimatedTimeInMinutes; }
    public void setEstimatedTimeInMinutes(Integer estimatedTimeInMinutes) { this.estimatedTimeInMinutes = estimatedTimeInMinutes; }
    
    public Double getDistanceTraveled() { return distanceTraveled; }
    public void setDistanceTraveled(Double distanceTraveled) { this.distanceTraveled = distanceTraveled; }
    
    public Double getRateBaseFare() { return rateBaseFare; }
    public void setRateBaseFare(Double rateBaseFare) { this.rateBaseFare = rateBaseFare; }
    
    public Double getRatePricePerKm() { return ratePricePerKm; }
    public void setRatePricePerKm(Double ratePricePerKm) { this.ratePricePerKm = ratePricePerKm; }
    
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getLicensePlates() { return licensePlates; }
    public void setLicensePlates(String licensePlates) { this.licensePlates = licensePlates; }
    
    public Boolean getBabyTransport() { return babyTransport; }
    public void setBabyTransport(Boolean babyTransport) { this.babyTransport = babyTransport; }
    
    public Boolean getPetTransport() { return petTransport; }
    public void setPetTransport(Boolean petTransport) { this.petTransport = petTransport; }
    
    public Boolean getPanicPressed() { return panicPressed; }
    public void setPanicPressed(Boolean panicPressed) { this.panicPressed = panicPressed; }
    
    public String getPanicReason() { return panicReason; }
    public void setPanicReason(String panicReason) { this.panicReason = panicReason; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public Boolean getPassengersExited() { return passengersExited; }
    public void setPassengersExited(Boolean passengersExited) { this.passengersExited = passengersExited; }
    
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    
    public Set<Integer> getCompletedStopIndexes() {
        return completedStopIndexes != null ? completedStopIndexes : new HashSet<>();
    }
    public void setCompletedStopIndexes(Set<Integer> completedStopIndexes) {
        this.completedStopIndexes = completedStopIndexes;
    }
    
    public List<RoutePointResponse> getRoutePoints() { return routePoints; }
    public void setRoutePoints(List<RoutePointResponse> routePoints) { this.routePoints = routePoints; }
    
    public List<ReviewInfo> getReviews() { return reviews; }
    public void setReviews(List<ReviewInfo> reviews) { this.reviews = reviews; }
    
    public List<InconsistencyInfo> getInconsistencyReports() { return inconsistencyReports; }
    public void setInconsistencyReports(List<InconsistencyInfo> inconsistencyReports) { this.inconsistencyReports = inconsistencyReports; }
    
    // Helper methods to get locations with fallbacks (like web app)
    public LocationDto getEffectiveStartLocation() {
        if (departure != null) return departure;
        if (start != null) return start;
        return startLocation;
    }
    
    public LocationDto getEffectiveEndLocation() {
        if (destination != null) return destination;
        return endLocation;
    }
    
    public Double getEffectiveDistance() {
        if (distanceKm != null) return distanceKm;
        if (distanceTraveled != null) return distanceTraveled;
        return 0.0;
    }
    
    public Double getEffectiveCost() {
        if (totalCost != null) return totalCost;
        if (estimatedCost != null) return estimatedCost;
        return 0.0;
    }
    
    public boolean isCancelled() {
        return status != null && (
            status.equals("CANCELLED") || 
            status.equals("CANCELLED_BY_DRIVER") || 
            status.equals("CANCELLED_BY_PASSENGER")
        );
    }
    
    public boolean isFinished() {
        return status != null && status.equals("FINISHED");
    }
    
    public String getDisplayStatus() {
        if (status == null) return "Unknown";
        switch (status) {
            case "FINISHED": return "Completed";
            case "CANCELLED": 
            case "CANCELLED_BY_DRIVER": 
            case "CANCELLED_BY_PASSENGER": 
                return "Cancelled";
            case "PENDING": return "Pending";
            case "ACCEPTED": return "Accepted";
            case "IN_PROGRESS": return "In Progress";
            case "REJECTED": return "Rejected";
            case "PANIC": return "Panic";
            case "SCHEDULED": return "Scheduled";
            default: return status;
        }
    }
    
    public String getCancelledBy() {
        if (status == null) return "";
        switch (status) {
            case "CANCELLED_BY_DRIVER": return "Driver";
            case "CANCELLED_BY_PASSENGER": return "Passenger";
            case "CANCELLED": return "System";
            default: return "";
        }
    }
}
