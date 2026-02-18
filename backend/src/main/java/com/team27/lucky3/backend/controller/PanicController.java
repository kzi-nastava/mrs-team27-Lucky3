package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.PanicResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.service.PanicService;
import com.team27.lucky3.backend.util.DummyData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Panic", description = "Panic alert notifications (ADMIN)")
public class PanicController {

    private final PanicService panicService;

    // 2.6.3 Admin views panic notifications
    /* TODO: Check what is better
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PanicResponse>> getPanicNotifications() {
        RideResponse ride = DummyData.createDummyRideResponse(1L, 10L, 20L, RideStatus.PANIC);
        UserResponse user = new UserResponse(20L, "Panic", "User", "p@gmail.com", "url", UserRole.PASSENGER, "123", "Addr");

        PanicResponse panic = new PanicResponse(1L, user, ride, LocalDateTime.now(), "Driver is driving too fast!");
        return ResponseEntity.ok(List.of(panic));
    }*/

    @Operation(summary = "Get panic alerts", description = "Paginated list of all panic notifications (ADMIN only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PanicResponse>> getPanics(Pageable pageable) {
        Page<PanicResponse> response = panicService.findAll(pageable);
        return ResponseEntity.ok(panicService.findAll(pageable));
    }
}
