package com.team27.lucky3.backend.entity;

import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime scheduledTime;
    private Double totalCost;
    private Double estimatedCost;
    private Double distance;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    private Boolean panicPressed;
    private String rejectionReason;

    private boolean petTransport;
    private boolean babyTransport;

    @Enumerated(EnumType.STRING)
    private VehicleType requestedVehicleType;

    private boolean paid;
    private boolean passengersExited;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "start_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "start_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "start_longitude"))
    })
    private Location startLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "end_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "end_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "end_longitude"))
    })
    private Location endLocation;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToMany
    @JoinTable(
            name = "ride_passengers",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> passengers;

    @ElementCollection
    @CollectionTable(name = "ride_invited_emails", joinColumns = @JoinColumn(name = "ride_id"))
    @Column(name = "email")
    private List<String> invitedEmails;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ride_id")
    private List<InconsistencyReport> inconsistencyReports;
}

