package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.FavoriteRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Long> {
    List<FavoriteRoute> findByUserId(Long userId);
    //prevent adding same route multiple times
    boolean existsByUserIdAndStartLocationAddressIgnoreCaseAndEndLocationAddressIgnoreCase(
            Long userId,
            String startAddress,
            String endAddress
    );
    Optional<FavoriteRoute> findByIdAndUserId(Long id, Long userId); // userId maps to favorite.user.id
}
