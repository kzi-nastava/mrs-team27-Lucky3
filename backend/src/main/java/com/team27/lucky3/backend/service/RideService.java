package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.FavoriteRouteResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RideService {
    RideResponse createRide(CreateRideRequest request);
    RideResponse acceptRide(Long id);
    RideResponse startRide(Long id);
    RideResponse endRide(Long id, EndRideRequest request);
    RideResponse cancelRide(Long id, String reason);
    RideResponse stopRide(Long id, RideStopRequest request);
    RideResponse panicRide(Long id, RidePanicRequest request);
    RideResponse completeStop(Long rideId, Integer stopIndex);
    Ride findById(Long id);
    RideResponse getRideDetails(Long id);
    org.springframework.data.domain.Page<RideResponse> getRidesHistory(
            org.springframework.data.domain.Pageable pageable,
            java.time.LocalDateTime fromDate,
            java.time.LocalDateTime toDate,
            Long driverId,
            Long passengerId,
            String status
    );
    RideEstimationResponse estimateRide(CreateRideRequest request);
    void reportInconsistency(Long rideId, InconsistencyRequest request);
    RideResponse getActiveRide(Long userId);
    void addToFavorite(Long userId, FavouriteRouteRequest request);
    void removeFromFavorite(Long userId, Long favouriteId);
    List<FavoriteRouteResponse> getFavoriteRoutes(Long userId);
}