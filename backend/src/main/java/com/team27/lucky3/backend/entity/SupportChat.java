package com.team27.lucky3.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a support chat session for a user.
 * Each user (driver/passenger) has exactly one support chat that persists forever.
 * Messages are accumulated over time and the chat is never deleted.
 */
@Entity
@Table(name = "support_chats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user (driver or passenger) who owns this support chat.
     * One-to-one relationship - each user has exactly one support chat.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * All messages in this support chat, ordered by timestamp.
     */
    @OneToMany(mappedBy = "supportChat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<Message> messages = new ArrayList<>();

    /**
     * Timestamp of the last message in this chat.
     * Used for sorting chats on admin side (newest first).
     */
    private LocalDateTime lastMessageTime;

    /**
     * Number of unread messages by admin.
     * Increments when user sends a message, resets when admin views/sends.
     */
    private int unreadCount = 0;

    /**
     * Timestamp when this chat was created.
     */
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageTime = LocalDateTime.now();
    }

    public void addMessage(Message message) {
        messages.add(message);
        message.setSupportChat(this);
        lastMessageTime = message.getTimestamp();
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void resetUnreadCount() {
        this.unreadCount = 0;
    }
}
