package com.team27.lucky3.backend.dto.request;

import com.team27.lucky3.backend.entity.enums.VehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequirements {
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType; // STANDARD / VAN / LUX

    private boolean babyTransport;
    private boolean petTransport;
}

