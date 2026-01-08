package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            driver.setActive(true);
            driver.setInactiveRequested(false);
        } else {
            // Turning OFF
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    driverId, List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS));

            if (hasActiveRide) {
                // Spec: If a driver changes status to inactive during ride, become inactive after it
                driver.setInactiveRequested(true);
            } else {
                driver.setActive(false);
            }
        }
        return userRepository.save(driver);
    }
}