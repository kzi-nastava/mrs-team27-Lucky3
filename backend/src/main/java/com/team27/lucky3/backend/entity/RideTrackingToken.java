package com.team27.lucky3.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a token for linked passengers to track a ride.
 * These tokens allow non-authenticated or external users to view ride status
 * without full app access.
 */
@Entity
@Data
@NoArgsConstructor
public class RideTrackingToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(nullable = false)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private boolean revoked = false;

    public RideTrackingToken(String token, Ride ride, String email) {
        this.token = token;
        this.ride = ride;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        // Token doesn't expire by time - only by ride status
        this.expiresAt = null;
        this.used = false;
        this.revoked = false;
    }

    /**
     * Checks if this token is still valid for tracking.
     * Token is valid if not revoked and ride is in trackable state.
     */
    public boolean isValid() {
        return !revoked && ride != null;
    }
}
