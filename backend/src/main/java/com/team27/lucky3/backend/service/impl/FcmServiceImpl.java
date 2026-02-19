package com.team27.lucky3.backend.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.team27.lucky3.backend.service.FcmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Production implementation of {@link FcmService}.
 * <p>
 * Sends <b>data-only</b> FCM messages so that the Android client's
 * {@code FirebaseMessagingService.onMessageReceived()} is always invoked,
 * regardless of whether the app is in foreground or background.
 * <p>
 * All sends are executed asynchronously via {@link CompletableFuture} to avoid
 * blocking the notification pipeline. If Firebase was not initialised (no service
 * account file), every call is a silent no-op.
 * <p>
 * Handles stale tokens gracefully: when FCM returns {@code UNREGISTERED},
 * the token is logged for future cleanup.
 */
@Service
@Slf4j
public class FcmServiceImpl implements FcmService {

    // ── availability check ──────────────────────────────────────────────

    @Override
    public boolean isAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }

    // ── public API ──────────────────────────────────────────────────────

    @Override
    public void sendToDevice(String fcmToken, String title, String body) {
        sendToDevice(fcmToken, title, body, Collections.emptyMap());
    }

    @Override
    public void sendToDevice(String fcmToken, String title, String body, Map<String, String> data) {
        if (!isAvailable()) {
            log.debug("FCM not available — skipping push to token {}", maskToken(fcmToken));
            return;
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("No FCM token provided — skipping push");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(fcmToken)
                        // Data payload — always triggers onMessageReceived()
                        .putData("title", title)
                        .putData("body", body)
                        .putData("click_action", "OPEN_MAIN_ACTIVITY");

                // Merge caller-supplied data
                if (data != null && !data.isEmpty()) {
                    data.forEach(builder::putData);
                }

                // Android-specific: high priority for timely delivery
                builder.setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setTtl(300_000L) // 5 minutes TTL
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("ride_updates")
                                .build())
                        .build());

                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                log.debug("FCM message sent successfully: {} → token {}", messageId, maskToken(fcmToken));

            } catch (FirebaseMessagingException e) {
                handleFcmError(fcmToken, e);
            } catch (Exception e) {
                log.error("Unexpected error sending FCM to {}: {}", maskToken(fcmToken), e.getMessage());
            }
        });
    }

    // ── error handling ──────────────────────────────────────────────────

    private void handleFcmError(String fcmToken, FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();

        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            // Token is stale / invalid — client needs to re-register
            log.warn("FCM token {} is invalid or unregistered ({}). "
                    + "Token should be cleared on next login.", maskToken(fcmToken), code);
        } else if (code == MessagingErrorCode.QUOTA_EXCEEDED) {
            log.warn("FCM quota exceeded — message to {} not delivered", maskToken(fcmToken));
        } else {
            log.error("FCM send failed for token {} — code={}, message={}",
                    maskToken(fcmToken), code, e.getMessage());
        }
    }

    // ── utility ─────────────────────────────────────────────────────────

    /**
     * Masks an FCM token for safe logging (shows first 10 + last 5 chars).
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) return "***";
        return token.substring(0, 10) + "..." + token.substring(token.length() - 5);
    }
}
