package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.BlockUserRequest;
import com.team27.lucky3.backend.dto.response.BlockUserResponse;
import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.service.UserBlockingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserBlockingController {

    private final UserBlockingService userBlockingService;

    @Autowired
    public UserBlockingController(UserBlockingService userBlockingService) {
        this.userBlockingService = userBlockingService;
    }

    @PostMapping("/block")
    public ResponseEntity<Void> blockUser(@Valid @RequestBody BlockUserRequest request) {
        userBlockingService.blockUser(
                request.getEmail(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/unblock/{email}")
    public ResponseEntity<BlockUserResponse> unblockUser(@PathVariable String email) {
        return ResponseEntity.ok(userBlockingService.unblockUser(email));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<UserProfile>> getBlockedUsers() {
        return ResponseEntity.ok(userBlockingService.getBlockedUsers());
    }

    @GetMapping("/unblocked")
    public ResponseEntity<List<UserProfile>> getUnblockedUsers() {
        return ResponseEntity.ok(userBlockingService.getUnblockedUsers());
    }
}

