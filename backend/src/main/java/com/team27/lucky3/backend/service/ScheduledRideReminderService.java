package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduled service that sends ride reminders 15 minutes before scheduled start time.
 * <p>
 * Runs every minute and checks for rides that:
 * <ul>
 *   <li>Are in SCHEDULED, PENDING, or ACCEPTED status</li>
 *   <li>Have a scheduledTime between 14-16 minutes from now (to account for timing variations)</li>
 *   <li>Haven't already been reminded (tracked in memory)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledRideReminderService {

    private final RideRepository rideRepository;
    private final NotificationService notificationService;

    // In-memory set to track rides that have already been reminded
    // In production, this could be persisted to the database
    private final Set<Long> remindedRideIds = new HashSet<>();

    /**
     * Runs every minute to check for rides needing 15-minute reminders.
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional(readOnly = true)
    public void sendScheduledRideReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusMinutes(14);
        LocalDateTime windowEnd = now.plusMinutes(16);

        // Find rides with scheduledTime in the 14-16 minute window
        List<Ride> upcomingRides = rideRepository.findByScheduledTimeBetweenAndStatusIn(
                windowStart,
                windowEnd,
                List.of(RideStatus.SCHEDULED, RideStatus.PENDING, RideStatus.ACCEPTED)
        );

        for (Ride ride : upcomingRides) {
            // Skip if already reminded
            if (remindedRideIds.contains(ride.getId())) {
                continue;
            }

            try {
                notificationService.sendScheduledRideReminder(ride);
                remindedRideIds.add(ride.getId());
                log.info("Sent 15-minute reminder for ride #{}", ride.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for ride #{}: {}", ride.getId(), e.getMessage());
            }
        }

        // Cleanup old reminder IDs (rides older than 1 hour ago) to prevent memory leaks
        cleanupOldReminderIds();
    }

    /**
     * Cleans up reminded ride IDs for rides that have likely completed or been cancelled.
     * Runs as part of the scheduled task.
     */
    private void cleanupOldReminderIds() {
        if (remindedRideIds.size() > 1000) {
            // Simple cleanup: just clear old entries periodically
            // In production, you'd query the database to check ride status
            log.info("Cleaning up {} reminded ride IDs", remindedRideIds.size());
            remindedRideIds.clear();
        }
    }
}
