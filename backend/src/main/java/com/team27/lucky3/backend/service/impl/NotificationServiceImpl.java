package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendRideFinishedNotification(Ride ride) {
        log.info("Sending notification for finished ride ID: {}", ride.getId());
        // Logic to send email or push notification would go here
    }
}
