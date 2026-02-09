package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.RideTrackingToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideTrackingTokenRepository extends JpaRepository<RideTrackingToken, Long> {

    Optional<RideTrackingToken> findByToken(String token);

    Optional<RideTrackingToken> findByRideIdAndEmail(Long rideId, String email);

    List<RideTrackingToken> findAllByRideId(Long rideId);

    @Modifying
    @Query("UPDATE RideTrackingToken t SET t.revoked = true WHERE t.ride.id = :rideId")
    int revokeAllByRideId(@Param("rideId") Long rideId);

    boolean existsByRideIdAndEmail(Long rideId, String email);
}
