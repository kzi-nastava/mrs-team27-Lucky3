package com.team27.lucky3.backend.entity;

import com.team27.lucky3.backend.entity.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private Double baseFare;

    @Column(nullable = false)
    private Double pricePerKm;
}