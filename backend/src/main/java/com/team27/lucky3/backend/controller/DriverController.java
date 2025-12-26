package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.LocationResponse;
import com.team27.lucky3.backend.dto.response.RideHistoryItemResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.RoutePointResponse;
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
        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.ACTIVE);
        response.setDriverId(id);
        response.setPassengerIds(List.of(123L));
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
        response.setDriverEmail("driver" + id + "@example.com");
        response.setPassengerEmail("passenger123@example.com");
        response.setEstimatedTimeInMinutes(5);
        response.setRideStatus(RideStatus.ACTIVE.name());
        response.setRejectionReason(null);
        response.setPanicPressed(false);
        response.setDeparture("Bulevar oslobodjenja 10");
        response.setDestination("Bulevar oslobodjenja 10");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponse> getDriverRideDetails(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");

        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RideResponse response = new RideResponse();
        response.setId(id);
        response.setStatus(RideStatus.FINISHED);
        response.setDriverId(10L);
        response.setPassengerIds(List.of(123L));
        response.setStartTime(LocalDateTime.now().minusDays(1));
        response.setEndTime(LocalDateTime.now().minusDays(1).plusMinutes(20));
        response.setTotalCost(500.0);
        response.setEstimatedCost(450.0);
        response.setVehicleType(VehicleType.STANDARD);
        response.setVehicleLocation(loc);
        response.setRoutePoints(List.of(new RoutePointResponse(45.2464, 19.8517, 1)));
        response.setEtaMinutes(0);
        response.setDistanceKm(3.5);

        // Set new fields
        response.setDriverEmail("driver10@example.com");
        response.setPassengerEmail("passenger123@example.com");
        response.setEstimatedTimeInMinutes(20);
        response.setRideStatus(RideStatus.FINISHED.name());
        response.setRejectionReason(null);
        response.setPanicPressed(false);
        response.setDeparture("Bulevar oslobodjenja 10");
        response.setDestination("Bulevar oslobodjenja 10");

        return ResponseEntity.ok(response);
    }
}
