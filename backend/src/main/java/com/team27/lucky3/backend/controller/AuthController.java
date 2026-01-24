package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.util.TokenUtils;
import com.team27.lucky3.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final TokenUtils tokenUtils;

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        TokenResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    // 2.2.2 User registration + email activation (unregistered -> registered user)
    @PostMapping(
            value = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserResponse> register(
            @Valid @RequestPart("data") PassengerRegistrationRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {

        User user = authService.registerPassenger(request, profileImage);

        String imageUrl = "/api/users/" + user.getId() + "/profile-image";      //slika se uzima sa endpointa. To je url

        UserResponse response = new UserResponse(
                user.getId(), user.getName(), user.getSurname(),
                user.getEmail(), imageUrl,
                user.getRole(), user.getPhoneNumber(), user.getAddress()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // 2.2.2 User registration + email activation
    @GetMapping("/activate")
    public ResponseEntity<Void> activateAccount(@RequestParam @NotBlank String token) {
        // Activation logic using token
        authService.activateAccount(token);
        return ResponseEntity.ok().build();
    }

    // 2.2.2 Resend activation email
    @PostMapping(value = "/resend-activation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resendActivation(@Valid @RequestBody EmailRequest emailRequest) {
        authService.resendActivationEmail(emailRequest.getEmail());
        return ResponseEntity.ok().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest emailRequest) {
        authService.forgotPassword(emailRequest.getEmail());
        return ResponseEntity.noContent().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        authService.resetPassword(resetRequest.getToken(), resetRequest.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    // 2.2.1 Login + forgot password + driver availability rules (registered user / driver)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // 1. Get token using TokenUtils
        String token = tokenUtils.getToken(request);

        if (token != null) {
            // 2. Get Email from token (matches AuthService.logout signature)
            String email = tokenUtils.getEmailFromToken(token);
            if (email != null) {
                authService.logout(email);
            }
        }
        return ResponseEntity.noContent().build();
    }

    // 2.2.3 Admin creates driver accounts + vehicle info + password setup via email link (admin, driver)
    @PutMapping(value = "/driver-activation/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setInitialPassword(@Valid @RequestBody SetInitialPassword initialPassword) {
//        authService.activateDriverWithPassword(initialPassword.getToken(), initialPassword.getPassword());
        return ResponseEntity.noContent().build();
    }

    // Validate password reset token (for reset-password page pre-check)
    @GetMapping("/reset-password/validate")
    public ResponseEntity<Void> validateResetPasswordToken(@RequestParam @NotBlank String token) {
        boolean valid = authService.isPasswordResetTokenValid(token);
        return valid ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
