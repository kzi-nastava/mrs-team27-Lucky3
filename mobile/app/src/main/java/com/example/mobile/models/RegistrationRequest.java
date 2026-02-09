package com.example.mobile.models;

/**
 * DTO for passenger registration request.
 * Matches the backend PassengerRegistrationRequest structure.
 */
public class RegistrationRequest {

    private String name;
    private String surname;
    private String email;
    private String password;
    private String confirmPassword;
    private String phoneNumber;
    private String address;

    public RegistrationRequest() {
    }

    public RegistrationRequest(String name, String surname, String email,
                               String password, String confirmPassword,
                               String phoneNumber, String address) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
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
     * Validates that password and confirm password match.
     */
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
