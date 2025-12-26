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
    @Valid
    private String start;

    @NotNull @Valid
    private String destination;

    // ako imate međustanice
    @Valid
    private List<String> stops;

    @NotNull @Valid
    private RideRequirements requirements;

    // opciono: da frontend pošalje kilometražu koju je izračunao
    // ali bolje da backend računa (ili barem verifikuje)
    @Positive
    private Double kilometers;
}
