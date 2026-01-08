package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.ReportResponse;
import com.team27.lucky3.backend.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class ReportController {

    // 2.10 Generate reports (Admin, Driver, User)
    // Types: "RIDES", "KILOMETERS", "MONEY"
    @GetMapping("/{userId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam @NotNull String type) {

        // Mock
        ReportResponse response = new ReportResponse(
                Map.of("2025-01-20", 150.0, "2025-01-21", 200.0),
                350.0,
                175.0
        );
        return ResponseEntity.ok(response);
    }

    // Admin global report
    @GetMapping("/admin")
    public ResponseEntity<ReportResponse> getGlobalReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam @NotNull String type) {

        ReportResponse response = new ReportResponse(
                Map.of("2025-01-20", 1500.0, "2025-01-21", 2000.0),
                3500.0,
                1750.0
        );
        return ResponseEntity.ok(response);
    }
}
