package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideEstimationResponse {
    private int estimatedTimeInMinutes; // Duration of the ride (A -> B)
    private double estimatedCost;
    private int estimatedDriverArrivalInMinutes; // Time for the closest driver to reach A
    private double estimatedDistance;
    private java.util.List<RoutePointResponse> routePoints; // For displaying route on map
}
