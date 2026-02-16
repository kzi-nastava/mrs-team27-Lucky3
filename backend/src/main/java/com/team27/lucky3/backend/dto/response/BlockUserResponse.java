package com.team27.lucky3.backend.dto.response;

public class BlockUserResponse {

    private Long userId;
    private String email;
    private String name;
    private String surname;
    private boolean isBlocked;
    private String blockReason;
    private String message;

    // Constructors
    public BlockUserResponse() {}

    public BlockUserResponse(Long userId, String email, String name, String surname,
                             boolean isBlocked, String blockReason, String message) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.isBlocked = isBlocked;
        this.blockReason = blockReason;
        this.message = message;
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
