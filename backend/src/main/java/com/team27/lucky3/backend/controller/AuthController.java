package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AuthController {

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        TokenResponse response = new TokenResponse("jwt-access-token", "jwt-refresh-token");
        return ResponseEntity.ok(response);
    }

    // 2.2.2 User registration + email activation (unregistered -> registered user)
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody PassengerRegistrationRequest request) {
        UserResponse response = new UserResponse(1L, request.getName(), request.getSurname(), request.getEmail(), "default.png", UserRole.PASSENGER, request.getPhoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2.2.2 User registration + email activation
    @GetMapping("/activate/{token}")
    public ResponseEntity<Void> activateAccount(@PathVariable @NotBlank String token) {
        // Activation logic using token
        return ResponseEntity.ok().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest emailRequest) {
        // Send email logic
        return ResponseEntity.noContent().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PutMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        // Change password logic
        return ResponseEntity.noContent().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // Logout logic: invalidate token
        return ResponseEntity.noContent().build();
    }

    // 2.2.3 Admin creates driver accounts + vehicle info + password setup via email link (admin, driver)
    @PostMapping(value = "/driver-activation/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setInitialPassword(@Valid @RequestBody SetInitialPassword initialPassword) {
        // Set initial password logic
        return ResponseEntity.noContent().build();
    }
}

