package com.example.mobile.models;
import java.time.LocalDateTime;

public class DriverChangeRequest {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String address;
    private String phone;
    private Long requestedDriverId;
    private LocalDateTime createdAt;
    private Long imageId;
    private VehicleInformation vehicle;
    private String status;

    public DriverChangeRequest() {}

    public DriverChangeRequest(Long id, String name, String surname, String email,
                               String address, String phone, Long requestedDriverId,
                               LocalDateTime createdAt, Long imageId,
                               VehicleInformation vehicle, String status) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.requestedDriverId = requestedDriverId;
        this.createdAt = createdAt;
        this.imageId = imageId;
        this.vehicle = vehicle;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getRequestedDriverId() { return requestedDriverId; }
    public void setRequestedDriverId(Long requestedDriverId) {
        this.requestedDriverId = requestedDriverId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }

    public VehicleInformation getVehicle() { return vehicle; }
    public void setVehicle(VehicleInformation vehicle) { this.vehicle = vehicle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

