package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;

    // 2.2.1 Manual Status Toggle
    @Transactional
    public User toggleActivity(Long driverId, boolean targetStatus) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (targetStatus) {
            // Turning ON
            driver.setActive(true);
            driver.setInactiveRequested(false);
        } else {
            // Turning OFF [cite: 44]
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    driverId, List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS));

            if (hasActiveRide) {
                // Spec: "If driver changes status to inactive during ride, become inactive after it"
                driver.setInactiveRequested(true);
                // Note: Actual switch to inactive happens in RideService.endRide()
            } else {
                driver.setActive(false);
            }
        }
        return userRepository.save(driver);
    }
}