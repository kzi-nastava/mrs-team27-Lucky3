package com.team27.lucky3.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.DriverChangeRequestCreated;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.dto.response.DriverStatsResponse;
import com.team27.lucky3.backend.dto.response.DriverStatusResponse;
import com.team27.lucky3.backend.entity.DriverChangeRequest;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import com.team27.lucky3.backend.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/drivers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Drivers", description = "Driver management, status toggle, statistics & vehicle info")
public class DriverController {
    private final DriverService driverService;
    private final DriverChangeRequestService driverChangeRequestService;

    @Operation(summary = "Toggle driver status", description = "Set driver online/offline (DRIVER only)")
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/status")
    public ResponseEntity<DriverStatusResponse> toggleDriverStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        User driver = driverService.toggleActivity(id, active);
        // Return full status after toggle
        DriverStatusResponse response = driverService.getDriverStatus(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get driver status", description = "Get current online/offline status and working hours (DRIVER only)")
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/{id}/status")
    public ResponseEntity<DriverStatusResponse> getDriverStatus(@PathVariable Long id) {
        DriverStatusResponse response = driverService.getDriverStatus(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get driver stats", description = "Earnings, rides completed, rating, online hours (public)", security = {})
    //@PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/{id}/stats")
    public ResponseEntity<DriverStatsResponse> getDriverStats(@PathVariable Long id) {
        DriverStatsResponse response = driverService.getDriverStats(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create driver (admin)", description = "Admin creates a new driver account with vehicle info")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestPart("request") CreateDriverRequest request,
                                                       @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
        DriverResponse created = driverService.createDriver(request, profileImage);
        return ResponseEntity.ok(created);
    }

    @Operation(summary = "Set initial driver password", description = "Driver sets password via activation token (public, no auth required)", security = {})
    @PostMapping(value = "/driver-activation/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setInitialPassword(@Valid @RequestBody SetInitialPassword initialPassword,
                                                   PasswordEncoder passwordEncoder,
                                                   DriverService driverService) {
        driverService.activateDriverWithPassword(initialPassword.getToken(), initialPassword.getPassword(), passwordEncoder);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all drivers (admin)", description = "List all driver accounts (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        List<DriverResponse> drivers = driverService.getAllDrivers();

        return ResponseEntity.ok(drivers);
    }

    @Operation(summary = "Get driver by ID", description = "Get driver profile details")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        DriverResponse response = driverService.getDriver(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Request profile change", description = "Driver submits a profile change request for admin review (DRIVER only)")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverChangeRequestCreated> createDriver(
            @PathVariable Long id,
            @Valid @RequestPart("request") CreateDriverRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {
        DriverChangeRequest changeRequest = driverChangeRequestService.createChangeRequest(id, request, profileImage);

        DriverChangeRequestCreated body = new DriverChangeRequestCreated(
                id,
                changeRequest.getId(),
                DriverChangeStatus.PENDING
        );

        return ResponseEntity
                .accepted() //202
                .body(body);
    }

    @Operation(summary = "Get driver vehicle", description = "Get vehicle info for a driver")
    @GetMapping("/{id}/vehicle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VehicleInformation> getDriverVehicle(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Driver not found");
        VehicleInformation vehicle = new VehicleInformation();
        vehicle.setModel("Toyota Prius");
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setLicenseNumber("NS-123-AB");
        vehicle.setPassengerSeats(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(true);
        vehicle.setDriverId(id);
        return ResponseEntity.ok(vehicle);
    }
}