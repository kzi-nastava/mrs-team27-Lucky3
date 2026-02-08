package com.example.mobile.models;

/**
 * Model class representing a User entity.
 * Used for local user data representation.
 */
public class User {

    private Long id;
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String address;
    private String profileImageUrl;
    private UserRole role;
    private boolean isActive;
    private boolean isBlocked;
    private boolean isVerified;

    public User() {
    }

    public User(Long id, String name, String surname, String email, String phoneNumber,
                String address, String profileImageUrl, UserRole role) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    /**
     * Creates a User from a UserResponse DTO.
     */
    public static User fromResponse(UserResponse response) {
        User user = new User();
        user.setId(response.getId());
        user.setName(response.getName());
        user.setSurname(response.getSurname());
        user.setEmail(response.getEmail());
        user.setPhoneNumber(response.getPhoneNumber());
        user.setAddress(response.getAddress());
        user.setProfileImageUrl(response.getProfilePictureUrl());
        
        // Parse role from string
        if (response.getRole() != null) {
            try {
                user.setRole(UserRole.valueOf(response.getRole()));
            } catch (IllegalArgumentException e) {
                user.setRole(UserRole.PASSENGER);
            }
        }
        
        return user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    /**
     * Returns the user's full name.
     */
    public String getFullName() {
        return name + " " + surname;
    }
}
