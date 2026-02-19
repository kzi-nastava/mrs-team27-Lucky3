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
import com.example.mobile.models.NotificationResponseDTO;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.PanicResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.SupportChatListItemResponse;
import com.example.mobile.models.SupportMessageResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    /**
     * Returns true if the notification manager is actively listening via WebSocket.
     * Used by {@link com.example.mobile.services.MyFirebaseMessagingService} to
     * avoid duplicate notifications.
     */
    public boolean isStarted() {
        return started;
    }

    // Track active subscriptions so we can clean up
    private final List<String> activeSubscriptionIds = new ArrayList<>();

    // Track known panic IDs to avoid duplicates (mirrors old polling behavior)
    private final Set<Long> knownPanicIds = new HashSet<>();
    private boolean panicFirstLoad = true;

    // Track the ride ID we're subscribed to for ride updates
    private Long subscribedRideId = null;
    private String rideSubscriptionId = null;

    // Track last known ride status to avoid duplicate notifications
    // from periodic cost-tracking broadcasts that don't change the status
    private String lastKnownRideStatus = null;

    // Track completed stop indexes to detect new stop completions
    private Set<Integer> lastKnownCompletedStops = new HashSet<>();

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

        // Load persisted notification history from backend so that notifications
        // received while the app was completely killed appear in the panel.
        fetchNotificationHistory();

        // Connect WebSocket if not already connected
        if (WebSocketManager.getInstance().isConnected()) {
            // Already connected — subscribe immediately, skip connect callback
            Log.i(TAG, "WebSocket already connected, subscribing to topics");
            subscribeToTopics();
        } else {
            WebSocketManager.getInstance().connect(appContext, new StompClient.ConnectionCallback() {
                @Override
                public void onConnected() {
                    // On auto-reconnect, StompClient already resubscribes existing
                    // SubscriptionInfo entries at the STOMP level. Only call
                    // subscribeToTopics() on the FIRST connect (when activeSubscriptionIds
                    // is empty) to avoid creating duplicate subscriptions.
                    if (activeSubscriptionIds.isEmpty()) {
                        Log.i(TAG, "WebSocket connected, subscribing to topics");
                        subscribeToTopics();
                    } else {
                        Log.d(TAG, "WebSocket reconnected — StompClient resubscribed existing topics");
                    }
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

    // ======================== History Loader ========================

    /**
     * Fetch the most recent notifications from the backend and populate
     * {@link NotificationStore}. This ensures that notifications sent while
     * the app was completely killed appear in the notification panel.
     */
    private void fetchNotificationHistory() {
        if (appContext == null) return;

        SharedPreferencesManager prefs = new SharedPreferencesManager(appContext);
        String token = prefs.getToken();
        if (token == null || token.isEmpty()) return;

        ClientUtils.notificationService.getNotifications(
                "Bearer " + token, 0, 50, "timestamp,desc"
        ).enqueue(new Callback<PageResponse<NotificationResponseDTO>>() {
            @Override
            public void onResponse(Call<PageResponse<NotificationResponseDTO>> call,
                                   Response<PageResponse<NotificationResponseDTO>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getContent() == null) {
                    Log.w(TAG, "Failed to fetch notification history: " +
                            (response.code()));
                    return;
                }

                List<NotificationResponseDTO> dtos = response.body().getContent();
                if (dtos.isEmpty()) {
                    Log.d(TAG, "No notification history to load");
                    return;
                }

                // Convert backend DTOs → AppNotification and add to store
                // Reverse so oldest are added first (newest end up at the top)
                List<NotificationResponseDTO> reversed = new ArrayList<>(dtos);
                Collections.reverse(reversed);
                for (NotificationResponseDTO dto : reversed) {
                    NotificationStore.getInstance().addNotification(dto.toAppNotification());
                }

                Log.i(TAG, "Loaded " + dtos.size() + " notifications from history");
            }

            @Override
            public void onFailure(Call<PageResponse<NotificationResponseDTO>> call,
                                  Throwable t) {
                Log.e(TAG, "Failed to fetch notification history", t);
            }
        });
    }

    // ======================== Topic Subscriptions ========================

    private void subscribeToTopics() {
        if (!started || currentRole == null) return;

        // All roles subscribe to the personal notification queue so that
        // notifications delivered via sendNotification() on the backend
        // (RIDE_CREATED, RIDE_INVITE, DRIVER_ASSIGNMENT, etc.) are received
        // in real time without relying solely on FCM.
        subscribeToPersonalNotifications();

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
        lastKnownRideStatus = null;
        lastKnownCompletedStops = new HashSet<>();
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
            lastKnownRideStatus = null;
            lastKnownCompletedStops = new HashSet<>();
        }
    }

    /**
     * Returns true if the manager is currently subscribed to updates for the given ride.
     * Used by {@link com.example.mobile.services.MyFirebaseMessagingService} to
     * decide whether to skip an FCM notification (because WebSocket will handle it).
     */
    public boolean isSubscribedToRide(Long rideId) {
        return rideId != null && rideId.equals(subscribedRideId) && rideSubscriptionId != null;
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

    // ---- Personal Notification Queue (All roles) ----

    /**
     * Subscribe to the user's personal notification queue. The backend's
     * {@code sendNotification()} pushes every notification to
     * {@code /user/{id}/queue/notifications} via WebSocket <em>and</em> via FCM.
     * <p>
     * By subscribing here we guarantee real-time delivery even when FCM is
     * unavailable or delayed. Duplicates with role-specific topic handlers
     * (ride, support, panic) are suppressed inside
     * {@link #onPersonalNotification(NotificationResponseDTO)}.
     */
    private void subscribeToPersonalNotifications() {
        if (currentUserId == null) return;
        String destination = "/user/" + currentUserId + "/queue/notifications";
        String subId = WebSocketManager.getInstance().subscribe(
                destination, NotificationResponseDTO.class,
                this::onPersonalNotification);
        activeSubscriptionIds.add(subId);
        Log.d(TAG, "Subscribed to personal queue: " + destination);
    }

    /**
     * Handle a notification received on the personal WebSocket queue.
     * <p>
     * Types already handled by dedicated topic subscriptions are skipped to
     * avoid duplicate in-app notifications:
     * <ul>
     *     <li>{@code SUPPORT} — handled by user/admin support topics</li>
     *     <li>{@code PANIC} — handled by {@code /topic/panic} (admin)</li>
     *     <li>Ride-specific statuses — handled by {@code /topic/ride/{id}}
     *         <em>only when</em> we are subscribed to that ride</li>
     * </ul>
     */
    private void onPersonalNotification(NotificationResponseDTO dto) {
        if (dto == null) return;

        String type = dto.getType();
        Long relatedEntityId = dto.getRelatedEntityId();

        // Skip types handled by dedicated WebSocket topic subscriptions
        if ("SUPPORT".equals(type)) {
            Log.d(TAG, "Skipping personal-queue SUPPORT — handled by support topic");
            return;
        }
        if ("PANIC".equals(type)) {
            Log.d(TAG, "Skipping personal-queue PANIC — handled by panic topic");
            return;
        }

        // Skip ride lifecycle types when we're already subscribed to that ride's topic.
        // Terminal states (RIDE_FINISHED, RIDE_CANCELLED) are NOT suppressed here:
        // sendRideFinishedNotification() only pushes to the personal queue, never to
        // the ride topic, so we must always deliver them from this handler.
        if (relatedEntityId != null && isSubscribedToRide(relatedEntityId)) {
            switch (type != null ? type : "") {
                case "RIDE_STATUS":
                case "STOP_COMPLETED":
                    Log.d(TAG, "Skipping personal-queue " + type
                            + " for ride " + relatedEntityId + " — handled by ride topic");
                    return;
            }
        }

        // Convert backend DTO → in-app notification
        AppNotification notification = dto.toAppNotification();
        NotificationStore.getInstance().addNotification(notification);

        // Determine Android notification channel and deep-link target
        String channelId;
        String navigateTo;
        Long rideId = notification.getRideId();
        Long chatId = notification.getChatId();

        switch (notification.getType()) {
            case PANIC_ALERT:
                channelId = NotificationHelper.CHANNEL_PANIC_ALERTS;
                navigateTo = "admin_panic";
                break;
            case RIDE_CREATED:
            case RIDE_STATUS:
            case DRIVER_ASSIGNED:
            case STOP_COMPLETED:
                channelId = NotificationHelper.CHANNEL_RIDE_UPDATES;
                navigateTo = "active_ride";
                break;
            case RIDE_FINISHED:
                channelId = NotificationHelper.CHANNEL_RIDE_UPDATES;
                navigateTo = "ride_history";
                break;
            case RIDE_CANCELLED:
                channelId = NotificationHelper.CHANNEL_RIDE_UPDATES;
                navigateTo = "ride_history";
                break;
            case LEAVE_REVIEW:
                channelId = NotificationHelper.CHANNEL_GENERAL;
                navigateTo = "review";
                break;
            case SUPPORT_MESSAGE:
                channelId = NotificationHelper.CHANNEL_GENERAL;
                navigateTo = "support";
                break;
            default:
                channelId = NotificationHelper.CHANNEL_GENERAL;
                navigateTo = null;
                break;
        }

        postSystemNotification(notification.getTitle(), notification.getBody(),
                channelId, navigateTo, rideId, chatId);

        // Clean up ride subscription when a terminal event arrives from the personal queue
        // (backend only sends RIDE_FINISHED / RIDE_CANCELLED to this queue, not the ride topic)
        if ("RIDE_FINISHED".equals(type) || "RIDE_CANCELLED".equals(type)) {
            unsubscribeFromRideUpdates();
        }
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

        // ── Detect stop completion events ──
        // Compare completedStopIndexes with our last-known set to find newly completed stops.
        Set<Integer> currentCompleted = ride.getCompletedStopIndexes();
        if (currentCompleted == null) currentCompleted = new HashSet<>();

        Set<Integer> newlyCompleted = new HashSet<>(currentCompleted);
        newlyCompleted.removeAll(lastKnownCompletedStops);
        lastKnownCompletedStops = new HashSet<>(currentCompleted);

        if (!newlyCompleted.isEmpty()) {
            // Handle each newly completed stop
            for (Integer stopIndex : newlyCompleted) {
                String stopLabel;
                if (stopIndex == -1) {
                    stopLabel = "Start location";
                } else {
                    int displayNum = stopIndex + 1;
                    stopLabel = "Stop " + displayNum;
                    if (ride.getStops() != null && stopIndex < ride.getStops().size()
                            && ride.getStops().get(stopIndex) != null
                            && ride.getStops().get(stopIndex).getAddress() != null) {
                        stopLabel += " (" + ride.getStops().get(stopIndex).getAddress() + ")";
                    }
                }

                String title = "Stop Completed";
                String body = stopLabel + " has been completed.";

                AppNotification notification = new AppNotification(
                        AppNotification.Type.STOP_COMPLETED, title, body);
                notification.setRideId(ride.getId());

                NotificationStore.getInstance().addNotification(notification);
                postSystemNotification(title, body, NotificationHelper.CHANNEL_RIDE_UPDATES,
                        "active_ride", ride.getId());
            }

            // If only stops changed but status didn't, we're done — don't create a status notification
            if (status.equals(lastKnownRideStatus)) return;
        }

        // ── Handle ride status changes ──
        // Skip repeated broadcasts with the same status (e.g. periodic cost-tracking
        // updates that don't change the ride status)
        if (status.equals(lastKnownRideStatus)) return;
        lastKnownRideStatus = status;

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
            case "CANCELLED":
            case "CANCELLED_BY_DRIVER":
            case "CANCELLED_BY_PASSENGER":
            case "REJECTED":
                // Terminal states: unsubscribe here and let the personal queue deliver
                // the notification. sendRideFinishedNotification() only pushes to the
                // personal queue, so delivering from onRideUpdate() would cause duplicates
                // if the ride topic also fires, and would miss it otherwise.
                unsubscribeFromRideUpdates();
                return;
            case "PANIC":
                title = "\uD83D\uDEA8 Panic Activated";
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

        // Note: WebSocket topics only deliver messages sent AFTER subscription,
        // so every message is a genuine new panic — no need to skip any.

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
                "support", null, msg.getChatId());
    }

    // ======================== System Notifications ========================

    private void postSystemNotification(String title, String body, String channelId,
                                        String navigateTo, Long rideId) {
        postSystemNotification(title, body, channelId, navigateTo, rideId, null);
    }

    private void postSystemNotification(String title, String body, String channelId,
                                        String navigateTo, Long rideId, Long chatId) {
        if (appContext == null) return;

        // Play in-app sound when foregrounded — or ALWAYS for panic alerts
        // (panic alarm must be audible regardless of app state)
        if (AppLifecycleTracker.isAppInForeground()
                || NotificationHelper.CHANNEL_PANIC_ALERTS.equals(channelId)) {
            playInAppSound(channelId);
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
                        .setColor(Color.RED)
                        .setColorized(true)
                        .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                        .setLights(Color.RED, 1000, 300)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setOngoing(true);
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
                int volume = NotificationHelper.CHANNEL_PANIC_ALERTS.equals(channelId)
                        ? ToneGenerator.MAX_VOLUME   // 100 — maximum urgency for panic
                        : 80;
                ToneGenerator toneGen = new ToneGenerator(stream, volume);

                if (NotificationHelper.CHANNEL_PANIC_ALERTS.equals(channelId)) {
                    // Urgent five-beep pattern for panic (more insistent than the old triple)
                    for (int i = 0; i < 5; i++) {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 200);
                        Thread.sleep(350);
                    }
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
