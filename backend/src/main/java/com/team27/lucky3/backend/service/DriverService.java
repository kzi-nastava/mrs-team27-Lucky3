package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.User;

public interface DriverService {
    User toggleActivity(Long driverId, boolean targetStatus);
}