package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.UpdateVehiclePriceRequest;
import com.team27.lucky3.backend.entity.DriverChangeRequest;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AdminController {
    // 2.14 Define ride price
    @PutMapping("/vehicle-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateVehiclePrice(@Valid @RequestBody UpdateVehiclePriceRequest request) {
        // Logic to update price in DB
        return ResponseEntity.noContent().build();
    }

}
