package com.team27.lucky3.backend.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
    
    /**
     * Send a review request email to a passenger after their ride ends.
     * @param to The passenger's email address
     * @param passengerName The passenger's name for personalization
     * @param reviewToken The JWT token for accessing the review page
     */
    void sendReviewRequestEmail(String to, String passengerName, String reviewToken);
}