package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.EmailRequest;
import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.request.PasswordResetRequest;
import com.team27.lucky3.backend.dto.request.SetInitialPassword;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // 2.2.1 - Login
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {

        // Later there will be a call to the authService.login(loginRequest);
        // Mock response

        TokenResponse response = new TokenResponse("jwt-access-token", "jwt-refresh-token");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> sendResetPasswordEmail(@RequestBody EmailRequest emailRequest) {
        // Mock: Send email logic
        return new ResponseEntity<>("Email with reset code sent.", HttpStatus.NO_CONTENT);
    }

    @PutMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest resetRequest) {
        // Mock: Change password logic
        return new ResponseEntity<>("Password successfully changed.", HttpStatus.NO_CONTENT);
    }

    @PostMapping("/driver-activation/password")
    public ResponseEntity<String> setInitialPassword(@RequestBody SetInitialPassword initialPassword) {
        // Mock: Set initial password logic
        /*
            nađe token (po hash-u) nisam siguran da li cemo koristiti hash, ali otprilike
            proveri expiresAt i usedAt  - kod tokena koji dobije
            upiše passwordHash (bcrypt) u Driver - updatuje drivera, nadje ga po id-u iz tokena
            setuje driver ACTIVE - updatuje drivera, nadje ga po id-u iz tokena
            markira token usedAt=now (link postaje nevažeći)
         */

        System.out.println("Initial Password: " + initialPassword);
        return new ResponseEntity<>("Initial password successfully set.", HttpStatus.NO_CONTENT);
    }

}
