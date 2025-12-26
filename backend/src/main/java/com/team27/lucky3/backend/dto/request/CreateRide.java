package com.team27.lucky3.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRide {
    @NotNull
    private String start;

    @NotNull
    private String destination;

    private List<String> stops;

    @NotNull
    @Valid
    private RideRequirements requirements;

    @Positive
    private Double kilometers;
}

