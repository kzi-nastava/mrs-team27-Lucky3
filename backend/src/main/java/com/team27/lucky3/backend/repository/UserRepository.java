package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    // This query finds drivers who are Active AND have NOT requested to be inactive
    @Query("SELECT u FROM User u WHERE u.role = 'DRIVER' AND u.isActive = true AND u.isInactiveRequested = false")
    List<User> findAvailableDrivers();

    List<User> findAllByRole(UserRole role);
    Optional<User> findByIdAndRole(Long id, UserRole role);

    @Query("select u from User u left join fetch u.profileImage where u.id = :id")
    Optional<User> findByIdWithProfileImage(@Param("id") Long id);
}

