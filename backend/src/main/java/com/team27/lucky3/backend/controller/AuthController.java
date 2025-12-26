package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.EmailRequest;
import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.request.PasswordResetRequest;
import com.team27.lucky3.backend.dto.request.SetInitialPassword;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AuthController {

    // 2.2.1 - Login
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

        // Later there will be a call to the authService.login(loginRequest);
        // Mock response

        TokenResponse response = new TokenResponse("jwt-access-token", "jwt-refresh-token");
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendResetPasswordEmail(@Valid @RequestBody EmailRequest emailRequest) {
        // Mock: Send email logic
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        // Mock: Change password logic
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/driver-activation/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setInitialPassword(@Valid @RequestBody SetInitialPassword initialPassword) {
        // Mock: Set initial password logic
        /*
            nađe token (po hash-u) nisam siguran da li cemo koristiti hash, ali otprilike
            proveri expiresAt i usedAt  - kod tokena koji dobije
            upiše passwordHash (bcrypt) u Driver - updatuje drivera, nadje ga po id-u iz tokena
            setuje driver ACTIVE - updatuje drivera, nadje ga po id-u iz tokena
            markira token usedAt=now (link postaje nevažeći)
         */

        System.out.println("Initial Password: " + initialPassword);
        return ResponseEntity.noContent().build();
    }
}

