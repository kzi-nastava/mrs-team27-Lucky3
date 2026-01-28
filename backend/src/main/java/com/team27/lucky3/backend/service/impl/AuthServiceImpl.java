package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.request.PassengerRegistrationRequest;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.entity.ActivationToken;
import com.team27.lucky3.backend.entity.DriverActivitySession;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.PasswordResetToken;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.EmailAlreadyUsedException;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.DriverActivitySessionRepository;
import com.team27.lucky3.backend.repository.PasswordResetTokenRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.ActivationTokenRepository;
import com.team27.lucky3.backend.service.AuthService;
import com.team27.lucky3.backend.service.DriverService;
import com.team27.lucky3.backend.service.EmailService;
import com.team27.lucky3.backend.service.ImageService;
import com.team27.lucky3.backend.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
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
    private final ImageService imageService;
    private final ActivationTokenRepository activationTokenRepository;
    private final DriverActivitySessionRepository activitySessionRepository;

    @Autowired
    @Lazy
    private final DriverService driverService;


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
            if (!driverService.hasExceededWorkingHours(user.getId())) {
                user.setActive(true);
                user.setInactiveRequested(false);
                userRepository.save(user);
                
                // Start a new activity session (close any orphaned one first)
                activitySessionRepository.findByDriverIdAndEndTimeIsNull(user.getId())
                        .ifPresent(session -> {
                            session.setEndTime(LocalDateTime.now());
                            activitySessionRepository.save(session);
                        });
                
                DriverActivitySession session = new DriverActivitySession();
                session.setDriver(user);
                session.setStartTime(LocalDateTime.now());
                activitySessionRepository.save(session);
            } else {
                // Force inactive if over limit
                user.setActive(false);
                user.setInactiveRequested(false);
                userRepository.save(user);
            }
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

        // Spec 2.2.1
        if (user.getRole() == UserRole.DRIVER) {

            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    user.getId(),
                    List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS)
            );
            
            boolean hasUpcomingRides = rideRepository.existsByDriverIdAndStatusIn(
                    user.getId(),
                    List.of(RideStatus.SCHEDULED, RideStatus.PENDING)
            );

            if (hasActiveRide) {
                throw new IllegalStateException("You cannot log out because you are currently on a ride.");
            }
            
            if (hasUpcomingRides) {
                throw new IllegalStateException("You cannot log out because you have scheduled rides. Complete or cancel them first.");
            }

            user.setActive(false);
            user.setInactiveRequested(false);
            userRepository.save(user);
            
            // Close any open activity session
            activitySessionRepository.findByDriverIdAndEndTimeIsNull(user.getId())
                    .ifPresent(session -> {
                        session.setEndTime(LocalDateTime.now());
                        activitySessionRepository.save(session);
                    });
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return;
        }

        // forces delete before insert, helps avoid constraint issues
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.sendSimpleMessage(email, "Reset Password", "If an account exists for this email, you’ll receive instructions shortly. " +
                "You can also open: " + link);
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
    public User registerPassenger(PassengerRegistrationRequest request, MultipartFile profileImage) throws IOException {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyUsedException("User with this email already exists.");
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

        Image image;
        if (profileImage != null && !profileImage.isEmpty()) {
            image = imageService.store(profileImage);  // snima upload
        } else {
            image = imageService.getDefaultAvatar();   // default
        }

        user.setProfileImage(image);
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

    @Override
    @Transactional
    public void resendActivationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Account is already activated");
        }

        // Ensure there's at most one activation token per user.
        // (activation_token.user_id has a unique constraint in the DB)
        activationTokenRepository.deleteByUser(user);
        activationTokenRepository.flush();

        // Generate new Activation Token
        String token = UUID.randomUUID().toString();
        ActivationToken activationToken = new ActivationToken(token, user);
        activationTokenRepository.save(activationToken);

        // Send Email
        String link = frontendUrl + "/activate?token=" + token;
        emailService.sendSimpleMessage(
                user.getEmail(),
                "Activate your Account",
                "Welcome! Please click here to activate your account: " + link
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordResetTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        return tokenRepository.findByToken(token.trim())
                .filter(t -> t.getExpiryDate() != null && t.getExpiryDate().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Override
    @Transactional
    public void activateDriverWithPassword(String token, String rawPassword) {

        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired activation token"));

        if (activationToken.isUsed() || LocalDateTime.now().isAfter(activationToken.getExpiryDate())) {
            throw new IllegalArgumentException("Token expired or already used");
        }

        User driver = activationToken.getUser();
        if (!driver.getRole().equals(UserRole.DRIVER) || driver.isEnabled()) {
            throw new IllegalStateException("Invalid user for activation");
        }

        // Encode and set password
        driver.setPassword(passwordEncoder.encode(rawPassword));
        driver.setLastPasswordResetDate(Timestamp.valueOf(LocalDateTime.now()));
        driver.setEnabled(true);
        driver.setActive(true);
        userRepository.save(driver);

        // Mark token as used and delete
        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);
        activationTokenRepository.delete(activationToken);  // Cleanup expired/used tokens
    }
}
