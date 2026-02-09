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
public class Panic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String reason;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Who pressed the panic button

    @ManyToOne
    @JoinColumn(name = "ride_id")
    private Ride ride;
}