package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.SupportMessageRequest;
import com.team27.lucky3.backend.dto.response.SupportChatListItemResponse;
import com.team27.lucky3.backend.dto.response.SupportChatResponse;
import com.team27.lucky3.backend.dto.response.SupportMessageResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.service.SupportChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/support", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Support Chat", description = "User support chat & admin chat management")
public class ChatController {

    private final SupportChatService supportChatService;

    // ==================== User Endpoints ====================

    @Operation(summary = "Get my support chat", description = "Get current user's support chat (creates if not exists). PASSENGER or DRIVER.")
    @GetMapping("/chat")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<SupportChatResponse> getMyChat(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportChatService.getOrCreateChatForUser(user));
    }

    @Operation(summary = "Send message as user", description = "Send a support message. PASSENGER or DRIVER.")
    @PostMapping("/chat/message")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<SupportMessageResponse> sendUserMessage(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(supportChatService.sendUserMessage(user, request));
    }

    // ==================== Admin Endpoints ====================

    @Operation(summary = "Get all chats (admin)", description = "List all support chats ordered by last message time")
    @GetMapping("/admin/chats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportChatListItemResponse>> getAllChats() {
        return ResponseEntity.ok(supportChatService.getAllChatsForAdmin());
    }

    @Operation(summary = "Get chat by ID (admin)", description = "Get a specific chat with all messages")
    @GetMapping("/admin/chat/{chatId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportChatResponse> getChatById(@PathVariable Long chatId) {
        return ResponseEntity.ok(supportChatService.getChatById(chatId));
    }

    @Operation(summary = "Get chat messages (admin)", description = "Get all messages for a specific chat")
    @GetMapping("/admin/chat/{chatId}/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportMessageResponse>> getChatMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(supportChatService.getChatMessages(chatId));
    }

    @Operation(summary = "Send message as admin", description = "Send a message to a user's support chat")
    @PostMapping("/admin/chat/{chatId}/message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportMessageResponse> sendAdminMessage(
            @AuthenticationPrincipal User admin,
            @PathVariable Long chatId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(supportChatService.sendAdminMessage(admin, chatId, request));
    }

    @Operation(summary = "Mark chat as read (admin)", description = "Reset unread count for a chat")
    @PostMapping("/admin/chat/{chatId}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markChatAsRead(@PathVariable Long chatId) {
        supportChatService.markChatAsRead(chatId);
        return ResponseEntity.ok().build();
    }
}
