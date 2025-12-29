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

import java.util.List;

@RestController
@RequestMapping(value = "/api/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class VehicleController {

    // 2.1.1 Display active vehicles on map (unregistered user)
    @GetMapping("/active")
    public ResponseEntity<List<VehicleLocationResponse>> getActiveVehicles() {
        List<VehicleLocationResponse> vehicles = List.of(
                new VehicleLocationResponse(1L, VehicleType.STANDARD, 45.2464, 19.8517, 10L, true),
                new VehicleLocationResponse(2L, VehicleType.VAN, 45.2450, 19.8500, 11L, false)
        );
        return ResponseEntity.ok(vehicles);
    }
}
