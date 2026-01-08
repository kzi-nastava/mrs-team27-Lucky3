package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.LoginRequest;
import com.team27.lucky3.backend.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    void logout(String email);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}