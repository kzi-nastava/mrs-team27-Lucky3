package com.team27.lucky3.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks driver online activity sessions.
 * A session starts when driver goes online and ends when they go offline.
 */
@Entity
@Table(name = "driver_activity_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverActivitySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
