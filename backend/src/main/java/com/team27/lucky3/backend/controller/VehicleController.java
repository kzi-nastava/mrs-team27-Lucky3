package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class VehicleController {

    // 2.1.1 Display active vehicles on map (unregistered user)
    @GetMapping("/active")
    public ResponseEntity<List<VehicleLocationResponse>> getActiveVehicles() {
        List<VehicleLocationResponse> vehicles = new ArrayList<>();

        // --- AVAILABLE VEHICLES (Green on map) ---
        // Center / Trg Slobode area
        vehicles.add(new VehicleLocationResponse(1L, VehicleType.STANDARD, 45.2551, 19.8450, 101L, true));
        // Liman 1
        vehicles.add(new VehicleLocationResponse(2L, VehicleType.LUXURY, 45.2480, 19.8480, 102L, true));
        // Grbavica
        vehicles.add(new VehicleLocationResponse(3L, VehicleType.STANDARD, 45.2495, 19.8360, 103L, true));
        // Novo Naselje
        vehicles.add(new VehicleLocationResponse(4L, VehicleType.VAN, 45.2530, 19.8050, 104L, true));
        // Petrovaradin
        vehicles.add(new VehicleLocationResponse(5L, VehicleType.STANDARD, 45.2510, 19.8650, 105L, true));

        // --- OCCUPIED VEHICLES (Red on map) ---
        // Train Station area
        vehicles.add(new VehicleLocationResponse(6L, VehicleType.STANDARD, 45.2630, 19.8290, 106L, false));
        // Promenade
        vehicles.add(new VehicleLocationResponse(7L, VehicleType.LUXURY, 45.2420, 19.8520, 107L, false));
        // Detelinara
        vehicles.add(new VehicleLocationResponse(8L, VehicleType.STANDARD, 45.2610, 19.8150, 108L, false));

        return ResponseEntity.ok(vehicles);
    }
}
