package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.request.PassengerRegistrationRequest;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.entity.ActivationToken;
import com.team27.lucky3.backend.entity.PasswordResetToken;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.PasswordResetTokenRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.ActivationTokenRepository;
import com.team27.lucky3.backend.service.AuthService;
import com.team27.lucky3.backend.service.DriverService;
import com.team27.lucky3.backend.service.EmailService;
import com.team27.lucky3.backend.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenUtils tokenUtils;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DriverService driverService;
    private final ActivationTokenRepository activationTokenRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. Authenticate via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Set Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Get User (Principal)
        User user = (User) authentication.getPrincipal();

        // 4. Spec 2.2.1: Drivers automatically become available upon login
        if (user.getRole() == UserRole.DRIVER) {
            // START CHANGE
            user.setActive(!driverService.hasExceededWorkingHours(user.getId())); // Force inactive if over limit
            // END CHANGE
            user.setInactiveRequested(false);
            userRepository.save(user);
        }

        // 5. Generate Token
        String jwt = tokenUtils.generateToken(user);
        return new TokenResponse(jwt, jwt);
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Spec 2.2.1: Driver specific logout logic
        if (user.getRole() == UserRole.DRIVER) {
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    user.getId(),
                    List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS)
            );

            if (hasActiveRide) {
                throw new IllegalStateException("Cannot logout while having an active ride.");
            }

            user.setActive(false);
            userRepository.save(user);
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);


        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.sendSimpleMessage(email, "Reset Password", "Click here to reset: " + link);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }

    @Override
    @Transactional
    public User registerPassenger(PassengerRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(request.getAddress());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(UserRole.PASSENGER);
        user.setBlocked(false);
        user.setEnabled(false); // Cannot login until activated

        // Default image if empty
        if (request.getProfilePictureUrl() == null || request.getProfilePictureUrl().isBlank()) {
            user.setProfilePictureUrl("assets/default.png");
        } else {
            user.setProfilePictureUrl(request.getProfilePictureUrl()); // Assuming you add this field to DTO
        }

        User savedUser = userRepository.save(user);

        // Generate Activation Token
        String token = UUID.randomUUID().toString();
        ActivationToken activationToken = new ActivationToken(token, savedUser);
        activationTokenRepository.save(activationToken);

        // Send Email
        String link = frontendUrl + "/activate?token=" + token;
        emailService.sendSimpleMessage(
                savedUser.getEmail(),
                "Activate your Account",
                "Welcome! Please click here to activate your account: " + link
        );

        return savedUser;
    }

    @Override
    @Transactional
    public void activateAccount(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid activation token"));

        if (activationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Activation token expired");
        }

        User user = activationToken.getUser();
        user.setEnabled(true); // Enable login
        userRepository.save(user);

        activationTokenRepository.delete(activationToken);
    }
}