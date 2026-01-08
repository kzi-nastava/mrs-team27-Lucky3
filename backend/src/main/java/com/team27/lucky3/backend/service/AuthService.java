package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenUtils tokenUtils;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;

    // 2.2.1 Login
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. Authenticate via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Set Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Get User
        User user = (User) authentication.getPrincipal();

        // 4. Spec 2.2.1: Drivers automatically become available upon login
        if (user.getRole() == UserRole.DRIVER) {
            user.setActive(true);
            user.setInactiveRequested(false);
            userRepository.save(user);
        }

        // 5. Generate Token
        String jwt = tokenUtils.generateToken(user);
        return new TokenResponse(jwt, jwt); // Using accessToken for refresh too in this basic example
    }

    // 2.2.1 Logout
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Spec 2.2.1: Driver specific logout logic
        if (user.getRole() == UserRole.DRIVER) {
            // Check for active rides
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    user.getId(),
                    List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS)
            );

            if (hasActiveRide) {
                // Spec 2.2.1: Drivers cannot log out if they are currently driving
                throw new IllegalStateException("Cannot logout while having an active ride.");
            }

            // Spec 2.2.1: Log out, unavailability
            user.setActive(false);
            userRepository.save(user);
        }

        // Clear context
        SecurityContextHolder.clearContext();
    }
}