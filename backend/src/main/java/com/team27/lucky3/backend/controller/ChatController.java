package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.SupportMessageRequest;
import com.team27.lucky3.backend.dto.response.SupportChatListItemResponse;
import com.team27.lucky3.backend.dto.response.SupportChatResponse;
import com.team27.lucky3.backend.dto.response.SupportMessageResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.service.SupportChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Support Chat functionality.
 * 
 * Endpoints:
 * - GET /api/support/chat - Get current user's support chat (creates if not exists)
 * - POST /api/support/chat/message - Send a message as user
 * - GET /api/support/admin/chats - Get all chats (admin only)
 * - GET /api/support/admin/chat/{chatId} - Get specific chat details (admin only)
 * - POST /api/support/admin/chat/{chatId}/message - Send message as admin
 * - POST /api/support/admin/chat/{chatId}/read - Mark chat as read
 */
@RestController
@RequestMapping(value = "/api/support", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final SupportChatService supportChatService;

    // ==================== User Endpoints ====================

    /**
     * Get current user's support chat.
     * Creates a new chat if one doesn't exist.
     */
    @GetMapping("/chat")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<SupportChatResponse> getMyChat(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportChatService.getOrCreateChatForUser(user));
    }

    /**
     * Send a message to support as a user.
     */
    @PostMapping("/chat/message")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<SupportMessageResponse> sendUserMessage(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(supportChatService.sendUserMessage(user, request));
    }

    // ==================== Admin Endpoints ====================

    /**
     * Get all support chats for admin view.
     * Returns chats ordered by last message time (newest first).
     */
    @GetMapping("/admin/chats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportChatListItemResponse>> getAllChats() {
        return ResponseEntity.ok(supportChatService.getAllChatsForAdmin());
    }

    /**
     * Get a specific chat with all messages (admin only).
     */
    @GetMapping("/admin/chat/{chatId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportChatResponse> getChatById(@PathVariable Long chatId) {
        return ResponseEntity.ok(supportChatService.getChatById(chatId));
    }

    /**
     * Get messages for a specific chat (admin only).
     */
    @GetMapping("/admin/chat/{chatId}/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportMessageResponse>> getChatMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(supportChatService.getChatMessages(chatId));
    }

    /**
     * Send a message to a user's support chat as admin.
     */
    @PostMapping("/admin/chat/{chatId}/message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportMessageResponse> sendAdminMessage(
            @AuthenticationPrincipal User admin,
            @PathVariable Long chatId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(supportChatService.sendAdminMessage(admin, chatId, request));
    }

    /**
     * Mark a chat as read (resets unread count).
     */
    @PostMapping("/admin/chat/{chatId}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markChatAsRead(@PathVariable Long chatId) {
        supportChatService.markChatAsRead(chatId);
        return ResponseEntity.ok().build();
    }
}
