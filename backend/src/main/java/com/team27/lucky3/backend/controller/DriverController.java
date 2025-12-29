package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.CreateDriver;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.UserRole;
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

@RestController
@RequestMapping(value = "/api/drivers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class DriverController {

    // 2.2.3 Admin creates driver accounts + vehicle info + password setup via email link (admin, driver)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriver request) {
        DriverResponse response = new DriverResponse(10L, request.getName(), request.getSurname(), request.getEmail(), "url", UserRole.DRIVER, request.getPhone(), request.getVehicle(), false, "0h 0m");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        if (id == 404) throw new ResourceNotFoundException("Driver not found");

        VehicleInformation vehicle = new VehicleInformation();
        vehicle.setModel("Toyota Prius");
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setLicenseNumber("NS-123-AB");
        vehicle.setPassengerSeats(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(true);

        DriverResponse response = new DriverResponse(id, "Pera", "Peric", "pera@gmail.com", "url", UserRole.DRIVER, "0601234567", vehicle, true, "5h 30m");
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DriverResponse> updateDriver(@PathVariable Long id, @Valid @RequestBody CreateDriver request) {
        if (id == 404) throw new ResourceNotFoundException("Driver not found");

        DriverResponse response = new DriverResponse(id, request.getName(), request.getSurname(), request.getEmail(), "url", UserRole.DRIVER, request.getPhone(), request.getVehicle(), true, "5h 30m");
        return ResponseEntity.ok(response);
    }

    // 2.2.3 Admin creates driver accounts + vehicle info + password setup via email link (admin, driver)
    // 2.3 Profile page (registered user, driver, admin)
    @GetMapping("/{id}/vehicle")
    public ResponseEntity<VehicleInformation> getDriverVehicle(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Driver not found");
        VehicleInformation vehicle = new VehicleInformation();
        vehicle.setModel("Toyota Prius");
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setLicenseNumber("NS-123-AB");
        vehicle.setPassengerSeats(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(true);
        return ResponseEntity.ok(vehicle);
    }
}
