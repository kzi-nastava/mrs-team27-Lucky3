package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ActivationRequest;
import com.team27.lucky3.backend.dto.request.PassengerRegistrationRequest;
import com.team27.lucky3.backend.dto.response.LocationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.RoutePointResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
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

    @PutMapping(value = "/activation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> activatePassenger(@Valid @RequestBody ActivationRequest activationRequest) {
        return ResponseEntity.ok(Collections.singletonMap("message", "Account activated successfully!"));
    }

    @GetMapping("/activations/{activationId}")
    public ResponseEntity<Map<String, String>> activatePassenger(@PathVariable @Min(1) Long activationId) {
        return new ResponseEntity<>(Collections.singletonMap("message", "Account activated successfully!"), HttpStatus.OK);
    }

    @GetMapping("/{id}/active-ride")
    public ResponseEntity<RideResponse> getPassengerActiveRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Active ride not found for passenger");

        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.ACTIVE);
        response.setDriverId(10L);
        response.setPassengerIds(List.of(id));
        response.setStartTime(LocalDateTime.now().minusMinutes(10));
        response.setEndTime(null);
        response.setTotalCost(500.0);
        response.setEstimatedCost(450.0);
        response.setVehicleType(VehicleType.STANDARD);
        response.setVehicleLocation(loc);
        response.setRoutePoints(List.of(new RoutePointResponse(45.2464, 19.8517, 1)));
        response.setEtaMinutes(5);
        response.setDistanceKm(3.5);

        // Set new fields
        response.setDriverEmail("driver10@example.com");
        response.setPassengerEmail("passenger" + id + "@example.com");
        response.setEstimatedTimeInMinutes(5);
        response.setRideStatus(RideStatus.ACTIVE.name());
        response.setRejectionReason(null);
        response.setPanicPressed(false);
        response.setDeparture("Bulevar oslobodjenja 10");
        response.setDestination("Bulevar oslobodjenja 10");

        return ResponseEntity.ok(response);
    }
}