package com.team27.lucky3.backend.entity.enums;

/**
 * Types of notifications in the system.
 *
 * <ul>
 *   <li>{@link #RIDE_STATUS}         – Ride status changed (accepted, started, finished, cancelled, etc.)</li>
 *   <li>{@link #RIDE_INVITE}         – Linked-passenger invite to join a ride</li>
 *   <li>{@link #PANIC}               – Someone pressed the panic button — directed to ALL admins</li>
 *   <li>{@link #SUPPORT}             – Support-chat message notification</li>
 *   <li>{@link #DRIVER_ASSIGNMENT}   – Driver was assigned / ride request for driver</li>
 *   <li>{@link #RIDE_FINISHED}       – Ride completed — triggers email summary to passengers</li>
 * </ul>
 */
public enum NotificationType {
    RIDE_STATUS,
    RIDE_INVITE,
    PANIC,
    SUPPORT,
    DRIVER_ASSIGNMENT,
    RIDE_FINISHED,
    RIDE_CREATED,
    RIDE_CANCELLED,
    RIDE_SCHEDULED_REMINDER,
    STOP_COMPLETED,
    LEAVE_REVIEW
}
