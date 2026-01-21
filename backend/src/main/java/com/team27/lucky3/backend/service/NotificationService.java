package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.Ride;

public interface NotificationService {
    void sendRideFinishedNotification(Ride ride);
}
