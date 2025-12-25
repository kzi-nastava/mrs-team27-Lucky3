package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.PanicRequest;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {
    // 2.1.2 Estimation of ride
    @GetMapping("/estimation")
    public ResponseEntity<RideEstimationResponse> estimateRide(
            @RequestParam String departure,
            @RequestParam String destination) {

        // Mock
        RideEstimationResponse response = new RideEstimationResponse(15, 650.00);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 2.5 Cancel ride
    @PutMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable Long id) {
        // Mock
        RideResponse response = new RideResponse(id, "CANCELLED", 0.0);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 2.6 Panic ride
    @PutMapping("/{id}/panic")
    public ResponseEntity<RideResponse> panicRide(@PathVariable Long id, @RequestBody PanicRequest request) {
        // There we would save request.getReason() to the database
        RideResponse response = new RideResponse(id, "PANIC", 450.0);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
