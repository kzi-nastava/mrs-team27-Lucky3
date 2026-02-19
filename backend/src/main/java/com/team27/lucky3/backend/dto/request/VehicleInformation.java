package com.team27.lucky3.backend.dto.request;

import com.team27.lucky3.backend.entity.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleInformation {
    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Type is required")
    private VehicleType vehicleType; // Renamed from 'type' to be explicit

    @NotBlank(message = "License plate number is required")
    private String licenseNumber; // Standardized

    @NotNull(message = "Seats count is required")
    @Min(value = 1)
    private Integer passengerSeats; // Standardized

    @NotNull
    private Boolean babyTransport;

    @NotNull
    private Boolean petTransport;

    private Long driverId;
}