package com.example.mobile.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mobile.MainActivity;
import com.example.mobile.R;
import com.example.mobile.models.AppNotification;
import com.example.mobile.models.PanicResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.SupportChatListItemResponse;
import com.example.mobile.models.SupportMessageResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central notification manager that subscribes to relevant WebSocket topics
 * based on the user's role, converts incoming messages into in-app
 * {@link AppNotification} items, stores them in {@link NotificationStore},
 * and posts Android system notifications.
 * <p>
 * Lifecycle: called from {@link MainActivity} on login (start) and logout (stop).
 * <p>
 * Replaces the old polling-based panic notification system with real-time
 * WebSocket subscriptions while keeping HTTP polling as a fallback if
 * WebSocket is unavailable.
 * <p>
 * <b>Topics subscribed per role:</b>
 * <ul>
 *   <li>DRIVER: {@code /topic/ride/{activeRideId}} (ride status changes)</li>
 *   <li>PASSENGER: {@code /topic/ride/{activeRideId}} (ride status changes),
 *       {@code /topic/support/user/{userId}/notification} (support replies)</li>
 *   <li>ADMIN: {@code /topic/panic} (panic alerts),
 *       {@code /topic/support/admin/messages} (new support messages),
 *       {@code /topic/support/admin/chats} (chat list updates)</li>
 * </ul>
 */
public class AppNotificationManager {

    private static final String TAG = "AppNotificationManager";
    private static final int SYSTEM_NOTIF_BASE_ID = 5000;

    private static volatile AppNotificationManager instance;

    private Context appContext;
    private String currentRole;
    private Long currentUserId;
    private boolean started = false;

    // Track active subscriptions so we can clean up
    private final List<String> activeSubscriptionIds = new ArrayList<>();

    // Track known panic IDs to avoid duplicates (mirrors old polling behavior)
    private final Set<Long> knownPanicIds = new HashSet<>();
    private boolean panicFirstLoad = true;

    // Track the ride ID we're subscribed to for ride updates
    private Long subscribedRideId = null;
    private String rideSubscriptionId = null;

    private int systemNotifCounter = 0;

    private AppNotificationManager() {}

    public static AppNotificationManager getInstance() {
        if (instance == null) {
            synchronized (AppNotificationManager.class) {
                if (instance == null) {
                    instance = new AppNotificationManager();
                }
            }
        }
        return instance;
    }

    // ======================== Lifecycle ========================

    /**
     * Start listening for notifications. Call after login.
     *
     * @param context Application context
     * @param role    User role (DRIVER, PASSENGER, ADMIN)
     * @param userId  Current user's ID
     */
    public void start(Context context, String role, Long userId) {
        if (started) stop();

        this.appContext = context.getApplicationContext();
        this.currentRole = role;
        this.currentUserId = userId;
        this.started = true;
        this.panicFirstLoad = true;
        this.knownPanicIds.clear();

        Log.i(TAG, "Starting notification manager for role=" + role + " userId=" + userId);

        // Connect WebSocket if not already connected
        WebSocketManager.getInstance().connect(appContext, new StompClient.ConnectionCallback() {
            @Override
            public void onConnected() {
                Log.i(TAG, "WebSocket connected, subscribing to topics");
                subscribeToTopics();
            }

            @Override
            public void onDisconnected(String reason) {
                Log.w(TAG, "WebSocket disconnected: " + reason);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "WebSocket error: " + error);
            }
        });

