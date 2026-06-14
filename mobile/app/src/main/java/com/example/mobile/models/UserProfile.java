package com.example.mobile.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model representing a user profile for administrative purposes (e.g., blocking).
 */
public class UserProfile {
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String address;
    private String imageUrl;
    
    @SerializedName("blocked")
    private boolean isBlocked;

    public UserProfile() {
    }

    public UserProfile(String name, String surname, String email, String phoneNumber, String address, String imageUrl, boolean isBlocked) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.imageUrl = imageUrl;
        this.isBlocked = isBlocked;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
    
    public String getFullName() {
        return name + " " + surname;
    }
}
