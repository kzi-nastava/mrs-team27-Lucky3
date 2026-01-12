package com.team27.lucky3.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.dto.request.UpdateDriverRequest;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.DriverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/drivers")
@RequiredArgsConstructor
@Validated

public class DriverController {
    private final DriverService driverService;

    // 2.2.3 Admin creates driver accounts + vehicle info + password setup via email link (admin, driver)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DriverResponse> createDriver(@RequestPart("request") String requestJson,
                                                       @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        CreateDriverRequest request = mapper.readValue(requestJson, CreateDriverRequest.class);

        DriverResponse created = driverService.createDriver(request, profileImage);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        DriverResponse response = driverService.getDriver(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DriverResponse> updateDriver(@PathVariable Long id, @Valid @RequestBody UpdateDriverRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Driver not found");

        request.getVehicle().setDriverId(id);
        DriverResponse response = new DriverResponse(id, request.getName(), request.getSurname(), request.getEmail(), "url", UserRole.DRIVER, request.getPhone(), request.getAddress(), request.getVehicle(), request.isActive(), request.getActive24h());
        return ResponseEntity.ok(response);
    }


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
        vehicle.setDriverId(id);
        return ResponseEntity.ok(vehicle);
    }
}
