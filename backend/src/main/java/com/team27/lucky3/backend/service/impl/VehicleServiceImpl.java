package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.VehicleStatus;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.team27.lucky3.backend.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public List<VehicleLocationResponse> getPublicMapVehicles() {
        // Fetch all active vehicles (drivers who are working)
        List<Vehicle> vehicles = vehicleRepository.findAllActiveVehicles();

        return vehicles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VehicleLocationResponse mapToResponse(Vehicle vehicle) {
        boolean isAvailable = vehicle.getStatus() == VehicleStatus.FREE;
        double lat = vehicle.getCurrentLocation() != null ? vehicle.getCurrentLocation().getLatitude() : 0.0;
        double lon = vehicle.getCurrentLocation() != null ? vehicle.getCurrentLocation().getLongitude() : 0.0;
        Long driverId = vehicle.getDriver() != null ? vehicle.getDriver().getId() : null;

        return new VehicleLocationResponse(
                vehicle.getId(),
                vehicle.getVehicleType(),
                lat,
                lon,
                driverId,
                isAvailable
        );
    }
}
