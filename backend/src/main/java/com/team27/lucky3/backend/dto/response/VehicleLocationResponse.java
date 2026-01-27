package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLocationResponse {
    private Long id;
    private VehicleType vehicleType;
    private double latitude;
    private double longitude;
    private Long driverId;
    private boolean available;
    private boolean currentPanic;
}
