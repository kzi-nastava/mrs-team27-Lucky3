package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.NotificationResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.NotificationType;
import com.team27.lucky3.backend.service.NotificationService;
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

/**
 * REST endpoints for the logged-in user's notification history and read-state
 * management.
 *
 * <ul>
 *   <li>{@code GET  /api/notification}           — paginated history</li>
 *   <li>{@code GET  /api/notification/unread}     — unread count (badge)</li>
 *   <li>{@code PUT  /api/notification/{id}/read}  — mark one as read</li>
 *   <li>{@code PUT  /api/notification/read-all}   — mark all as read</li>
 * </ul>
 */
@RestController
@RequestMapping(value = "/api/notification", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieve paginated notification history for the authenticated user.
     * Optionally filter by {@code type} query parameter.
     *
     * <pre>GET /api/notification?page=0&size=20&type=PANIC</pre>
     */
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

    /**
     * Returns the number of unread notifications for the badge/counter.
     *
     * <pre>GET /api/notification/unread</pre>
     * Response: {@code { "unreadCount": 5 }}
     */
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark a specific notification as read.
     *
     * <pre>PUT /api/notification/42/read</pre>
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        NotificationResponse response = notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Mark <b>all</b> unread notifications as read for the current user.
     *
     * <pre>PUT /api/notification/read-all</pre>
     * Response: {@code { "markedCount": 12 }}
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("markedCount", count));
    }
}
