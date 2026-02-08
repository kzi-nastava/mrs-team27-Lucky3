package com.team27.lucky3.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Represents a single message in a support chat.
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;

    /**
     * Type of message: "SUPPORT" for support chat messages.
     */
    private String type;

    /**
     * The user who sent this message.
     * Can be the chat owner (user) or an admin.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @ToString.Exclude
    private User sender;

    /**
     * The support chat this message belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_chat_id")
    @ToString.Exclude
    private SupportChat supportChat;

    /**
     * Whether this message was sent by admin (true) or user (false).
     */
    private boolean fromAdmin;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (type == null) {
            type = "SUPPORT";
        }
    }
}