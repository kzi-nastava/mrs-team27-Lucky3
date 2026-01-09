package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.EndRideRequest;
import com.team27.lucky3.backend.dto.request.InconsistencyRequest;
import com.team27.lucky3.backend.dto.request.RideCancellationRequest;
import com.team27.lucky3.backend.dto.request.RidePanicRequest;
import com.team27.lucky3.backend.dto.request.RideStopRequest;
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
        List<RoutePointResponse> route = List.of(
                new RoutePointResponse(new LocationDto("Bulevar oslobodjenja 10", 45.2464, 19.8517), 1),
                new RoutePointResponse(new LocationDto("Bulevar oslobodjenja 12", 45.2464, 19.8520), 2)
        );
        RideEstimationResponse response = new RideEstimationResponse(15, 650.00, route);
        return ResponseEntity.ok(response);
    }

    // 2.4.1 Order a ride (logged-in user)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideResponse response = DummyData.createDummyRideResponse(12L, 10L, 123L, RideStatus.PENDING);
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

        List<RideResponse> rides = List.of(
                DummyData.createDummyRideResponse(1L, 10L, 123L, RideStatus.FINISHED),
                DummyData.createDummyRideResponse(2L, 10L, 123L, RideStatus.CANCELLED)
        );
        return ResponseEntity.ok(new PageImpl<>(rides, pageable, rides.size()));
    }

    // 2.9.2 & 2.9.3 Admin ride history + detailed ride view (admin)
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.ok(DummyData.createDummyRideResponse(id, 10L, 123L, RideStatus.IN_PROGRESS));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.ok(DummyData.createDummyRideResponse(id, 10L, 123L, RideStatus.ACCEPTED));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.ok(DummyData.createDummyRideResponse(id, 10L, 123L, RideStatus.IN_PROGRESS));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<RideResponse> endRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody EndRideRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = DummyData.createDummyRideResponse(id, 10L, 123L, RideStatus.FINISHED);
        response.setPassengersExited(request.getPassengersExited());
        response.setPaid(request.getPaid());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
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
        return ResponseEntity.ok(DummyData.createDummyRideResponse(99L, 10L, userId != null ? userId : 123L, RideStatus.IN_PROGRESS));
    }
}
