package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.PassengerRegistrationRequest;
import com.team27.lucky3.backend.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {

    // 2.2.2 Registration of passanger
    @PostMapping
    public ResponseEntity<UserResponse> registerPassenger(@RequestBody PassengerRegistrationRequest request) {
        // Mock response
        UserResponse response = new UserResponse(1L, request.getName(), request.getEmail(), "PASSENGER");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}