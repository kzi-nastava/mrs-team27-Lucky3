package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ChangePasswordRequest;
import com.team27.lucky3.backend.dto.request.FcmTokenRequest;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.FavoriteRouteResponse;
import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.UserBlockingService;
import com.team27.lucky3.backend.service.UserService;
import com.team27.lucky3.backend.util.DummyData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "User profiles, passwords, vehicles, favorites, notes & FCM tokens")
public class UserController {
    private final UserService userService;
    private final UserBlockingService userBlockingService;

    @Operation(summary = "Get user profile", description = "Retrieve a user's profile by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable @Min(1) Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        UserProfile response = new UserProfile();

        response.setName(user.getName());
        response.setSurname(user.getSurname());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setEmail(user.getEmail());

        if (user.getProfileImage() != null) {
            response.setImageUrl("/api/users/" + user.getId() + "/profile-image");
        }

        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update user profile", description = "Update profile info with optional profile image (PASSENGER or ADMIN)")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('ADMIN')")
    @PutMapping(value = "/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable @Min(1) Long id,
            @Valid @RequestPart("user") UserProfile request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {
        User updated = userService.updateUser(id, request, profileImage);

        // map User -> UserProfile DTO
        UserProfile response = new UserProfile();
        response.setName(updated.getName());
        response.setSurname(updated.getSurname());
        response.setEmail(updated.getEmail());
        response.setPhoneNumber(updated.getPhoneNumber());
        response.setAddress(updated.getAddress());

        if (updated.getProfileImage() != null) {
            response.setImageUrl("/api/users/" + updated.getId() + "/profile-image");
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get profile image", description = "Retrieve the user's profile image as binary data", security = {})
    @GetMapping("/{id}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long id) {
        Image image = userService.getProfileImage(id); // loads from DB

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getData());
    }

    @Operation(summary = "Change password", description = "Change the user's password")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user vehicle", description = "Get the vehicle assigned to a driver")
    @GetMapping("/{id}/vehicle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VehicleInformation> getVehicle(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(DummyData.createDummyVehicle(id));
    }

    @Operation(summary = "Get favourite routes", description = "List all favourite routes for a passenger")
    @GetMapping("/{id}/favorites")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<List<FavoriteRouteResponse>> getFavoriteRoutes(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(List.of(DummyData.createDummyFavoriteRoute(1L)));
    }

    @Operation(summary = "Add favourite route", description = "Save a route as a favourite")
    @PostMapping("/{id}/favorites")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<FavoriteRouteResponse> addFavoriteRoute(@PathVariable @Min(1) Long id, @Valid @RequestBody FavoriteRouteResponse route) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.status(HttpStatus.CREATED).body(DummyData.createDummyFavoriteRoute(1L));
    }

    @Operation(summary = "Remove favourite route", description = "Delete a saved favourite route")
    @DeleteMapping("/{id}/favorites/{routeId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<Void> removeFavoriteRoute(@PathVariable @Min(1) Long id, @PathVariable @Min(1) Long routeId) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Block user (admin)", description = "Block a user by ID (ADMIN only)")
    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockUser(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unblock user (admin)", description = "Unblock a user by ID (ADMIN only)")
    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblockUser(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add note to user (admin)", description = "Attach a note to a user (ADMIN only)")
    @PostMapping("/{id}/note")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addNote(@PathVariable @Min(1) Long id, @Valid @RequestBody com.team27.lucky3.backend.dto.request.NoteRequest request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get user notes (admin)", description = "Get all admin notes for a user")
    @GetMapping("/{id}/note")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getNotes(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(List.of("User was rude.", "Cancelled too many rides."));
    }

    @Operation(summary = "Update FCM token", description = "Register or refresh the device's FCM push notification token")
    @PutMapping("/{id}/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateFcmToken(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody FcmTokenRequest request) {
        userService.updateFcmToken(id, request.getToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if user is blocked", description = "Returns the block reason if the user is blocked")
    @PreAuthorize("hasRole('DRIVER') or hasRole('PASSENGER')")
    @GetMapping("/is-blocked")
    public ResponseEntity<String> isUserBlocked(@RequestParam Long id) {
        String blockReason = userBlockingService.isBlocked(id);
        return ResponseEntity.ok(blockReason != null ? blockReason : "");
    }
}
