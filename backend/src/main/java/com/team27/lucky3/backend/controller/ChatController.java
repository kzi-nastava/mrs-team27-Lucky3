package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.MessageRequest;
import com.team27.lucky3.backend.dto.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class ChatController {

    // 2.11 Live Support - Get Session History
    @GetMapping("/session/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getChatSession(@PathVariable Long userId) {
        MessageResponse m1 = new MessageResponse(1L, userId, null, "Hello support", LocalDateTime.now().minusMinutes(5), "SUPPORT");
        MessageResponse m2 = new MessageResponse(2L, 100L, userId, "How can I help?", LocalDateTime.now().minusMinutes(4), "SUPPORT"); // 100L is admin
        return ResponseEntity.ok(List.of(m1, m2));
    }

    // 2.11 Live Support - Send Message
    @PostMapping("/session/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(@PathVariable Long userId, @Valid @RequestBody MessageRequest request) {
        MessageResponse response = new MessageResponse(3L, userId, 100L, request.getMessage(), LocalDateTime.now(), request.getType());
        return ResponseEntity.ok(response);
    }
}
