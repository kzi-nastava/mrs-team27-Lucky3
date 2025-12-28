package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.CreateDriver;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
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
    public ResponseEntity<UserResponse> createDriver(@Valid @RequestBody CreateDriver request) {
        UserResponse response = new UserResponse(10L, request.getName(), request.getSurname(), request.getEmail(), "url", UserRole.DRIVER, request.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