        // If already connected, subscribe immediately
        if (WebSocketManager.getInstance().isConnected()) {
            subscribeToTopics();
        }
    }

    /**
     * Stop all subscriptions. Call on logout.
     */
    public void stop() {
        Log.i(TAG, "Stopping notification manager");
        started = false;
        unsubscribeAll();
        WebSocketManager.getInstance().disconnect();
        currentRole = null;
        currentUserId = null;
        subscribedRideId = null;
        rideSubscriptionId = null;
        panicFirstLoad = true;
        knownPanicIds.clear();
    }

    // ======================== Topic Subscriptions ========================

    private void subscribeToTopics() {
        if (!started || currentRole == null) return;

        switch (currentRole) {
            case "ADMIN":
                subscribeToPanic();
                subscribeToAdminSupportMessages();
                subscribeToAdminChatList();
                break;
            case "PASSENGER":
                subscribeToUserSupportNotifications();
                break;
            case "DRIVER":
                subscribeToUserSupportNotifications();
                break;
        }
    }

    /**
     * Subscribe to ride updates for a specific ride.
     * Call when an active ride is detected (from polling or status change).
     */
    public void subscribeToRideUpdates(Long rideId) {
        if (rideId == null || rideId.equals(subscribedRideId)) return;

        // Unsub from old ride if any
        if (rideSubscriptionId != null) {
            WebSocketManager.getInstance().unsubscribe(rideSubscriptionId);
            activeSubscriptionIds.remove(rideSubscriptionId);
            rideSubscriptionId = null;
        }

        subscribedRideId = rideId;
        String destination = "/topic/ride/" + rideId;
        rideSubscriptionId = WebSocketManager.getInstance().subscribe(
                destination, RideResponse.class, this::onRideUpdate);
        activeSubscriptionIds.add(rideSubscriptionId);
        Log.d(TAG, "Subscribed to ride updates: " + destination);
    }

    /**
     * Unsubscribe from ride updates (e.g. ride finished).
     */
    public void unsubscribeFromRideUpdates() {
        if (rideSubscriptionId != null) {
            WebSocketManager.getInstance().unsubscribe(rideSubscriptionId);
            activeSubscriptionIds.remove(rideSubscriptionId);
            rideSubscriptionId = null;
            subscribedRideId = null;
        }
    }

    // ---- Panic (Admin) ----

    private void subscribeToPanic() {
        String subId = WebSocketManager.getInstance().subscribe(
                "/topic/panic", PanicResponse.class, this::onPanicAlert);
        activeSubscriptionIds.add(subId);
        Log.d(TAG, "Subscribed to /topic/panic");
    }

    // ---- Support Messages (Admin) ----

    private void subscribeToAdminSupportMessages() {
        String subId = WebSocketManager.getInstance().subscribe(
                "/topic/support/admin/messages", SupportMessageResponse.class,
                this::onAdminSupportMessage);
        activeSubscriptionIds.add(subId);
        Log.d(TAG, "Subscribed to /topic/support/admin/messages");
    }

    // ---- Admin Chat List ----

    private void subscribeToAdminChatList() {
        String subId = WebSocketManager.getInstance().subscribe(
                "/topic/support/admin/chats", SupportChatListItemResponse.class,
                this::onAdminChatListUpdate);
        activeSubscriptionIds.add(subId);
        Log.d(TAG, "Subscribed to /topic/support/admin/chats");
    }

    // ---- User Support Notifications (Passenger/Driver) ----

    private void subscribeToUserSupportNotifications() {
        if (currentUserId == null) return;
        String destination = "/topic/support/user/" + currentUserId + "/notification";
        String subId = WebSocketManager.getInstance().subscribe(
                destination, SupportMessageResponse.class,
                this::onUserSupportNotification);
        activeSubscriptionIds.add(subId);
        Log.d(TAG, "Subscribed to " + destination);
    }

    private void unsubscribeAll() {
        WebSocketManager wsm = WebSocketManager.getInstance();
        for (String subId : activeSubscriptionIds) {
            try {
                wsm.unsubscribe(subId);
            } catch (Exception e) {
                Log.w(TAG, "Error unsubscribing " + subId, e);
            }
        }
        activeSubscriptionIds.clear();
        rideSubscriptionId = null;
    }

    // ======================== Message Handlers ========================

    private void onRideUpdate(RideResponse ride) {
        if (ride == null) return;
        String status = ride.getStatus();
        if (status == null) return;

        String title;
        String body;
        switch (status) {
            case "ACCEPTED":
                title = "Ride Accepted";
                body = "Your ride has been accepted by the driver.";
                break;
            case "IN_PROGRESS":
                title = "Ride Started";
                body = "Your ride is now in progress.";
                break;
            case "FINISHED":
                title = "Ride Completed";
                body = "Your ride has been completed. Total cost: " +
                        String.format("%.0f RSD", ride.getEffectiveCost());
                unsubscribeFromRideUpdates();
                break;
            case "CANCELLED":
            case "CANCELLED_BY_DRIVER":
                title = "Ride Cancelled";
                body = "Your ride has been cancelled by the driver.";
                unsubscribeFromRideUpdates();
                break;
            case "CANCELLED_BY_PASSENGER":
                title = "Ride Cancelled";
                body = "The ride has been cancelled.";
                unsubscribeFromRideUpdates();
                break;
            case "REJECTED":
                title = "Ride Rejected";
                body = "Your ride request was rejected.";
                if (ride.getRejectionReason() != null) {
                    body += " Reason: " + ride.getRejectionReason();
                }
                unsubscribeFromRideUpdates();
                break;
            case "PANIC":
                title = "\u26A0 Panic Activated";
                body = "Panic button was pressed on your ride!";
                break;
            default:
                title = "Ride Update";
                body = "Ride status changed to: " + status;
                break;
        }

        AppNotification.Type notifType;
        String navigateTo;

        switch (status) {
            case "FINISHED":
                notifType = AppNotification.Type.RIDE_FINISHED;
                navigateTo = "ride_history";
                break;
            case "CANCELLED":
            case "CANCELLED_BY_DRIVER":
            case "CANCELLED_BY_PASSENGER":
                notifType = AppNotification.Type.RIDE_CANCELLED;
                navigateTo = "ride_history";
                break;
            default:
                notifType = AppNotification.Type.RIDE_STATUS;
                navigateTo = "active_ride";
                break;
        }

        AppNotification notification = new AppNotification(notifType, title, body);
        notification.setRideId(ride.getId());

        NotificationStore.getInstance().addNotification(notification);
        postSystemNotification(title, body, NotificationHelper.CHANNEL_RIDE_UPDATES,
                navigateTo, ride.getId());
    }

    private void onPanicAlert(PanicResponse panic) {
        if (panic == null || panic.getId() == null) return;

        // Deduplicate (same logic as old polling)
        if (knownPanicIds.contains(panic.getId())) return;
        knownPanicIds.add(panic.getId());

        // Skip notifications on first load (initial state sync)
        if (panicFirstLoad) {
            panicFirstLoad = false;
            return;
        }

        String userName = panic.getUser() != null ? panic.getUser().getFullName() : "Unknown";
        Long rideId = panic.getRide() != null ? panic.getRide().getId() : null;

        String title = "\uD83D\uDEA8 PANIC ALERT";
        String body = userName + " triggered panic on ride #" +
                (rideId != null ? rideId : "?");
        if (panic.getReason() != null && !panic.getReason().trim().isEmpty()) {
            body += "\nReason: " + panic.getReason();
        }

        AppNotification notification = new AppNotification(
                AppNotification.Type.PANIC_ALERT, title, body);
        notification.setRideId(rideId);

        NotificationStore.getInstance().addNotification(notification);
        postSystemNotification(title, body, NotificationHelper.CHANNEL_PANIC_ALERTS,
                "admin_panic", null);
    }

    private void onAdminSupportMessage(SupportMessageResponse msg) {
        if (msg == null) return;
        // Only notify for user messages (not admin's own)
        if (msg.isFromAdmin()) return;

        String senderName = msg.getSenderName() != null ? msg.getSenderName() : "User";
        String title = "Support: " + senderName;
        String body = msg.getContent() != null ? msg.getContent() : "New message";

        AppNotification notification = new AppNotification(
                AppNotification.Type.SUPPORT_MESSAGE, title, body);
        notification.setChatId(msg.getChatId());

        NotificationStore.getInstance().addNotification(notification);
        postSystemNotification(title, body, NotificationHelper.CHANNEL_GENERAL,
                "support", null, msg.getChatId());
    }

    private void onAdminChatListUpdate(SupportChatListItemResponse chatItem) {
        // Chat list update — we use this for badge count awareness but
        // the actual notification is handled by onAdminSupportMessage.
        // No need for a separate notification here.
        Log.d(TAG, "Admin chat list updated: " + (chatItem != null ? chatItem.getId() : "null"));
    }

    private void onUserSupportNotification(SupportMessageResponse msg) {
        if (msg == null) return;
        // For non-admin users, only notify about admin replies
        if (!msg.isFromAdmin()) return;

        String title = "Support Reply";
        String body = msg.getContent() != null ? msg.getContent() : "New message from support";

        AppNotification notification = new AppNotification(
                AppNotification.Type.SUPPORT_MESSAGE, title, body);
        notification.setChatId(msg.getChatId());

        NotificationStore.getInstance().addNotification(notification);
        postSystemNotification(title, body, NotificationHelper.CHANNEL_GENERAL,
                "support", null, null);
    }

    // ======================== System Notifications ========================

    private void postSystemNotification(String title, String body, String channelId,
                                        String navigateTo, Long rideId) {
        postSystemNotification(title, body, channelId, navigateTo, rideId, null);
    }

    private void postSystemNotification(String title, String body, String channelId,
                                        String navigateTo, Long rideId, Long chatId) {
        if (appContext == null) return;

        // When the app is in the foreground, play an in-app sound instead
        // of posting an Android system notification.
        if (AppLifecycleTracker.isAppInForeground()) {
            playInAppSound(channelId);
            return;
        }

        try {
            Intent intent = new Intent(appContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("navigate_to", navigateTo);
            if (rideId != null) {
                intent.putExtra("rideId", rideId);
            }
            if (chatId != null) {
                intent.putExtra("chatId", chatId);
            }

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    appContext, systemNotifCounter, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // Channel-specific styling
            if (NotificationHelper.CHANNEL_PANIC_ALERTS.equals(channelId)) {
                builder.setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                        .setLights(Color.RED, 1000, 300)
                        .setCategory(NotificationCompat.CATEGORY_ALARM);
            } else if (NotificationHelper.CHANNEL_RIDE_UPDATES.equals(channelId)) {
                builder.setVibrate(new long[]{0, 300, 150, 300})
                        .setLights(Color.BLUE, 1000, 300);
            }

            int notifId = SYSTEM_NOTIF_BASE_ID + (systemNotifCounter++ % 50);
            NotificationManagerCompat.from(appContext).notify(notifId, builder.build());

        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to post system notification", e);
        }
    }

    // ======================== In-App Sound ========================

    /**
     * Plays a short in-app notification sound on a background thread.
     * Uses {@link ToneGenerator} so no audio file is needed.
     * <p>
     * Panic alerts get an urgent three-beep pattern; everything else
     * gets a single short "ding".
     */
    private void playInAppSound(String channelId) {
        new Thread(() -> {
            try {
                int stream = NotificationHelper.CHANNEL_PANIC_ALERTS.equals(channelId)
                        ? AudioManager.STREAM_ALARM
                        : AudioManager.STREAM_NOTIFICATION;
                ToneGenerator toneGen = new ToneGenerator(stream, 80);

                if (NotificationHelper.CHANNEL_PANIC_ALERTS.equals(channelId)) {
                    // Urgent triple-beep for panic
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                    Thread.sleep(250);
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                    Thread.sleep(250);
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                    Thread.sleep(200);
                } else {
                    // Single short tone for normal notifications
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                    Thread.sleep(250);
                }
                toneGen.release();
            } catch (Exception e) {
                Log.w(TAG, "Failed to play in-app notification sound", e);
            }
        }).start();
    }
}
