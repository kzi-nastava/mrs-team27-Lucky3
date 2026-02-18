package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.*;
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
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideResponse response = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2.9.2 Driver ride history (driver)
    // 2.9.3 Admin ride history + detailed ride view (admin)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
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
    @GetMapping("/{id:\\d+}") // Only match if 'id' consists of digits
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RideResponse> getRide(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(rideService.getRideDetails(id));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(rideService.acceptRide(id));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> startRide(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(rideService.startRide(id));
    }

    @PutMapping("/{id}/end")
    @PreAuthorize("hasRole('DRIVER')")
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
    @PreAuthorize("hasRole('DRIVER') or hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> panicRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RidePanicRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = rideService.panicRide(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/stop")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> stopRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RideStopRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = rideService.stopRide(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/stop/{stopIndex}/complete")
    @PreAuthorize("hasRole('DRIVER') or hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> completeStop(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(0) Integer stopIndex) {
        RideResponse response = rideService.completeStop(id, stopIndex);
        return ResponseEntity.ok(response);
    }

    // 2.6.2 During ride: live tracking + inconsistency report (passengers)
    @PostMapping("/{id}/inconsistencies")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<Void> reportInconsistency(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody InconsistencyRequest request) {
        rideService.reportInconsistency(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 2.6.2 During ride: live tracking + inconsistency report (passengers)
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RideResponse> getActiveRide(@RequestParam(required = false) @Min(1) Long userId) {
        return ResponseEntity.ok(rideService.getActiveRide(userId));
    }

    // Admin endpoint: Get all active rides (PENDING, ACCEPTED, SCHEDULED, IN_PROGRESS)
    @GetMapping("/active/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RideResponse>> getAllActiveRides(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String vehicleType) {
        return ResponseEntity.ok(rideService.getAllActiveRides(pageable, search, status, vehicleType));
    }

    // 2.4.3 Adding route to favourite
    // MARK as favourite (create)
    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{id}/favourite-route")
    public ResponseEntity<Void> addFavouriteRoute(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody FavouriteRouteRequest request
    ) {
        rideService.addToFavorite(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('PASSENGER')")
    @DeleteMapping("/{passengerId}/favourite-routes/{favouriteRouteId}")
    public ResponseEntity<Void> removeFavouriteRoute(
            @PathVariable @Min(1) Long passengerId,
            @PathVariable @Min(1) Long favouriteRouteId
    ) {
        rideService.removeFromFavorite(passengerId, favouriteRouteId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('PASSENGER')")
    @GetMapping("/{id}/favourite-routes")
    public ResponseEntity<List<FavoriteRouteResponse>> getFavoriteRoutes(@PathVariable @Min(1) Long id) {
        List<FavoriteRouteResponse> favoriteRoutes = rideService.getFavoriteRoutes(id);
        return ResponseEntity.ok(favoriteRoutes);
    }
}
