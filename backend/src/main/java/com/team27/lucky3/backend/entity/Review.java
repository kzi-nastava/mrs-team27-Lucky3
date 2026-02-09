package com.team27.lucky3.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int driverRating;
    private int vehicleRating;

    private String comment;
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = true)
    private User passenger;

    /** Email of the reviewer when they are a linked (non-registered) passenger. */
    @Column(name = "reviewer_email")
    private String reviewerEmail;
}