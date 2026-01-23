package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import java.util.List;

public interface VehicleService {
    List<VehicleLocationResponse> getPublicMapVehicles();
}
