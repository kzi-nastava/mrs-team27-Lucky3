package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.InconsistencyRequest;
import com.team27.lucky3.backend.dto.request.RideEndRequest;
import com.team27.lucky3.backend.dto.response.LocationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/rides", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class RideController {

    @GetMapping
    public ResponseEntity<Page<RideResponse>> getRidesHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long passengerId) {

        List<RideResponse> rides = new ArrayList<>();
        rides.add(createDummyRideResponse(101L, 1L, 2L, RideStatus.FINISHED));
        rides.add(createDummyRideResponse(102L, 2L, 3L, RideStatus.REJECTED));
        rides.add(createDummyRideResponse(103L, 1L, 4L, RideStatus.PANIC));

        Pageable pageable = PageRequest.of(page, size);
        Page<RideResponse> ridePage = new PageImpl<>(rides, pageable, rides.size());

        return ResponseEntity.ok(ridePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.ok(createDummyRideResponse(id, 10L, 123L, RideStatus.ACTIVE));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<RideResponse> endRide(@PathVariable @Min(1) Long id, @RequestBody RideEndRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = createDummyRideResponse(id, 10L, 123L, RideStatus.FINISHED);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/inconsistencies")
    public ResponseEntity<Map<String, String>> reportInconsistency(@PathVariable @Min(1) Long id, @RequestBody InconsistencyRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.status(201).body(Collections.singletonMap("message", "Inconsistency reported: " + request.getRemark()));
    }

    private RideResponse createDummyRideResponse(Long id, Long driverId, Long passengerId, RideStatus status) {
        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        return new RideResponse(
                id, status, driverId, List.of(passengerId),
                LocalDateTime.now().minusMinutes(10),
                status == RideStatus.FINISHED ? LocalDateTime.now() : null,
                500.0, 450.0, VehicleType.STANDARD, loc, List.of(loc, loc), 5, 3.5
        );
    }
}