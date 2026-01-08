package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;

    @Override
    @Transactional
    public User toggleActivity(Long driverId, boolean targetStatus) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (targetStatus) {
            // Turning ON
            // Spec 2.5/2.2.1: Check working hours before enabling
            if (hasExceededWorkingHours(driverId)) {
                throw new IllegalStateException("Cannot go active: 8-hour working limit exceeded.");
            }
            driver.setActive(true);
            driver.setInactiveRequested(false);
        } else {
            // Turning OFF
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    driverId, List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS));

            if (hasActiveRide) {
                // Spec 2.2.1: If inactive requested during the ride, defer it
                driver.setInactiveRequested(true);
            } else {
                driver.setActive(false);
            }
        }
        return userRepository.save(driver);
    }

    @Override
    public boolean hasExceededWorkingHours(Long driverId) {
        // Spec 2.5: Driver becomes unavailable if working hours > 8h in the day
        // We calculate this by summing the duration of rides finished in the last 24h
        LocalDateTime startOfDay = LocalDateTime.now().minusHours(24);

        List<Ride> recentRides = rideRepository.findFinishedRidesSince(driverId, startOfDay);

        long totalSeconds = 0;
        for (Ride ride : recentRides) {
            if (ride.getStartTime() != null && ride.getEndTime() != null) {
                totalSeconds += Duration.between(ride.getStartTime(), ride.getEndTime()).getSeconds();
            }
        }

        // 8 hours = 8 * 60 * 60 = 28800 seconds
        return totalSeconds > 28800;
    }
}