package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.entity.User;

public interface DriverService {
    User toggleActivity(Long driverId, boolean targetStatus);
    boolean hasExceededWorkingHours(Long driverId);
    //this is used for 2.2.3 create driver by admin
    DriverResponse createDriver(CreateDriverRequest request);
    DriverResponse getDriver(Long id);
}