package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.FavoriteRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Long> {
    List<FavoriteRoute> findByUserId(Long userId);
    //prevent adding same route multiple times
    boolean existsByUserIdAndStartLocationAddressIgnoreCaseAndEndLocationAddressIgnoreCase(
            Long userId,
            String startAddress,
            String endAddress
    );
}
