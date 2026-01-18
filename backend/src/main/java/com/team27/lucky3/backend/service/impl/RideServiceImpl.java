package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.*;
import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Panic;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.PanicRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.team27.lucky3.backend.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.team27.lucky3.backend.entity.enums.VehicleType.VAN;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final PanicRepository panicRepository;
    private final VehicleRepository vehicleRepository;

    private static final double PRICE_PER_KM = 120.0;
    private static final double AVG_SPEED_KMH = 50.0; // Average speed in city
    private static final double AVG_SPEED_KMM = AVG_SPEED_KMH / 60.0; // km per minute (~0.83)

    @Override
    public RideEstimationResponse estimateRide(CreateRideRequest request) {
        Location start = mapLocation(request.getStart());
        Location end = mapLocation(request.getDestination());
        VehicleType type = request.getRequirements() != null ? request.getRequirements().getVehicleType() : VehicleType.STANDARD;

        // Calculate Ride Details (A -> B)
        double distanceKm = calculateHaversineDistance(start, end);
        double basePrice = getBasePriceForVehicle(type);

        double estimatedCost = Math.round((basePrice + (distanceKm * PRICE_PER_KM)) * 100.0) / 100.0;
        int estimatedRideTime = (int) Math.ceil(distanceKm / AVG_SPEED_KMM);

        // Find Closest Active Driver
        List<Vehicle> activeVehicles = vehicleRepository.findAllActiveVehicles();

        List<Vehicle> candidateVehicles = activeVehicles.stream()
                .filter(v -> v.getVehicleType() == type || type == null)
                .toList();

        int timeToPickup = -1; // -1 indicates no drivers available

        if (!candidateVehicles.isEmpty()) {
            double minDistanceToUser = Double.MAX_VALUE;

            for (Vehicle v : candidateVehicles) {
                if (v.getCurrentLocation() != null) {
                    double dist = calculateHaversineDistance(start, v.getCurrentLocation());
                    if (dist < minDistanceToUser) {
                        minDistanceToUser = dist;
                    }
                }
            }
            if (minDistanceToUser != Double.MAX_VALUE) {
                timeToPickup = (int) Math.ceil(minDistanceToUser / AVG_SPEED_KMM);
            }
        }

        List<RoutePointResponse> routePoints = new ArrayList<>();
        routePoints.add(new RoutePointResponse(request.getStart(), 1));
        routePoints.add(new RoutePointResponse(request.getDestination(), 2));

        return new RideEstimationResponse(estimatedRideTime, estimatedCost, timeToPickup, routePoints);
    }

    @Override
    @Transactional
    public RideResponse createRide(CreateRideRequest request) {
        User passenger = getCurrentUser();
        if (passenger == null) {
            throw new IllegalStateException("User must be logged in to create a ride");
        }

        Ride ride = new Ride();

        ride.setStartTime(null); // Will be set when accepted/started
        ride.setScheduledTime(request.getScheduledTime());
        ride.setStartLocation(mapLocation(request.getStart()));
        ride.setEndLocation(mapLocation(request.getDestination()));

        if (request.getStops() != null) {
            ride.setStops(request.getStops().stream()
                    .map(this::mapLocation)
                    .collect(Collectors.toList()));
        }

        ride.setPassengers(Set.of(passenger));
        ride.setStatus(RideStatus.PENDING);
        ride.setRequestedVehicleType(request.getRequirements().getVehicleType());
        ride.setBabyTransport(request.getRequirements().isBabyTransport());
        ride.setPetTransport(request.getRequirements().isPetTransport());
        ride.setEstimatedCost(450.0); // Dummy estimation
        ride.setDistance(3.5); // Dummy distance
        ride.setInvitedEmails(request.getPassengerEmails());

        Ride savedRide = rideRepository.save(ride);
        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse acceptRide(Long id) {
        Ride ride = findById(id);
        User driver = getCurrentUser(); // Driver accepting the ride

        if (ride.getStatus() != RideStatus.PENDING) {
            throw new IllegalStateException("Ride is not pending");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        Ride savedRide = rideRepository.save(ride);
        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse startRide(Long id) {
        Ride ride = findById(id);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException("Ride must be accepted before starting");
        }

        ride.setStartTime(LocalDateTime.now());
        ride.setStatus(RideStatus.IN_PROGRESS); // or ACTIVE based on enum
        Ride savedRide = rideRepository.save(ride);
        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse endRide(Long id, EndRideRequest request) {
        Ride ride = findById(id);

        if (ride.getStatus() != RideStatus.IN_PROGRESS && ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Ride is not in progress");
        }

        ride.setEndTime(LocalDateTime.now());
        ride.setStatus(RideStatus.FINISHED);
        ride.setPaid(request.getPaid());
        ride.setPassengersExited(request.getPassengersExited());

        Ride savedRide = rideRepository.save(ride);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse cancelRide(Long id, String reason) {
        Ride ride = findById(id);
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to cancel a ride.");
        }

        // Only cancellable in certain states
        if (ride.getStatus() == RideStatus.FINISHED || ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.PANIC) {
            throw new IllegalStateException("Ride already finished/cancelled");
        }

        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(currentUser.getId());
        boolean isPassenger = ride.getPassengers().contains(currentUser);

        if (isDriver) {
            if (ride.getStatus() == RideStatus.IN_PROGRESS || ride.getStatus() == RideStatus.ACTIVE) {
                throw new IllegalStateException("Driver cannot cancel ride after it has started (passengers entered).");
            }
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalStateException("Driver must provide a reason for cancellation.");
            }
        } else if (isPassenger) {
            if (ride.getScheduledTime() != null) {
                LocalDateTime limit = ride.getScheduledTime().minusMinutes(10);
                if (LocalDateTime.now().isAfter(limit)) {
                    throw new IllegalStateException("You cannot cancel a scheduled ride within 10 minutes of its start time.");
                }
            }

            if (ride.getStatus() == RideStatus.IN_PROGRESS) {
                throw new IllegalStateException("Cannot cancel an active ride. Request a stop instead.");
            }
        } else {
            throw new IllegalStateException("User is not authorized to cancel this ride.");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setRejectionReason(reason);
        ride.setEndTime(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    /* TODO: Check what is better between this and down same function
    @Override
    @Transactional
    public RideResponse panicRide(Long id, RidePanicRequest request) {
        Ride ride = findById(id);
        User user = getCurrentUser();

        ride.setStatus(RideStatus.PANIC);
        ride.setPanicPressed(true);
        ride.setEndTime(LocalDateTime.now());

        Panic panic = new Panic();
        panic.setRide(ride);
        panic.setUser(user);
        panic.setReason(request.getReason());
        panic.setTimestamp(LocalDateTime.now());
        panicRepository.save(panic);

        Ride savedRide = rideRepository.save(ride);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }*/

    @Override
    public Ride findById(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }

    private void checkAndHandleInactiveRequest(User driver) {
        if (driver != null && driver.isInactiveRequested()) {
            driver.setActive(false);
            driver.setInactiveRequested(false);
            userRepository.save(driver);
        }
    }

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        // Fallback for tests or if principal is not User object (unlikely in this setup)
        return null;
    }

    private Location mapLocation(LocationDto dto) {
        if (dto == null) return null;
        return new Location(dto.getAddress(), dto.getLatitude(), dto.getLongitude());
    }

    // Manual mapping for now
    private RideResponse mapToResponse(Ride ride) {
        RideResponse res = new RideResponse();
        res.setId(ride.getId());
        res.setStartTime(ride.getStartTime());
        res.setEndTime(ride.getEndTime());
        res.setTotalCost(ride.getTotalCost());
        res.setEstimatedCost(ride.getEstimatedCost());
        res.setDistanceKm(ride.getDistance());
        res.setStatus(ride.getStatus());
        res.setPanicPressed(Boolean.TRUE.equals(ride.getPanicPressed()));
        res.setRejectionReason(ride.getRejectionReason());
        res.setPetTransport(ride.isPetTransport());
        res.setBabyTransport(ride.isBabyTransport());
        res.setVehicleType(ride.getRequestedVehicleType());
        res.setPaid(ride.isPaid());
        res.setPassengersExited(ride.isPassengersExited());

        if (ride.getStartLocation() != null) {
            res.setDeparture(new LocationDto(ride.getStartLocation().getAddress(), ride.getStartLocation().getLatitude(), ride.getStartLocation().getLongitude()));
        }
        if (ride.getEndLocation() != null) {
            res.setDestination(new LocationDto(ride.getEndLocation().getAddress(), ride.getEndLocation().getLatitude(), ride.getEndLocation().getLongitude()));
        }
        res.setScheduledTime(ride.getScheduledTime());

        if (ride.getDriver() != null) {
            // Minimal driver info
            Vehicle vehicle = vehicleRepository.findByDriverId(ride.getDriver().getId()).orElse(null);
            VehicleInformation vInfo = null;
            String model = null;
            String plates = null;
            if(vehicle != null) {
                 vInfo = new VehicleInformation(vehicle.getModel(), vehicle.getVehicleType(), vehicle.getLicensePlates(), vehicle.getSeatCount(), vehicle.isBabyTransport(), vehicle.isPetTransport(), ride.getDriver().getId());
                 model = vehicle.getModel();
                 plates = vehicle.getLicensePlates();
            }
            // Logic to get driver status/time would be complex, simplifying here
            DriverResponse dr = new DriverResponse(ride.getDriver().getId(), ride.getDriver().getName(), ride.getDriver().getSurname(), ride.getDriver().getEmail(), "/api/users/"+ride.getDriver().getId()+"/profile-image", ride.getDriver().getRole(), ride.getDriver().getPhoneNumber(), ride.getDriver().getAddress(), vInfo, ride.getDriver().isActive(), "0h 0m");
            res.setDriver(dr);
            res.setModel(model);
            res.setLicensePlates(plates);
        }

        if (ride.getPassengers() != null) {
             List<UserResponse> passengers = ride.getPassengers().stream().map(p -> new UserResponse(p.getId(), p.getName(), p.getSurname(), p.getEmail(), "/api/users/"+p.getId()+"/profile-image", p.getRole(), p.getPhoneNumber(), p.getAddress())).collect(Collectors.toList());
             res.setPassengers(passengers);
        }

        return res;
    }

    @Override
    @Transactional
    public RideResponse stopRide(Long id, RideStopRequest request) {
        Ride ride = findById(id);
        User currentUser = getCurrentUser();

        // Validate User (Must be driver)
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated.");
        }
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned driver can stop the ride.");
        }

        // Validate Status
        if (ride.getStatus() != RideStatus.IN_PROGRESS && ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Ride cannot be stopped if it is not in progress.");
        }

        // Update Ride Data
        Location newEndLocation = mapLocation(request.getStopLocation());
        ride.setEndLocation(newEndLocation);
        ride.setEndTime(LocalDateTime.now());
        ride.setStatus(RideStatus.FINISHED);
        ride.setPassengersExited(true);
        ride.setPaid(true);


        double distanceKm = calculateHaversineDistance(
                ride.getStartLocation(),
                newEndLocation
        );
        ride.setDistance(distanceKm);

        double basePrice = getBasePriceForVehicle(ride.getRequestedVehicleType());
        double newPrice = basePrice + (distanceKm * 120.0);
        ride.setTotalCost(Math.round(newPrice * 100.0) / 100.0); // Round to 2 decimals

        Ride savedRide = rideRepository.save(ride);

        // Handle Driver Inactivity
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse panicRide(Long id, RidePanicRequest request) {
        Ride ride = findById(id);
        User currentUser = getCurrentUser(); // The user pressing the button

        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated.");
        }

        // Validate that the ride is actually active/in-progress
        if (ride.getStatus() == RideStatus.FINISHED ||
                ride.getStatus() == RideStatus.CANCELLED ||
                ride.getStatus() == RideStatus.PANIC) {
            throw new IllegalStateException("Ride is already finished or cancelled.");
        }

        // Validate that the user is actually part of this ride
        boolean isPassenger = ride.getPassengers().contains(currentUser);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(currentUser.getId());

        if (!isPassenger && !isDriver) {
            throw new IllegalStateException("Only a participant of the ride can activate panic.");
        }

        // Update Ride Status
        ride.setStatus(RideStatus.PANIC);
        ride.setPanicPressed(true);
        ride.setEndTime(LocalDateTime.now());

        // Create Panic Record
        Panic panic = new Panic();
        panic.setRide(ride);
        panic.setUser(currentUser);
        panic.setReason(request.getReason());
        panic.setTimestamp(LocalDateTime.now());
        panicRepository.save(panic);

        // Save Ride
        Ride savedRide = rideRepository.save(ride);

        // Handle Driver Inactivity (If driver pressed it or is affected)
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    private double calculateHaversineDistance(Location start, Location end) {
        if (start == null || end == null) return 0.0;
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDistance = Math.toRadians(end.getLongitude() - start.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double getBasePriceForVehicle(VehicleType type) {
        if (type == null) return 120.0;
        return switch (type) {
            case LUXURY -> 200.0;
            case VAN -> 180.0;
            default -> 120.0;
        };
    }
}
