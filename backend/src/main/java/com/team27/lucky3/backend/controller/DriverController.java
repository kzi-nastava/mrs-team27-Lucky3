package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.LocationResponse;
import com.team27.lucky3.backend.dto.response.RideHistoryItemResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/drivers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class DriverController {

    @GetMapping("/{id}/rides")
    public ResponseEntity<List<RideHistoryItemResponse>> getDriverRideHistory(
            @PathVariable @Min(1) Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (id == 404) throw new ResourceNotFoundException("Driver not found");

        RideHistoryItemResponse item = new RideHistoryItemResponse(
                101L, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusMinutes(20),
                "Bulevar oslobodjenja 10", "Futoska 5", 500.0, RideStatus.FINISHED, null, false
        );

        return new ResponseEntity<>(List.of(item), HttpStatus.OK);
    }

    @GetMapping("/{id}/active-ride")
    public ResponseEntity<RideResponse> getDriverActiveRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Active ride not found for driver");

        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RideResponse response = new RideResponse(
                1L, RideStatus.ACTIVE, id, List.of(123L),
                LocalDateTime.now().minusMinutes(10), null,
                500.0, 450.0, VehicleType.STANDARD, loc, List.of(loc, loc), 5, 3.5
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponse> getDriverRideDetails(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");

        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RideResponse response = new RideResponse(
                id, RideStatus.FINISHED, 10L, List.of(123L),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusMinutes(20),
                500.0, 450.0, VehicleType.STANDARD, loc, List.of(loc, loc), 0, 3.5
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
