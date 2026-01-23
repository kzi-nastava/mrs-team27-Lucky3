package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.request.PassengerRegistrationRequest;
import com.team27.lucky3.backend.dto.response.TokenResponse;
import com.team27.lucky3.backend.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    void logout(String email);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    User registerPassenger(PassengerRegistrationRequest request, MultipartFile profileImage) throws IOException;
    void activateAccount(String token);
    void resendActivationEmail(String email);
    boolean isPasswordResetTokenValid(String token);
}