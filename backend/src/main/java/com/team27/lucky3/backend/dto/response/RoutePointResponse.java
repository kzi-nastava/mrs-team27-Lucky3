package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePointResponse {
    private double latitude;
    private double longitude;
    private int order;
}

