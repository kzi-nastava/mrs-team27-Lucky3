package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/active")
    public ResponseEntity<List<VehicleLocationResponse>> getActiveVehicles() {
        VehicleLocationResponse v1 = new VehicleLocationResponse(1L, VehicleType.STANDARD, 45.2464, 19.8517, 10L, true);
        VehicleLocationResponse v2 = new VehicleLocationResponse(2L, VehicleType.LUXURY, 45.2544, 19.8427, 11L, true);
        return new ResponseEntity<>(List.of(v1, v2), HttpStatus.OK);
    }
}
