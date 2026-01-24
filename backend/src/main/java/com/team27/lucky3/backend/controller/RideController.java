package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.RideCreated;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.RoutePointResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.RideService;
import com.team27.lucky3.backend.util.DummyData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/rides", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class RideController {

    private final RideService rideService;

    // 2.1.2 Ride estimation on the map page (unregistered user)
    @PostMapping("/estimate")
    public ResponseEntity<RideEstimationResponse> estimateRide(@Valid @RequestBody CreateRideRequest request) {
        RideEstimationResponse response = rideService.estimateRide(request);
        return ResponseEntity.ok(response);
    }

    // 2.4.1 Order a ride (logged-in user)
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RideCreated> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideCreated response = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2.9.2 Driver ride history (driver)
    // 2.9.3 Admin ride history + detailed ride view (admin)
    @GetMapping
    public ResponseEntity<Page<RideResponse>> getRidesHistory(
            Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) @Min(1) Long driverId,
            @RequestParam(required = false) @Min(1) Long passengerId,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(rideService.getRidesHistory(pageable, fromDate, toDate, driverId, passengerId, status));
    }

    // 2.9.2 & 2.9.3 Admin ride history + detailed ride view (admin)
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(rideService.getRideDetails(id));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(rideService.acceptRide(id));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(rideService.startRide(id));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<RideResponse> endRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody EndRideRequest request) {
        return ResponseEntity.ok(rideService.endRide(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DRIVER') or hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RideCancellationRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = rideService.cancelRide(id, request.getReason());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/panic")
    public ResponseEntity<RideResponse> panicRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RidePanicRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = rideService.panicRide(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/stop")
    public ResponseEntity<RideResponse> stopRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RideStopRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = rideService.stopRide(id, request);
        return ResponseEntity.ok(response);
    }

    // 2.6.2 During ride: live tracking + inconsistency report (passengers)
    @PostMapping("/{id}/inconsistencies")
    public ResponseEntity<Void> reportInconsistency(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody InconsistencyRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 2.6.2 During ride: live tracking + inconsistency report (passengers)
    @GetMapping("/active")
    public ResponseEntity<RideResponse> getActiveRide(@RequestParam(required = false) @Min(1) Long userId) {
        return ResponseEntity.ok(rideService.getActiveRide(userId));
    }
}
