package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.RideTrackingToken;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.repository.RideTrackingTokenRepository;
import com.team27.lucky3.backend.service.RideService;
import com.team27.lucky3.backend.util.RideTrackingTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ride-tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ride Tracking", description = "Token-based ride tracking for linked passengers (public, no auth required)")
public class RideTrackingController {

    private final RideTrackingTokenRepository trackingTokenRepository;
    private final RideTrackingTokenUtils trackingTokenUtils;
    private final RideService rideService;

    // Trackable statuses - token is valid only while ride is in these states
    private static final Set<RideStatus> TRACKABLE_STATUSES = Set.of(
            RideStatus.PENDING,
            RideStatus.SCHEDULED,
            RideStatus.ACCEPTED,
            RideStatus.IN_PROGRESS,
            RideStatus.ACTIVE
    );

    @Operation(summary = "Validate tracking token", description = "Check if a ride tracking token is valid and ride is trackable (public)", security = {})
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            // 1. Check JWT structure
            if (!trackingTokenUtils.isTokenStructureValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "reason", "Invalid or expired token"));
            }

            Long rideId = trackingTokenUtils.getRideIdFromToken(token);
            String email = trackingTokenUtils.getEmailFromToken(token);

            if (rideId == null || email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "reason", "Invalid token data"));
            }

            // 2. Check if token exists in database and is not revoked
            RideTrackingToken trackingToken = trackingTokenRepository.findByToken(token)
                    .orElse(null);

            if (trackingToken == null || trackingToken.isRevoked()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "reason", "Token has been revoked or does not exist"));
            }

            // 3. Check ride status
            Ride ride = trackingToken.getRide();
            if (ride == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("valid", false, "reason", "Ride not found"));
            }

            if (!TRACKABLE_STATUSES.contains(ride.getStatus())) {
                String statusMessage = getStatusMessage(ride.getStatus());
                return ResponseEntity.ok(Map.of(
                        "valid", false, 
                        "reason", statusMessage,
                        "rideId", rideId,
                        "status", ride.getStatus().name()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "rideId", rideId,
                    "email", email,
                    "status", ride.getStatus().name()
            ));

        } catch (Exception e) {
            log.error("Error validating tracking token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("valid", false, "reason", "Error validating token"));
        }
    }

    @Operation(summary = "Get ride by tracking token", description = "Retrieve ride details for tracking (public, read-only)", security = {})
    @GetMapping("/ride")
    public ResponseEntity<?> getRideByToken(@RequestParam String token) {
        try {
            // 1. Validate token structure
            if (!trackingTokenUtils.isTokenStructureValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }

            Long rideId = trackingTokenUtils.getRideIdFromToken(token);
            if (rideId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token data"));
            }

            // 2. Check token in database
            RideTrackingToken trackingToken = trackingTokenRepository.findByToken(token)
                    .orElse(null);

            if (trackingToken == null || trackingToken.isRevoked()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token has been revoked"));
            }

            // 3. Get ride data
            Ride ride = trackingToken.getRide();
            if (ride == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Ride not found"));
            }

            // 4. Check if ride is still trackable
            if (!TRACKABLE_STATUSES.contains(ride.getStatus())) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(Map.of(
                                "error", "Ride is no longer trackable",
                                "status", ride.getStatus().name(),
                                "message", getStatusMessage(ride.getStatus())
                        ));
            }

            // 5. Return ride response
            RideResponse rideResponse = rideService.getRideDetails(rideId);
            return ResponseEntity.ok(rideResponse);

        } catch (Exception e) {
            log.error("Error getting ride by tracking token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving ride data"));
        }
    }

    private String getStatusMessage(RideStatus status) {
        return switch (status) {
            case FINISHED -> "This ride has been completed";
            case CANCELLED, CANCELLED_BY_DRIVER, CANCELLED_BY_PASSENGER -> "This ride has been cancelled";
            case REJECTED -> "This ride was rejected";
            case PANIC -> "This ride was ended due to a safety concern";
            default -> "This ride is no longer available for tracking";
        };
    }
}
