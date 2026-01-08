package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.EndRideRequest;
import com.team27.lucky3.backend.dto.request.RidePanicRequest;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Panic;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.RideStatus;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final PanicRepository panicRepository;
    private final VehicleRepository vehicleRepository;

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

        // Only cancellable in certain states
        if (ride.getStatus() == RideStatus.FINISHED || ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.PANIC) {
            throw new IllegalStateException("Ride already finished/cancelled");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setRejectionReason(reason);
        ride.setEndTime(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

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
    }

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
            DriverResponse dr = new DriverResponse(ride.getDriver().getId(), ride.getDriver().getName(), ride.getDriver().getSurname(), ride.getDriver().getEmail(), ride.getDriver().getProfilePictureUrl(), ride.getDriver().getRole(), ride.getDriver().getPhoneNumber(), ride.getDriver().getAddress(), vInfo, ride.getDriver().isActive(), "0h 0m");
            res.setDriver(dr);
            res.setModel(model);
            res.setLicensePlates(plates);
        }

        if (ride.getPassengers() != null) {
             List<UserResponse> passengers = ride.getPassengers().stream().map(p -> new UserResponse(p.getId(), p.getName(), p.getSurname(), p.getEmail(), p.getProfilePictureUrl(), p.getRole(), p.getPhoneNumber(), p.getAddress())).collect(Collectors.toList());
             res.setPassengers(passengers);
        }

        return res;
    }
}
