package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ChangePasswordRequest;
import com.team27.lucky3.backend.dto.request.CreateDriver;
import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.PasswordResetRequest;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.FavoriteRouteResponse;
import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.util.DummyData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class UserController {

    @PostMapping("/drivers")
    public ResponseEntity<UserResponse> createDriver(@Valid @RequestBody CreateDriver request) {
        UserResponse response = new UserResponse(1L, request.getName(), request.getSurname(), request.getEmail(), "default.png", UserRole.DRIVER, request.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(DummyData.createDummyUserProfile(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UserProfile request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(request);
    }
    
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vehicle")
    public ResponseEntity<VehicleInformation> getVehicle(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(DummyData.createDummyVehicle(id));
    }

    @GetMapping("/{id}/favorites")
    public ResponseEntity<List<FavoriteRouteResponse>> getFavoriteRoutes(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(List.of(DummyData.createDummyFavoriteRoute(1L)));
    }

    @PostMapping("/{id}/favorites")
    public ResponseEntity<FavoriteRouteResponse> addFavoriteRoute(@PathVariable @Min(1) Long id, @Valid @RequestBody FavoriteRouteResponse route) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.status(HttpStatus.CREATED).body(DummyData.createDummyFavoriteRoute(1L));
    }

    @DeleteMapping("/{id}/favorites/{routeId}")
    public ResponseEntity<Void> removeFavoriteRoute(@PathVariable @Min(1) Long id, @PathVariable @Min(1) Long routeId) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }
}
