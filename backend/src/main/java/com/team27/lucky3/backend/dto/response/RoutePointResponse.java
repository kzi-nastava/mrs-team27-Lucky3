package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.LocationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePointResponse {
    private LocationDto location;
    private int order;
}

