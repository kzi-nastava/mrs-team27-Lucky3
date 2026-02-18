package com.example.mobile.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mobile.MainActivity;
import com.example.mobile.R;
import com.example.mobile.models.AppNotification;
import com.example.mobile.models.FcmTokenRequest;
import com.example.mobile.utils.AppLifecycleTracker;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.NotificationHelper;
import com.example.mobile.utils.NotificationStore;
import com.example.mobile.utils.SharedPreferencesManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * <p>
 * Receives <b>data-only</b> messages from the backend's {@code FcmService}.
 * This guarantees that {@code onMessageReceived()} is called in every app state
 * (foreground, background, killed), giving us full control over notification display.
 * <p>
 * Deep-link handling:
 * <ul>
 *   <li>Messages with {@code rideId} data open the Active Ride screen</li>
 *   <li>Messages with type {@code PANIC} open the Admin Panic screen</li>
 *   <li>All other messages open the main activity</li>
 * </ul>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    /** Thread-safe counter for unique notification IDs. */
    private static final AtomicInteger notificationIdCounter = new AtomicInteger(1000);

    // ════════════════════════════════════════════════════════════════════
    //  TOKEN REFRESH
    // ════════════════════════════════════════════════════════════════════

    /**
     * Called when the FCM registration token is created or rotated.
     * Stores it locally and attempts to sync with the backend if the user is logged in.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "FCM token refreshed");

        SharedPreferencesManager prefs = new SharedPreferencesManager(getApplicationContext());
        prefs.saveFcmToken(token);
        prefs.setFcmTokenSynced(false);

        // If user is logged in, immediately sync the new token with the backend
        if (prefs.isLoggedIn()) {
            syncTokenWithBackend(prefs, token);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  MESSAGE RECEIVED
    // ════════════════════════════════════════════════════════════════════

    /**
     * Called when a data message is received from the backend.
     * Builds and displays a local notification with appropriate deep-link intent.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) {
            Log.w(TAG, "Received empty data message — ignoring");
            return;
        }

        String title = data.getOrDefault("title", "Lucky3");
        String body = data.getOrDefault("body", "");
        String type = data.getOrDefault("type", "GENERAL");
        String rideIdStr = data.get("rideId");
        String notificationIdStr = data.get("notificationId");
        Log.d(TAG, "FCM data message received — type=" + type + ", rideId=" + rideIdStr);

        // Support and ride-status notifications are handled by AppNotificationManager
        // via WebSocket when the manager is active AND the WebSocket is actually connected.
        // The personal notification queue (/user/{id}/queue/notifications) now handles
        // RIDE_CREATED, RIDE_INVITE, DRIVER_ASSIGNMENT, and ride statuses when not
        // subscribed to the specific ride topic. Skip all of these to avoid duplicates.
        //
        // IMPORTANT: only skip when the app is in the foreground. When backgrounded,
        // Android may silently kill the idle TCP connection while the STOMP client still
        // reports isConnected()=true (stale connection). Skipping FCM in that state means
        // the notification is lost entirely — so we always fall through to show the system
        // notification when the user is not actively using the app.
        if (com.example.mobile.utils.AppLifecycleTracker.isAppInForeground()
                && com.example.mobile.utils.AppNotificationManager.getInstance().isStarted()
                && com.example.mobile.utils.WebSocketManager.getInstance().isConnected()) {

            switch (type) {
                case "SUPPORT":
                    // Support is always handled via WS user topic
                    Log.d(TAG, "Skipping SUPPORT FCM \u2014 handled by WebSocket");
                    return;
                case "RIDE_CREATED":
                case "RIDE_INVITE":
                case "DRIVER_ASSIGNMENT":
                    // Handled by personal notification queue subscription
                    Log.d(TAG, "Skipping " + type + " FCM \u2014 handled by personal queue WebSocket");
                    return;
                case "RIDE_STATUS":
                case "RIDE_FINISHED":
                case "RIDE_CANCELLED":
                case "STOP_COMPLETED":
                    // Handled by ride topic when subscribed, or personal queue otherwise
                    Log.d(TAG, "Skipping " + type + " FCM \u2014 handled by WebSocket");
                    return;
                case "LEAVE_REVIEW":
                    // Handled by personal notification queue
                    Log.d(TAG, "Skipping LEAVE_REVIEW FCM \u2014 handled by personal queue WebSocket");
                    return;
                case "PANIC":
                    // Handled by /topic/panic subscription (admin)
                    Log.d(TAG, "Skipping PANIC FCM \u2014 handled by WebSocket");
                    return;
            }
        }

        // Determine channel and priority based on notification type
        String channelId;
        int priority;
        switch (type) {
            case "PANIC":
                channelId = NotificationHelper.CHANNEL_PANIC_ALERTS;
                priority = NotificationCompat.PRIORITY_MAX;
                break;
            case "RIDE_STATUS":
            case "RIDE_INVITE":
            case "RIDE_FINISHED":
            case "RIDE_CANCELLED":
            case "RIDE_CREATED":
            case "DRIVER_ASSIGNMENT":
            case "STOP_COMPLETED":
                channelId = NotificationHelper.CHANNEL_RIDE_UPDATES;
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            case "LEAVE_REVIEW":
                channelId = NotificationHelper.CHANNEL_GENERAL;
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            case "SUPPORT":
                channelId = NotificationHelper.CHANNEL_GENERAL;
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            default:
                channelId = NotificationHelper.CHANNEL_GENERAL;
                priority = NotificationCompat.PRIORITY_DEFAULT;
                break;
        }

        // Build deep-link intent
        Intent intent = buildDeepLinkIntent(type, rideIdStr);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Pass backend notification ID so it can be deleted when the user taps
        if (notificationIdStr != null && !notificationIdStr.isEmpty()) {
            try {
                intent.putExtra("backendNotificationId", Long.parseLong(notificationIdStr));
            } catch (NumberFormatException ignored) {}
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationIdCounter.get(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Vibration for ride and panic notifications
        if ("PANIC".equals(type)) {
            builder.setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setColor(Color.RED)
                    .setColorized(true)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setLights(Color.RED, 1000, 300)
                    .setOngoing(true);
            // Play urgent alarm sound for panic
            playPanicAlarm();
        } else if (channelId.equals(NotificationHelper.CHANNEL_RIDE_UPDATES)) {
            builder.setVibrate(new long[]{0, 300, 150, 300});
        }

        // Expandable text for long messages
        if (body.length() > 50) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        // Always display the Android system notification so the user sees it
        // regardless of whether the app is in the foreground or background.
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted — cannot show notification");
        }

        // Feed in-app notification store so bell badge and panel stay in sync
        AppNotification.Type notifType;
        switch (type) {
            case "PANIC":
                notifType = AppNotification.Type.PANIC_ALERT;
                break;
            case "RIDE_FINISHED":
                notifType = AppNotification.Type.RIDE_FINISHED;
                break;
            case "RIDE_CANCELLED":
                notifType = AppNotification.Type.RIDE_CANCELLED;
                break;
            case "RIDE_CREATED":
                notifType = AppNotification.Type.RIDE_CREATED;
                break;
            case "DRIVER_ASSIGNMENT":
                notifType = AppNotification.Type.DRIVER_ASSIGNED;
                break;
            case "STOP_COMPLETED":
                notifType = AppNotification.Type.STOP_COMPLETED;
                break;
            case "LEAVE_REVIEW":
                notifType = AppNotification.Type.LEAVE_REVIEW;
                break;
            case "RIDE_STATUS":
            case "RIDE_INVITE":
                notifType = AppNotification.Type.RIDE_STATUS;
                break;
            case "SUPPORT":
                notifType = AppNotification.Type.SUPPORT_MESSAGE;
                break;
            default:
                notifType = AppNotification.Type.GENERAL;
                break;
        }
        AppNotification appNotif = new AppNotification(notifType, title, body);
        // Set backend notification ID so it can be deleted when clicked in notification panel
        if (notificationIdStr != null && !notificationIdStr.isEmpty()) {
            try {
                appNotif.setBackendId(Long.parseLong(notificationIdStr));
            } catch (NumberFormatException ignored) {}
        }
        if (rideIdStr != null && !rideIdStr.isEmpty()) {
            try {
                long parsedId = Long.parseLong(rideIdStr);
                if ("SUPPORT".equals(type)) {
                    appNotif.setChatId(parsedId);
                } else {
                    appNotif.setRideId(parsedId);
                }
            } catch (NumberFormatException ignored) {}
        }
        NotificationStore.getInstance().addNotification(appNotif);
    }

    // ════════════════════════════════════════════════════════════════════
    //  DEEP-LINK INTENT BUILDER
    // ════════════════════════════════════════════════════════════════════

    /**
     * Builds an Intent that deep-links into the appropriate screen.
     * <p>
     * For ride-related notifications, passes the {@code rideId} as an extra
     * so that {@code MainActivity} can navigate to the Active Ride fragment.
     */
    private Intent buildDeepLinkIntent(String type, String rideIdStr) {
        Intent intent = new Intent(this, MainActivity.class);

        if ("SUPPORT".equals(type)) {
            intent.putExtra("navigate_to", "support");
            // rideId field is reused as chatId for SUPPORT notifications from backend
            if (rideIdStr != null && !rideIdStr.isEmpty()) {
                try {
                    intent.putExtra("chatId", Long.parseLong(rideIdStr));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid chatId in FCM data: " + rideIdStr);
                }
            }
            return intent;
        }

        if ("LEAVE_REVIEW".equals(type)) {
            intent.putExtra("navigate_to", "review");
            if (rideIdStr != null && !rideIdStr.isEmpty()) {
                try {
                    intent.putExtra("rideId", Long.parseLong(rideIdStr));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid rideId in FCM data: " + rideIdStr);
                }
            }
            return intent;
        }

        if ("PANIC".equals(type)) {
            intent.putExtra("navigate_to", "admin_panic");
            return intent;
        }

        // Ride-related deep-links
        if (rideIdStr != null && !rideIdStr.isEmpty()) {
            try {
                long rideId = Long.parseLong(rideIdStr);
                intent.putExtra("rideId", rideId);

                // Finished/cancelled rides → ride history detail, others → active ride
                if ("RIDE_FINISHED".equals(type) || "RIDE_CANCELLED".equals(type)) {
                    intent.putExtra("navigate_to", "ride_history");
                } else {
                    intent.putExtra("navigate_to", "active_ride");
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid rideId in FCM data: " + rideIdStr);
            }
        }

        return intent;
    }

    // ════════════════════════════════════════════════════════════════════
    //  TOKEN SYNC HELPER
    // ════════════════════════════════════════════════════════════════════

    /**
     * Sends the FCM token to the backend. Called when:
     * 1. Token refreshes while user is logged in
     * 2. User logs in and token hasn't been synced yet
     */
    public static void syncTokenWithBackend(SharedPreferencesManager prefs, String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) return;

        Long userId = prefs.getUserId();
        String jwt = prefs.getToken();
        if (userId == null || userId <= 0 || jwt == null) {
            Log.w(TAG, "Cannot sync FCM token — user not logged in");
            return;
        }

        String authHeader = jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;
        FcmTokenRequest request = new FcmTokenRequest(fcmToken);

        ClientUtils.userService.updateFcmToken(userId, request, authHeader)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            prefs.setFcmTokenSynced(true);
                            Log.i(TAG, "FCM token synced with backend successfully");
                        } else {
                            Log.w(TAG, "FCM token sync failed — HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "FCM token sync network error: " + t.getMessage());
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════════
    //  PANIC ALARM SOUND
    // ════════════════════════════════════════════════════════════════════

    /**
     * Plays an urgent triple-beep alarm sound for PANIC notifications.
     * Uses ToneGenerator on a background thread so no audio file is needed.
     */
    private void playPanicAlarm() {
        new Thread(() -> {
            try {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                Thread.sleep(250);
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                Thread.sleep(250);
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                Thread.sleep(200);
                toneGen.release();
            } catch (Exception e) {
                Log.w(TAG, "Failed to play panic alarm sound", e);
            }
        }).start();
    }
}
