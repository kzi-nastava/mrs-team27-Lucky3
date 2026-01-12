package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.*;
import com.team27.lucky3.backend.entity.Panic;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.repository.PanicRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.team27.lucky3.backend.service.PanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PanicServiceImpl implements PanicService {

    private final PanicRepository panicRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PanicResponse> findAll(Pageable pageable) {
        Page<Panic> panics = panicRepository.findAll(pageable);
        return panics.map(this::mapToResponse);
    }

    private PanicResponse mapToResponse(Panic panic) {
        PanicResponse response = new PanicResponse();
        response.setId(panic.getId());
        response.setTime(panic.getTimestamp());
        response.setReason(panic.getReason());

        if (panic.getUser() != null) {
            response.setUser(new UserResponse(
                    panic.getUser().getId(),
                    panic.getUser().getName(),
                    panic.getUser().getSurname(),
                    panic.getUser().getEmail(),
                    "/api/users/" + panic.getUser().getId() + "/profile-image",
                    panic.getUser().getRole(),
                    panic.getUser().getPhoneNumber(),
                    panic.getUser().getAddress()
            ));
        }

        if (panic.getRide() != null) {
            response.setRide(mapRideToResponse(panic.getRide()));
        }

        return response;
    }

    // Independent mapper to avoid circular dependency with RideService
    private RideResponse mapRideToResponse(Ride ride) {
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

        if (ride.getStartLocation() != null) {
            res.setDeparture(new LocationDto(ride.getStartLocation().getAddress(), ride.getStartLocation().getLatitude(), ride.getStartLocation().getLongitude()));
        }
        if (ride.getEndLocation() != null) {
            res.setDestination(new LocationDto(ride.getEndLocation().getAddress(), ride.getEndLocation().getLatitude(), ride.getEndLocation().getLongitude()));
        }
        res.setScheduledTime(ride.getScheduledTime());

        if (ride.getDriver() != null) {
            Vehicle vehicle = vehicleRepository.findByDriverId(ride.getDriver().getId()).orElse(null);
            VehicleInformation vInfo = null;
            if(vehicle != null) {
                vInfo = new VehicleInformation(vehicle.getModel(), vehicle.getVehicleType(), vehicle.getLicensePlates(), vehicle.getSeatCount(), vehicle.isBabyTransport(), vehicle.isPetTransport(), ride.getDriver().getId());
            }
            DriverResponse dr = new DriverResponse(ride.getDriver().getId(), ride.getDriver().getName(), ride.getDriver().getSurname(), ride.getDriver().getEmail(), "/api/users/" + ride.getDriver().getId() + "/profile-image", ride.getDriver().getRole(), ride.getDriver().getPhoneNumber(), ride.getDriver().getAddress(), vInfo, ride.getDriver().isActive(), "0h 0m");
            res.setDriver(dr);
        }

        if (ride.getPassengers() != null) {
            List<UserResponse> passengers = ride.getPassengers().stream()
                    .map(p -> new UserResponse(p.getId(), p.getName(), p.getSurname(), p.getEmail(), "/api/users/" + p.getId() + "/profile-image", p.getRole(), p.getPhoneNumber(), p.getAddress()))
                    .collect(Collectors.toList());
            res.setPassengers(passengers);
        }
        return res;
    }
}