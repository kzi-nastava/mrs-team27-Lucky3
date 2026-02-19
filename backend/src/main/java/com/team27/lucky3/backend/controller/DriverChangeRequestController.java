package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ReviewDriverChange;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/driver-change-requests", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Driver Change Requests", description = "Admin review of driver profile change requests")
public class DriverChangeRequestController {
    private final DriverChangeRequestService driverChangeRequestService;

    @Operation(summary = "List change requests", description = "Get all driver change requests, optionally filter by status (ADMIN only)")
    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.team27.lucky3.backend.entity.DriverChangeRequest>> getDriverChangeRequests(@RequestParam(name = "status", required = false) DriverChangeStatus status) {
        List<com.team27.lucky3.backend.entity.DriverChangeRequest> requests = driverChangeRequestService.getChangeRequests(status);

        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Review change request", description = "Approve or reject a driver profile change request (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{requestId}/review")
    public ResponseEntity<Void> reviewDriverChangeRequest(
            @PathVariable Long requestId,
            @RequestBody @Valid ReviewDriverChange review) {

        driverChangeRequestService.reviewChangeRequest(requestId, review);
        return ResponseEntity.ok().build();
    }

}
