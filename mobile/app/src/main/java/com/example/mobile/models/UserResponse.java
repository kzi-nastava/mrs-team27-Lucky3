package com.example.mobile.models;

/**
 * DTO for user response from the backend.
 * Contains user profile information returned after registration or login.
 */
public class UserResponse {

    private Long id;
    private String name;
    private String surname;
    private String email;
    private String profilePictureUrl;
    private String role;
    private String phoneNumber;
    private String address;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String surname, String email,
                        String profilePictureUrl, String role, String phoneNumber, String address) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.address = address;
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

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    /**
     * Returns the full name of the user.
     */
    public String getFullName() {
        return name + " " + surname;
    }
}
