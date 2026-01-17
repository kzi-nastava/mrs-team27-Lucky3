package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ChangePasswordRequest;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.FavoriteRouteResponse;
import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.UserService;
import com.team27.lucky3.backend.util.DummyData;
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
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
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
    
    //2.3 Update user profile
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

    @GetMapping("/{id}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long id) {
        Image image = userService.getProfileImage(id); // loads from DB

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getData());
    }

    //@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('DRIVER')")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vehicle")
    public ResponseEntity<VehicleInformation> getVehicle(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(DummyData.createDummyVehicle(id));
    }

    @GetMapping("/{id}/favorites")
    public ResponseEntity<List<FavoriteRouteResponse>> getFavoriteRoutes(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(List.of(DummyData.createDummyFavoriteRoute(1L)));
    }

    @PostMapping("/{id}/favorites")
    public ResponseEntity<FavoriteRouteResponse> addFavoriteRoute(@PathVariable @Min(1) Long id, @Valid @RequestBody FavoriteRouteResponse route) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.status(HttpStatus.CREATED).body(DummyData.createDummyFavoriteRoute(1L));
    }

    @DeleteMapping("/{id}/favorites/{routeId}")
    public ResponseEntity<Void> removeFavoriteRoute(@PathVariable @Min(1) Long id, @PathVariable @Min(1) Long routeId) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    // 2.12 Block user (driver or passenger) - Admin only
    @PutMapping("/{id}/block")
    public ResponseEntity<Void> blockUser(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    // 2.12 Unblock user - Admin only
    @PutMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.noContent().build();
    }

    // 2.12 Add note to user - Admin only
    // Note: Request body needs a DTO
    @PostMapping("/{id}/note")
    public ResponseEntity<Void> addNote(@PathVariable @Min(1) Long id, @Valid @RequestBody com.team27.lucky3.backend.dto.request.NoteRequest request) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 2.12 Get notes (optional, useful for admin to see why they blocked someone)
    @GetMapping("/{id}/note")
    public ResponseEntity<List<String>> getNotes(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("User not found");
        return ResponseEntity.ok(List.of("User was rude.", "Cancelled too many rides."));
    }
}
