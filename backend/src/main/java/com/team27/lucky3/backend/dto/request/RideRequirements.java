package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequirements {

    @NotEmpty
    private String vehicleType; // STANDARD / VAN / LUX

    private boolean babyTransport;
    private boolean petTransport;
}

