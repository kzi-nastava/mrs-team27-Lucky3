package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.NotificationResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.NotificationType;
import com.team27.lucky3.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/notification", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Notifications", description = "Notification history, read-state & cleanup")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications", description = "Paginated notification history, optionally filtered by type")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) NotificationType type,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponse> page;
        if (type != null) {
            page = notificationService.getNotificationsForUser(user.getId(), type, pageable);
        } else {
            page = notificationService.getNotificationsForUser(user.getId(), pageable);
        }
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get unread count", description = "Returns unread notification count for the badge")
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark as read", description = "Mark a specific notification as read")
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        NotificationResponse response = notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark all as read", description = "Mark all unread notifications as read")
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("markedCount", count));
    }

    @Operation(summary = "Delete all notifications", description = "Clear entire notification history")
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> deleteAll(
            @AuthenticationPrincipal User user) {
        int count = notificationService.deleteAllForUser(user.getId());
        return ResponseEntity.ok(Map.of("deletedCount", count));
    }

    @Operation(summary = "Delete one notification", description = "Delete a single notification by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteOne(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
