package com.team27.lucky3.backend.entity;

import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    @Column(name = "panic_reason")
    private String panicReason;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    private boolean petTransport;
    private boolean babyTransport;

    @Enumerated(EnumType.STRING)
    private VehicleType requestedVehicleType;

    private boolean paid;
    private boolean passengersExited;

    // Fields for tracking vehicle location and calculating cost incrementally
    @Column(name = "last_tracked_latitude")
    private Double lastTrackedLatitude;
    
    @Column(name = "last_tracked_longitude")
    private Double lastTrackedLongitude;
    
    @Column(name = "distance_traveled")
    private Double distanceTraveled;

    // Rate snapshot: locked at ride creation so price changes don't affect existing rides
    @Column(name = "rate_base_fare")
    private Double rateBaseFare;

    @Column(name = "rate_price_per_km")
    private Double ratePricePerKm;

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

    // Added intermediate stops (Spec 2.4.1)
    @ElementCollection
    @CollectionTable(name = "ride_stops", joinColumns = @JoinColumn(name = "ride_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "stop_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "stop_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "stop_longitude"))
    })
    private List<Location> stops;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ride_route_points", joinColumns = @JoinColumn(name = "ride_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "rp_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "rp_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "rp_longitude"))
    })
    private List<Location> routePoints;

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

    @ElementCollection
    @CollectionTable(name = "ride_completed_stops", joinColumns = @JoinColumn(name = "ride_id"))
    @Column(name = "stop_index")
    private Set<Integer> completedStopIndexes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ride_id")
    private List<InconsistencyReport> inconsistencyReports;

    @OneToMany(mappedBy = "ride", fetch = FetchType.LAZY)
    private List<Review> reviews;
}

