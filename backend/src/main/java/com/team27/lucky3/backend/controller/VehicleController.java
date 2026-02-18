package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.dto.response.VehiclePriceResponse;
import com.team27.lucky3.backend.service.VehiclePriceService;
import com.team27.lucky3.backend.service.VehicleService;
import com.team27.lucky3.backend.service.VehicleSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Vehicles", description = "Active vehicles, pricing & simulation locks")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehiclePriceService vehiclePriceService;
    private final VehicleSimulationService vehicleSimulationService;

    @Operation(summary = "Get active vehicles", description = "List all vehicles of active drivers on the map (public)", security = {})
    @GetMapping("/active")
    public ResponseEntity<List<VehicleLocationResponse>> getActiveVehicles() {
        List<VehicleLocationResponse> vehicles = vehicleService.getPublicMapVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @Operation(summary = "Get vehicle prices", description = "Get pricing info for all vehicle types (public)", security = {})
    @GetMapping("/prices")
    public ResponseEntity<List<VehiclePriceResponse>> getVehiclePrices() {
        List<VehiclePriceResponse> prices = vehiclePriceService.getAllPrices().stream()
                .map(p -> new VehiclePriceResponse(p.getId(), p.getVehicleType(), p.getBaseFare(), p.getPricePerKm()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(prices);
    }

    @Operation(summary = "Update vehicle location", description = "Update lat/lng of a vehicle")
    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDto locationDto) {
        vehicleService.updateVehicleLocation(id, locationDto);
        return ResponseEntity.ok().build();
    }


//     Refresh a simulation lock for a vehicle.
//     The frontend calls this every ~10s to claim "leader" status so that only
//     one browser tab drives the vehicle during a ride. The backend patrol simulation
//     also respects this lock.

    @Operation(summary = "Acquire simulation lock", description = "Claim leader status for vehicle simulation (called every ~10s)")
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

    @Operation(summary = "Release simulation lock", description = "Give up simulation leader status for a vehicle")
    @DeleteMapping("/{id}/simulation-lock")
    public ResponseEntity<Void> releaseSimulationLock(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "");
        vehicleSimulationService.releaseLock(id, sessionId);
        return ResponseEntity.ok().build();
    }
}