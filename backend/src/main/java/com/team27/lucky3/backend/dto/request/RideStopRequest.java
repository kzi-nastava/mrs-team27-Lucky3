package com.team27.lucky3.backend.dto.request;

import com.team27.lucky3.backend.dto.LocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideStopRequest {
    @NotNull(message = "Stop location is required")
    @Valid
    private LocationDto stopLocation;
}

