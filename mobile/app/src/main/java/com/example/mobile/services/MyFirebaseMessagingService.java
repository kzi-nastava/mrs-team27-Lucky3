package com.example.mobile.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mobile.MainActivity;
import com.example.mobile.R;
import com.example.mobile.models.FcmTokenRequest;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.NotificationHelper;
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

        Log.d(TAG, "FCM data message received — type=" + type + ", rideId=" + rideIdStr);

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
            case "DRIVER_ASSIGNMENT":
                channelId = NotificationHelper.CHANNEL_RIDE_UPDATES;
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
            builder.setVibrate(new long[]{0, 500, 200, 500, 200, 500});
        } else if (channelId.equals(NotificationHelper.CHANNEL_RIDE_UPDATES)) {
            builder.setVibrate(new long[]{0, 300, 150, 300});
        }

        // Expandable text for long messages
        if (body.length() > 50) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        // Display the notification
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted — cannot show notification");
        }
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

        if (rideIdStr != null && !rideIdStr.isEmpty()) {
            try {
                long rideId = Long.parseLong(rideIdStr);
                intent.putExtra("navigate_to", "active_ride");
                intent.putExtra("rideId", rideId);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid rideId in FCM data: " + rideIdStr);
            }
        }

        if ("PANIC".equals(type)) {
            intent.putExtra("navigate_to", "admin_panic");
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
}
