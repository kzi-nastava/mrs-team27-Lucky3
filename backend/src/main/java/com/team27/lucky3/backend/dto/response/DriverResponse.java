package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.entity.enums.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DriverResponse extends UserResponse {
    private VehicleInformation vehicle;
    private boolean isActive;
    private boolean isBlocked;
    private String active24h;
    //TODO: maybe add rating, total rides, total earnings, etc.

    public DriverResponse(Long id, String name, String surname, String email, String profilePictureUrl, UserRole role, String phoneNumber, String address, VehicleInformation vehicle, boolean isActive, boolean isBlocked, String active24h) {
        super(id, name, surname, email, profilePictureUrl, role, phoneNumber, address);
        this.vehicle = vehicle;
        this.isActive = isActive;
        this.isBlocked = isBlocked;
        this.active24h = active24h;
    }
}

