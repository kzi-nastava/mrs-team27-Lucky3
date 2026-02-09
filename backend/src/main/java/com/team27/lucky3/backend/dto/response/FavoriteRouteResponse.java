package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.LocationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRouteResponse {
    private Long id;
    private String routeName;
    private LocationDto startLocation;
    private LocationDto endLocation;
    private List<LocationDto> stops;
    private Double distance;
    private Double estimatedTime;
}

