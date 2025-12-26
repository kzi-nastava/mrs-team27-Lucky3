package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.PassengerRegistrationRequest;
import com.team27.lucky3.backend.dto.response.LocationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/passengers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class PassengerController {

    @PostMapping
    public ResponseEntity<UserResponse> registerPassenger(@RequestBody PassengerRegistrationRequest request) {
        UserResponse response = new UserResponse(1L, request.getName(), request.getEmail(), "PASSENGER");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/activations/{activationId}")
    public ResponseEntity<Map<String, String>> activatePassenger(@PathVariable @Min(1) Long activationId) {
        return new ResponseEntity<>(Collections.singletonMap("message", "Account activated successfully!"), HttpStatus.OK);
    }

    @GetMapping("/{id}/active-ride")
    public ResponseEntity<RideResponse> getPassengerActiveRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Active ride not found for passenger");

        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RideResponse response = new RideResponse(
                1L, RideStatus.ACTIVE, 10L, List.of(id),
                LocalDateTime.now().minusMinutes(10), null,
                500.0, 450.0, VehicleType.STANDARD, loc, List.of(loc, loc), 5, 3.5
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}