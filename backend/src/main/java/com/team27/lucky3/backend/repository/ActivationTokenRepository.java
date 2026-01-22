package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.ActivationToken;
import com.team27.lucky3.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByToken(String token);
    Optional<ActivationToken> findByUser(User user);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}