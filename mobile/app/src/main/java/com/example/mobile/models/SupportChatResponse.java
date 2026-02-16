package com.example.mobile.models;

import java.util.List;

/**
 * Mirrors the backend {@code SupportChatResponse} DTO.
 * Returned when fetching a user's support chat or admin fetching a specific chat.
 */
public class SupportChatResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;
    private String createdAt;
    private List<SupportMessageResponse> messages;

    public SupportChatResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<SupportMessageResponse> getMessages() { return messages; }
    public void setMessages(List<SupportMessageResponse> messages) { this.messages = messages; }
}
