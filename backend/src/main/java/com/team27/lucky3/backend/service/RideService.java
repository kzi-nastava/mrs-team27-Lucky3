package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.EndRideRequest;
import com.team27.lucky3.backend.dto.request.RidePanicRequest;
import com.team27.lucky3.backend.dto.request.RideStopRequest;
import com.team27.lucky3.backend.dto.response.RideCreated;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.dto.request.InconsistencyRequest;

public interface RideService {
    RideCreated createRide(CreateRideRequest request);
    RideResponse acceptRide(Long id);
    RideResponse startRide(Long id);
    RideResponse endRide(Long id, EndRideRequest request);
    RideResponse cancelRide(Long id, String reason);
    RideResponse stopRide(Long id, RideStopRequest request);
    RideResponse panicRide(Long id, RidePanicRequest request);
    Ride findById(Long id);
    RideEstimationResponse estimateRide(CreateRideRequest request);
    void reportInconsistency(Long rideId, InconsistencyRequest request);
}