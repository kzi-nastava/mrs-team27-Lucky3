package com.example.mobile.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

/**
 * Centralized notification channel management.
 * <p>
 * Creates all notification channels required by the app on Android O+.
 * Channel IDs must match what the backend sends in FCM push payloads
 * and what {@code MyFirebaseMessagingService} uses when building notifications.
 */
public class NotificationHelper {

    /** Channel for ride lifecycle events (accepted, started, finished, cancelled). */
    public static final String CHANNEL_RIDE_UPDATES = "ride_updates";

    /** Channel for panic alerts (high priority, vibration). */
    public static final String CHANNEL_PANIC_ALERTS = "panic_alerts";

    /** Channel for general notifications (support messages, invites). */
    public static final String CHANNEL_GENERAL = "general_notifications";

    private NotificationHelper() {
        // utility class
    }

    /**
     * Creates all notification channels. Safe to call multiple times —
     * the system ignores channels that already exist.
     *
     * @param context Application or Activity context
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        // 1. Ride Updates — default importance, sound + badge
        NotificationChannel rideChannel = new NotificationChannel(
                CHANNEL_RIDE_UPDATES,
                "Ride Updates",
                NotificationManager.IMPORTANCE_HIGH);
        rideChannel.setDescription("Notifications about ride status changes (accepted, started, finished, cancelled)");
        rideChannel.enableVibration(true);
        rideChannel.setVibrationPattern(new long[]{0, 300, 150, 300});
        rideChannel.enableLights(true);
        rideChannel.setLightColor(Color.BLUE);
        manager.createNotificationChannel(rideChannel);

        // 2. Panic Alerts — high importance, persistent vibration
        NotificationChannel panicChannel = new NotificationChannel(
                CHANNEL_PANIC_ALERTS,
                "Panic Alerts",
                NotificationManager.IMPORTANCE_HIGH);
        panicChannel.setDescription("Emergency panic alert notifications from rides");
        panicChannel.enableVibration(true);
        panicChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
        panicChannel.enableLights(true);
        panicChannel.setLightColor(Color.RED);
        manager.createNotificationChannel(panicChannel);

        // 3. General — default importance
        NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT);
        generalChannel.setDescription("Support messages, ride invitations, and other notifications");
        manager.createNotificationChannel(generalChannel);
    }
}
