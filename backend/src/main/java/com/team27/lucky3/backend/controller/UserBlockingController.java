package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.BlockUserRequest;
import com.team27.lucky3.backend.dto.response.BlockUserResponse;
import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.service.UserBlockingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "User Blocking", description = "Block & unblock users (ADMIN)")
public class UserBlockingController {

    private final UserBlockingService userBlockingService;

    @Autowired
    public UserBlockingController(UserBlockingService userBlockingService) {
        this.userBlockingService = userBlockingService;
    }

    @Operation(summary = "Block user", description = "Block a user by email with a reason")
    @PostMapping("/block")
    public ResponseEntity<Void> blockUser(@Valid @RequestBody BlockUserRequest request) {
        userBlockingService.blockUser(
                request.getEmail(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Unblock user", description = "Remove block from a user by email")
    @PostMapping("/unblock/{email}")
    public ResponseEntity<BlockUserResponse> unblockUser(@PathVariable String email) {
        return ResponseEntity.ok(userBlockingService.unblockUser(email));
    }

    @Operation(summary = "Get blocked users", description = "List all currently blocked users")
    @GetMapping("/blocked")
    public ResponseEntity<List<UserProfile>> getBlockedUsers() {
        return ResponseEntity.ok(userBlockingService.getBlockedUsers());
    }

    @Operation(summary = "Get unblocked users", description = "List all non-blocked users")
    @GetMapping("/unblocked")
    public ResponseEntity<List<UserProfile>> getUnblockedUsers() {
        return ResponseEntity.ok(userBlockingService.getUnblockedUsers());
    }
}

