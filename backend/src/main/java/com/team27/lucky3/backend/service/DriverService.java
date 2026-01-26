package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.dto.response.DriverStatusResponse;
import com.team27.lucky3.backend.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DriverService {
    User toggleActivity(Long driverId, boolean targetStatus);
    boolean hasExceededWorkingHours(Long driverId);
    boolean hasActiveRide(Long driverId);
    DriverStatusResponse getDriverStatus(Long driverId);
    //this is used for 2.2.3 create driver by admin
    DriverResponse createDriver(CreateDriverRequest request, MultipartFile file) throws IOException;
    DriverResponse getDriver(Long id);
    List<DriverResponse> getAllDrivers();
    void activateDriverWithPassword(String token, String password, PasswordEncoder passwordEncoder);
}