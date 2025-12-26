package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.request.PasswordResetRequest;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class UserController {

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        TokenResponse response = new TokenResponse("jwt-access-token", "jwt-refresh-token");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> sendResetPasswordEmail(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@PathVariable @Min(1) Long id, @RequestBody PasswordResetRequest resetRequest) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }
}
