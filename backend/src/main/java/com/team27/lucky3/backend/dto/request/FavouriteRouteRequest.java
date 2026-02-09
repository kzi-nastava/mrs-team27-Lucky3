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
public class FavouriteRouteRequest {
    @NotNull(message = "Start location is required")
    @Valid
    private String start;

    @NotNull(message = "Destination is required")
    @Valid
    private String destination;

    private String routeName;
}
