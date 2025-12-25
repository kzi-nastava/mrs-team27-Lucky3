package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.AdminDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    // 2.9.3 Admin Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboardData() {
        // Mock
        AdminDashboardResponse response = new AdminDashboardResponse(1250, 42, 125000.50);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}