package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.Ride;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
    
    /**
     * Send a review request email to a passenger after their ride ends.
     * @param to The passenger's email address
     * @param passengerName The passenger's name for personalization
     * @param reviewToken The JWT token for accessing the review page
     */
    void sendReviewRequestEmail(String to, String passengerName, String reviewToken);

    /**
     * Send an email to a linked passenger when they are added to a ride.
     * Includes a tracking token link to view the ride status.
     * @param to The linked passenger's email address
     * @param passengerName The passenger's name for personalization (or null if unknown)
     * @param ride The ride they are linked to
     * @param trackingToken The JWT token for tracking the ride
     */
    void sendLinkedPassengerAddedEmail(String to, String passengerName, Ride ride, String trackingToken);

    /**
     * Send an email to a linked passenger when they are added to a ride (async-safe version).
     * @param to The linked passenger's email address
     * @param passengerName The passenger's name for personalization (or null if unknown)
     * @param rideId The ID of the ride
     * @param startAddress The start address
     * @param endAddress The end address
     * @param scheduledTime The scheduled time as formatted string
     * @param driverName The driver's full name
     * @param estimatedCost The estimated cost
     * @param trackingToken The JWT token for tracking the ride
     */
    void sendLinkedPassengerAddedEmail(String to, String passengerName, Long rideId,
                                        String startAddress, String endAddress, String scheduledTime,
                                        String driverName, double estimatedCost, String trackingToken);

    /**
     * Send an email to a linked passenger when the ride is completed.
     * @param to The linked passenger's email address
     * @param passengerName The passenger's name for personalization (or null if unknown)
     * @param ride The completed ride
     */
    void sendLinkedPassengerRideCompletedEmail(String to, String passengerName, Ride ride);

    /**
     * Send an email to a linked passenger when the ride is completed (async-safe version).
     * @param to The linked passenger's email address
     * @param passengerName The passenger's name for personalization (or null if unknown)
     * @param rideId The ID of the ride
     * @param startAddress The start address
     * @param endAddress The end address
     * @param distance The distance traveled
     * @param totalCost The total cost
     * @param startTime The start time formatted
     * @param endTime The end time formatted
     */
    void sendLinkedPassengerRideCompletedEmail(String to, String passengerName, Long rideId,
                                                String startAddress, String endAddress, double distance,
                                                double totalCost, String startTime, String endTime);

    /**
     * Send an email to a linked passenger when the ride is cancelled.
     * @param to The linked passenger's email address
     * @param passengerName The passenger's name for personalization (or null if unknown)
     * @param ride The cancelled ride
     * @param cancelledByName The name of the person who cancelled the ride
     * @param cancelledByRole "driver" or "passenger"
     */
    void sendLinkedPassengerRideCancelledEmail(String to, String passengerName, Ride ride, 
                                                String cancelledByName, String cancelledByRole);

    /**
     * Send an email to a linked passenger when the ride is cancelled (async-safe version with primitive params).
     * @param to The linked passenger's email address
     * @param passengerName The passenger's name for personalization (or null if unknown)
     * @param rideId The ID of the cancelled ride
     * @param startAddress The start address of the ride
     * @param endAddress The end address of the ride
     * @param estimatedCost The estimated cost of the ride
     * @param cancelledByName The name of the person who cancelled the ride
     * @param cancelledByRole "driver" or "passenger"
     */
    void sendLinkedPassengerRideCancelledEmail(String to, String passengerName, Long rideId,
                                                String startAddress, String endAddress, double estimatedCost,
                                                String cancelledByName, String cancelledByRole);
}