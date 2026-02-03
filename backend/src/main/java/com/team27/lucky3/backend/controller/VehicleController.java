package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    // 2.1.1 Display active vehicles on map (unregistered user)
    // Returns vehicles of active drivers: green = FREE (not occupied), red = BUSY (occupied)
    @GetMapping("/active")
    public ResponseEntity<List<VehicleLocationResponse>> getActiveVehicles() {
        List<VehicleLocationResponse> vehicles = vehicleService.getPublicMapVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDto locationDto) {
        vehicleService.updateVehicleLocation(id, locationDto);
        return ResponseEntity.ok().build();
    }
}
