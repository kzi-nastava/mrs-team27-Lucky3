package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.dto.response.VehiclePriceResponse;
import com.team27.lucky3.backend.service.VehiclePriceService;
import com.team27.lucky3.backend.service.VehicleService;
import com.team27.lucky3.backend.service.VehicleSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@CrossOrigin
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehiclePriceService vehiclePriceService;
    private final VehicleSimulationService vehicleSimulationService;

    // 2.1.1 Display active vehicles on map (unregistered user)
    // Returns vehicles of active drivers: green = FREE (not occupied), red = BUSY (occupied)
    @GetMapping("/active")
    public ResponseEntity<List<VehicleLocationResponse>> getActiveVehicles() {
        List<VehicleLocationResponse> vehicles = vehicleService.getPublicMapVehicles();
        return ResponseEntity.ok(vehicles);
    }

    // Public endpoint: get vehicle pricing for all types (used by ride estimation pages)
    @GetMapping("/prices")
    public ResponseEntity<List<VehiclePriceResponse>> getVehiclePrices() {
        List<VehiclePriceResponse> prices = vehiclePriceService.getAllPrices().stream()
                .map(p -> new VehiclePriceResponse(p.getId(), p.getVehicleType(), p.getBaseFare(), p.getPricePerKm()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(prices);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDto locationDto) {
        vehicleService.updateVehicleLocation(id, locationDto);
        return ResponseEntity.ok().build();
    }


//     Refresh a simulation lock for a vehicle.
//     The frontend calls this every ~10s to claim "leader" status so that only
//     one browser tab drives the vehicle during a ride. The backend patrol simulation
//     also respects this lock.

    @PutMapping("/{id}/simulation-lock")
    public ResponseEntity<Map<String, Object>> acquireSimulationLock(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "");
        if (sessionId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("acquired", false, "reason", "sessionId is required"));
        }
        boolean acquired = vehicleSimulationService.acquireLock(id, sessionId);
        return ResponseEntity.ok(Map.of("acquired", acquired));
    }

    /**
     * Release a simulation lock for a vehicle.
     */
    @DeleteMapping("/{id}/simulation-lock")
    public ResponseEntity<Void> releaseSimulationLock(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "");
        vehicleSimulationService.releaseLock(id, sessionId);
        return ResponseEntity.ok().build();
    }
}