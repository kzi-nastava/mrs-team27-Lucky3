package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.Ride;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.team27.lucky3.backend.service.EmailService;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${frontend.url}")
    private String frontendUrl;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

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

    @Override
    public void sendLinkedPassengerAddedEmail(String to, String passengerName, Ride ride, String trackingToken) {
        String trackingLink = frontendUrl + "/ride/track?token=" + trackingToken;

        String startAddress = ride.getStartLocation() != null 
                ? ride.getStartLocation().getAddress() : "Unknown";
        String endAddress = ride.getEndLocation() != null 
                ? ride.getEndLocation().getAddress() : "Unknown";
        String scheduledTime = ride.getScheduledTime() != null 
                ? ride.getScheduledTime().format(FMT) : "As soon as possible";
        String driverName = ride.getDriver() != null 
                ? ride.getDriver().getName() + " " + ride.getDriver().getSurname() : "To be assigned";

        sendLinkedPassengerAddedEmail(to, passengerName, ride.getId(), 
                startAddress, endAddress, scheduledTime, driverName, 
                ride.getEstimatedCost() != null ? ride.getEstimatedCost() : 0.0, trackingToken);
    }

    @Override
    public void sendLinkedPassengerAddedEmail(String to, String passengerName, Long rideId,
                                               String startAddress, String endAddress, String scheduledTime,
                                               String driverName, double estimatedCost, String trackingToken) {
        String trackingLink = frontendUrl + "/ride/track?token=" + trackingToken;

        String subject = "You've been added to a ride - Lucky3";
        String text = String.format(
            "Hi %s,\n\n" +
            "You have been added as a passenger to a Lucky3 ride!\n\n" +
            "═══ Ride Details ═══\n" +
            "From:      %s\n" +
            "To:        %s\n" +
            "Scheduled: %s\n" +
            "Driver:    %s\n" +
            "Estimated Cost: %.2f RSD\n\n" +
            "Track your ride in real-time:\n" +
            "%s\n\n" +
            "This link will allow you to monitor the ride status and see the vehicle location.\n\n" +
            "If you did not expect this, you can safely ignore this email.\n\n" +
            "Safe travels!\n" +
            "— The Lucky3 Team",
            passengerName != null ? passengerName : "Passenger",
            startAddress,
            endAddress,
            scheduledTime,
            driverName,
            estimatedCost,
            trackingLink
        );
        
        sendSimpleMessage(to, subject, text);
    }

    @Override
    public void sendLinkedPassengerRideCompletedEmail(String to, String passengerName, Ride ride) {
        String startAddress = ride.getStartLocation() != null 
                ? ride.getStartLocation().getAddress() : "Unknown";
        String endAddress = ride.getEndLocation() != null 
                ? ride.getEndLocation().getAddress() : "Unknown";
        String startTime = ride.getStartTime() != null 
                ? ride.getStartTime().format(FMT) : "N/A";
        String endTime = ride.getEndTime() != null 
                ? ride.getEndTime().format(FMT) : "N/A";

        sendLinkedPassengerRideCompletedEmail(to, passengerName, ride.getId(),
                startAddress, endAddress,
                ride.getDistance() != null ? ride.getDistance() : 0.0,
                ride.getTotalCost() != null ? ride.getTotalCost() : 0.0,
                startTime, endTime);
    }

    @Override
    public void sendLinkedPassengerRideCompletedEmail(String to, String passengerName, Long rideId,
                                                       String startAddress, String endAddress, double distance,
                                                       double totalCost, String startTime, String endTime) {
        String subject = "Your ride has been completed - Lucky3";
        String text = String.format(
            "Hi %s,\n\n" +
            "The ride you were part of has been completed!\n\n" +
            "═══ Ride Summary ═══\n" +
            "From:     %s\n" +
            "To:       %s\n" +
            "Distance: %.2f km\n" +
            "Cost:     %.2f RSD\n" +
            "Started:  %s\n" +
            "Ended:    %s\n\n" +
            "Thank you for riding with Lucky3!\n\n" +
            "— The Lucky3 Team",
            passengerName != null ? passengerName : "Passenger",
            startAddress,
            endAddress,
            distance,
            totalCost,
            startTime,
            endTime
        );
        
        sendSimpleMessage(to, subject, text);
    }

    @Override
    public void sendLinkedPassengerRideCancelledEmail(String to, String passengerName, Ride ride, 
                                                       String cancelledByName, String cancelledByRole) {
        String startAddress = ride.getStartLocation() != null 
                ? ride.getStartLocation().getAddress() : "Unknown";
        String endAddress = ride.getEndLocation() != null 
                ? ride.getEndLocation().getAddress() : "Unknown";
        String reason = ride.getRejectionReason() != null && !ride.getRejectionReason().isEmpty()
                ? ride.getRejectionReason() : "No reason provided";

        String subject = "Your ride has been cancelled - Lucky3";
        String text = String.format(
            "Hi %s,\n\n" +
            "Unfortunately, the ride you were part of has been cancelled.\n\n" +
            "═══ Cancelled Ride Details ═══\n" +
            "From:          %s\n" +
            "To:            %s\n" +
            "Cancelled by:  %s (%s)\n" +
            "Reason:        %s\n\n" +
            "We apologize for any inconvenience. " +
            "Please book a new ride if you still need transportation.\n\n" +
            "— The Lucky3 Team",
            passengerName != null ? passengerName : "Passenger",
            startAddress,
            endAddress,
            cancelledByName,
            cancelledByRole,
            reason
        );
        
        sendSimpleMessage(to, subject, text);
    }

    @Override
    public void sendLinkedPassengerRideCancelledEmail(String to, String passengerName, Long rideId,
                                                       String startAddress, String endAddress, double estimatedCost,
                                                       String cancelledByName, String cancelledByRole) {
        String subject = "Your ride has been cancelled - Lucky3";
        String text = String.format(
            "Hi %s,\n\n" +
            "Unfortunately, the ride #%d you were part of has been cancelled.\n\n" +
            "═══ Cancelled Ride Details ═══\n" +
            "From:          %s\n" +
            "To:            %s\n" +
            "Cancelled by:  %s (%s)\n\n" +
            "We apologize for any inconvenience. " +
            "Please book a new ride if you still need transportation.\n\n" +
            "— The Lucky3 Team",
            passengerName != null ? passengerName : "Passenger",
            rideId,
            startAddress,
            endAddress,
            cancelledByName,
            cancelledByRole
        );
        
        sendSimpleMessage(to, subject, text);
    }
}