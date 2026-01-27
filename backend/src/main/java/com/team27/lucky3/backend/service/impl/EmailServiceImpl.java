package com.team27.lucky3.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.team27.lucky3.backend.service.EmailService;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@lucky3.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
            System.out.println("EMAILING: " + to + " | " + subject + " | " + text);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void sendReviewRequestEmail(String to, String passengerName, String reviewToken) {
        String reviewLink = frontendUrl + "/review?token=" + reviewToken;
        
        String subject = "Rate Your Recent Ride - Lucky3";
        String text = String.format(
            "Hi %s,\n\n" +
            "Thank you for riding with Lucky3!\n\n" +
            "We'd love to hear about your experience. Please take a moment to rate your driver and vehicle:\n\n" +
            "%s\n\n" +
            "This link will expire in 3 days.\n\n" +
            "Your feedback helps us improve our service and reward great drivers.\n\n" +
            "Thank you,\n" +
            "The Lucky3 Team",
            passengerName != null ? passengerName : "Valued Customer",
            reviewLink
        );
        
        sendSimpleMessage(to, subject, text);
    }
}