package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ReviewDriverChange;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/driver-change-requests", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class DriverChangeRequestController {
    private final DriverChangeRequestService driverChangeRequestService;

    // Get all driver change requests (optionally filter by status=PENDING)
    @GetMapping()
    public ResponseEntity<List<com.team27.lucky3.backend.entity.DriverChangeRequest>> getDriverChangeRequests(@RequestParam(name = "status", required = false) DriverChangeStatus status) {
        List<com.team27.lucky3.backend.entity.DriverChangeRequest> requests = driverChangeRequestService.getChangeRequests(status);

        return ResponseEntity.ok(requests);
    }

    // Admin sends review for a driver change request, approving or rejecting it
    @PutMapping("/{requestId}/review")
    public ResponseEntity<Void> reviewDriverChangeRequest(
            @PathVariable Long requestId,
            @RequestBody @Valid ReviewDriverChange review) {

        driverChangeRequestService.reviewChangeRequest(requestId, review);
        return ResponseEntity.ok().build();
    }

}
