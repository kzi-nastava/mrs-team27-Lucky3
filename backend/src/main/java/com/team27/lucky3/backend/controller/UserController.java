package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.PasswordResetRequest;
import com.team27.lucky3.backend.dto.response.UserProfile;
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

    // 2.3 Profile page (registered user, driver, admin)
    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(DummyData.createDummyUserProfile(id));
    }

    // 2.3 Profile page (registered user, driver, admin)
    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UserProfile request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(request);
    }
    
    // 2.3 Profile page (registered user, driver, admin)
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody PasswordResetRequest request) { // Using PasswordResetRequest as placeholder or create ChangePasswordRequest
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PutMapping("/{id}/availability")
    public ResponseEntity<Void> toggleAvailability(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    // 2.4.3 Order from favorite routes (logged-in user)
    @GetMapping("/{id}/favorite-routes")
    public ResponseEntity<List<CreateRideRequest>> getFavoriteRoutes(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(new ArrayList<>());
    }

    // 2.4.3 Order from favorite routes (logged-in user)
    @PostMapping("/{id}/favorite-routes")
    public ResponseEntity<Void> addFavoriteRoute(@PathVariable @Min(1) Long id, @Valid @RequestBody CreateRideRequest route) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 2.4.3 Order from favorite routes (logged-in user)
    @DeleteMapping("/{id}/favorite-routes/{routeId}")
    public ResponseEntity<Void> removeFavoriteRoute(@PathVariable @Min(1) Long id, @PathVariable @Min(1) Long routeId) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }
}
