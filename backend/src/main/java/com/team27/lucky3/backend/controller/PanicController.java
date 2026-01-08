package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.PanicResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.util.DummyData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/panic", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class PanicController {

    // 2.6.3 Admin views panic notifications
    @GetMapping
    public ResponseEntity<List<PanicResponse>> getPanicNotifications() {
        RideResponse ride = DummyData.createDummyRideResponse(1L, 10L, 20L, RideStatus.PANIC);
        UserResponse user = new UserResponse(20L, "Panic", "User", "p@gmail.com", "url", UserRole.PASSENGER, "123", "Addr");

        PanicResponse panic = new PanicResponse(1L, user, ride, LocalDateTime.now(), "Driver is driving too fast!");
        return ResponseEntity.ok(List.of(panic));
    }
}
