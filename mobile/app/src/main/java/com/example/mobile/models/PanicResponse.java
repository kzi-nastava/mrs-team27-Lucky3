package com.example.mobile.models;

/**
 * Response DTO for a panic alert.
 * Matches backend's PanicResponse.
 */
public class PanicResponse {

    private Long id;
    private PanicUserResponse user;
    private PanicRideResponse ride;
    private String time;
    private String reason;

    // ======================== Getters & Setters ========================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PanicUserResponse getUser() { return user; }
    public void setUser(PanicUserResponse user) { this.user = user; }

    public PanicRideResponse getRide() { return ride; }
    public void setRide(PanicRideResponse ride) { this.ride = ride; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    // ======================== Nested Classes ========================

    public static class PanicUserResponse {
        private Long id;
        private String name;
        private String surname;
        private String email;
        private String profilePictureUrl;
        private String role;
        private String phoneNumber;
        private String address;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSurname() { return surname; }
        public void setSurname(String surname) { this.surname = surname; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getProfilePictureUrl() { return profilePictureUrl; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getFullName() {
            String full = "";
            if (name != null) full += name;
            if (surname != null) full += " " + surname;
            return full.trim().isEmpty() ? "Unknown" : full.trim();
        }
    }

    public static class PanicRideResponse {
        private Long id;
        private PanicLocationDto startLocation;
        private PanicLocationDto endLocation;
        private String status;
        private PanicUserResponse driver;
        private java.util.List<PanicUserResponse> passengers;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public PanicLocationDto getStartLocation() { return startLocation; }
        public void setStartLocation(PanicLocationDto startLocation) { this.startLocation = startLocation; }

        public PanicLocationDto getEndLocation() { return endLocation; }
        public void setEndLocation(PanicLocationDto endLocation) { this.endLocation = endLocation; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public PanicUserResponse getDriver() { return driver; }
        public void setDriver(PanicUserResponse driver) { this.driver = driver; }

        public java.util.List<PanicUserResponse> getPassengers() { return passengers; }
        public void setPassengers(java.util.List<PanicUserResponse> passengers) { this.passengers = passengers; }
    }

    public static class PanicLocationDto {
        private String address;
        private Double latitude;
        private Double longitude;

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
}
