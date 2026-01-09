package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.EndRideRequest;
import com.team27.lucky3.backend.dto.request.RidePanicRequest;
import com.team27.lucky3.backend.dto.request.RideStopRequest;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.Ride;

public interface RideService {
    RideResponse createRide(CreateRideRequest request);
    RideResponse acceptRide(Long id);
    RideResponse startRide(Long id);
    RideResponse endRide(Long id, EndRideRequest request);
    RideResponse cancelRide(Long id, String reason);
    RideResponse stopRide(Long id, RideStopRequest request);
    RideResponse panicRide(Long id, RidePanicRequest request);
    Ride findById(Long id);
}