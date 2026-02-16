package com.team27.lucky3.backend.service;

import java.util.Map;

/**
 * Service for sending Firebase Cloud Messaging (FCM) push notifications.
 * <p>
 * All methods are designed to be non-blocking and failure-tolerant —
 * a failed FCM push must never break the notification persist-first contract.
 */
public interface FcmService {

    /**
     * Returns {@code true} if the Firebase Admin SDK was successfully initialized.
     * When {@code false}, all send methods become no-ops.
     */
    boolean isAvailable();

    /**
     * Sends a data-only FCM message to a specific device token.
     * Data messages are always handled by {@code onMessageReceived()} on the client,
     * giving the app full control over notification display.
     *
     * @param fcmToken  the target device registration token
     * @param title     notification title (passed as data field)
     * @param body      notification body (passed as data field)
     * @param data      extra key-value pairs (e.g. rideId, type for deep-linking)
     */
    void sendToDevice(String fcmToken, String title, String body, Map<String, String> data);

    /**
     * Convenience overload without extra data payload.
     */
    void sendToDevice(String fcmToken, String title, String body);
}
