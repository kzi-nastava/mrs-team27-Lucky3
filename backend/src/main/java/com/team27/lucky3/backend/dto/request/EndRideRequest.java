package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndRideRequest {
    @NotNull(message = "Passengers exited confirmation is required")
    private Boolean passengersExited;

    @NotNull(message = "Payment confirmation is required")
    private Boolean paid;
}

