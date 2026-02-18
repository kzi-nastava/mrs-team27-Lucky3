# Notifications

> Three-layer notification system: WebSocket (foreground), FCM (background), and NotificationStore (unified state). Covers channels, subscriptions, deep-linking, and the in-app notification panel.

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     3-LAYER NOTIFICATION SYSTEM                     │
│                                                                     │
│  Layer 1: WebSocket (STOMP)                                         │
│  ├── Foreground real-time                                           │
│  ├── AppNotificationManager subscribes per role                     │
│  └── Creates AppNotification + system notification                  │
│                                                                     │
│  Layer 2: FCM (Firebase Cloud Messaging)                            │
│  ├── Background/killed state push                                   │
│  ├── MyFirebaseMessagingService handles data-only messages          │
│  └── Creates AppNotification + system notification                  │
│                                                                     │
│  Layer 3: NotificationStore (In-Memory State)                       │
│  ├── Central singleton for all notifications                        │
│  ├── Fed by both Layer 1 and Layer 2                                │
│  ├── Exposes LiveData for reactive UI updates                       │
│  └── Drives badge count + notification panel                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Notification Channels

Android notification channels (required API 26+):

| Channel ID | Name | Importance | LED Color | Description |
|------------|------|------------|-----------|-------------|
| `ride_updates` | Ride Updates | `HIGH` | Blue (`#2196F3`) | Status changes, cost updates |
| `panic_alerts` | Panic Alerts | `HIGH` | Red (`#F44336`) | Emergency panic alerts |
| `general_notifications` | General | `DEFAULT` | — | Support messages, general notifications |

Channels are created once in `AppNotificationManager.createNotificationChannels()` called from `start()`.

---

## 3. AppNotificationManager — WebSocket Subscription Hub

### Lifecycle

```
Login (MainActivity)
    │
    ▼
AppNotificationManager.start(context, userId, role)
    ├── Create notification channels
    ├── WebSocketManager.connect()
    ├── Subscribe based on role:
    │     ADMIN:     /topic/panic
    │                /topic/support/admin/messages
    │                /topic/support/admin/chats
    │     DRIVER:    /topic/support/user/{userId}/notification
    │     PASSENGER: /topic/support/user/{userId}/notification
    └── Store subscription IDs for cleanup
    
    ...app runs, receives real-time notifications...

Logout (MainActivity)
    │
    ▼
AppNotificationManager.stop()
    ├── Unsubscribe all stored subscription IDs
    ├── WebSocketManager.disconnect()
    └── Clear state
```

### Role-Based Subscriptions

| Role | Topic | Handler | Notification Type |
|------|-------|---------|-------------------|
| ADMIN | `/topic/panic` | `onPanicAlert()` | `PANIC_ALERT` |
| ADMIN | `/topic/support/admin/messages` | `onAdminSupportMessage()` | `SUPPORT_MESSAGE` |
| ADMIN | `/topic/support/admin/chats` | `onAdminChatListUpdate()` | *(no notification)* |
| DRIVER | `/topic/support/user/{userId}/notification` | `onUserSupportNotification()` | `SUPPORT_MESSAGE` |
| PASSENGER | `/topic/support/user/{userId}/notification` | `onUserSupportNotification()` | `SUPPORT_MESSAGE` |
| ALL | `/topic/ride/{id}` | `onRideUpdate()` | `RIDE_STATUS` |

### Dynamic Ride Subscription

```java
// Called when active ride polling discovers a ride
AppNotificationManager.getInstance().subscribeToRideUpdates(rideId);

// Subscribes to /topic/ride/{rideId}
// onRideUpdate() callback creates AppNotification + system notification
// Auto-unsubscribes on terminal states:
//   FINISHED, CANCELLED, CANCELLED_BY_DRIVER, CANCELLED_BY_PASSENGER, REJECTED
```

### Handler Flow (Per Notification Type)

```
WebSocket MESSAGE arrives
    │
    ▼
StompClient deserializes JSON → model object
    │
    ▼
Handler(mainLooper).post(callback)
    │
    ▼
AppNotificationManager handler method (e.g., onPanicAlert)
    │
    ├── 1. Create AppNotification (id, title, message, type, timestamp)
    │
    ├── 2. NotificationStore.getInstance().addNotification(appNotification)
    │      └── LiveData updated → badge + panel react
    │
    └── 3. postSystemNotification(title, message, type)
           └── Android NotificationManager.notify()
               ├── Channel selected by type
               ├── Deep-link PendingIntent (see §6)
               └── Auto-cancel on tap
```

---

## 4. FCM — Firebase Cloud Messaging

### Message Format (Data-Only)

```json
{
    "data": {
        "title": "Ride Accepted",
        "body": "Your ride #42 has been accepted by driver John",
        "type": "RIDE_STATUS",
        "rideId": "42"
    }
}
```

> Data-only messages ensure `onMessageReceived()` fires even in background/killed state. Notification-type FCM messages would be handled by the system tray directly, bypassing our custom logic.

### MyFirebaseMessagingService

```
FCM push received
    │
    ▼
onMessageReceived(RemoteMessage)
    │
    ├── Extract: title, body, type, rideId from data map
    │
    ├── Build system notification:
    │     ├── Channel: mapped by type (ride_updates / panic_alerts / general)
    │     ├── Icon: ic_notification
    │     ├── Deep-link intent: MainActivity with extras
    │     └── Auto-cancel: true
    │
    ├── NotificationManager.notify(uniqueId, notification)
    │
    └── Create AppNotification:
          ├── Map FCM type → AppNotification.Type:
          │     "PANIC"       → PANIC_ALERT
          │     "RIDE_STATUS" → RIDE_STATUS
          │     "RIDE_*"      → RIDE_STATUS
          │     default       → GENERAL
          │
          └── NotificationStore.getInstance().addNotification(appNotif)
              └── LiveData updated (visible if app comes to foreground)
```

### Token Lifecycle

| Event | Action |
|-------|--------|
| App install / token rotation | `onNewToken()` → save to SharedPreferences, sync if logged in |
| Login | `FirebaseMessaging.getToken()` → sync with backend via `PUT /api/users/{id}/fcm-token` |
| Token not yet synced | Flagged via `fcm_token_synced = false`, retried on next login |
| Logout | Token stays on backend (overwritten on next user's login) |

---

## 5. NotificationStore — Unified State

### Data Model

```java
public class AppNotification {
    private long id;              // Auto-incrementing
    private String title;         // "Panic Alert!"
    private String message;       // "Driver John triggered panic on ride #42"
    private Type type;            // Enum: RIDE_STATUS, PANIC_ALERT, SUPPORT_MESSAGE, GENERAL
    private LocalDateTime timestamp;
    private boolean read;         // false by default

    public enum Type {
        RIDE_STATUS,
        PANIC_ALERT,
        SUPPORT_MESSAGE,
        GENERAL
    }
}
```

### Store API

```java
public class NotificationStore {  // Singleton

    // Observable state
    LiveData<List<AppNotification>> getNotifications();  // newest first
    LiveData<Integer> getUnreadCount();                  // auto-computed

    // Mutations
    void addNotification(AppNotification notification);  // caps at 100, FIFO
    void markAsRead(long id);
    void markAllAsRead();
    void removeNotification(long id);
    void clearAll();
}
```

### Capacity & Eviction

- **Maximum**: 100 notifications
- **Order**: Newest first (prepend)
- **Eviction**: FIFO — oldest removed when exceeding 100
- **Persistence**: In-memory only — cleared on process death
- **Thread safety**: `MutableLiveData.postValue()` (any thread → main thread delivery)

---

## 6. Deep-Link Routing

System notifications include a `PendingIntent` that deep-links back into the app.

### Intent Extras

| Extra Key | Value | Purpose |
|-----------|-------|---------|
| `navigate_to` | `"active_ride"` / `"admin_panic"` / `"support"` / `"notifications"` | Target destination |
| `ride_id` | Long | Ride ID for ride-related notifications |

### Routing Logic (MainActivity.handleFcmDeepLink)

```
Intent received with "navigate_to" extra
    │
    ├── "active_ride"
    │     └── NavController.navigate(R.id.activeRideFragment)
    │         with Bundle { rideId = intent.getLongExtra("ride_id") }
    │
    ├── "admin_panic"
    │     └── NavController.navigate(R.id.adminPanicFragment)
    │
    ├── "support"
    │     └── NavController.navigate(R.id.supportFragment)
    │
    └── "notifications"
          └── NavController.navigate(R.id.notificationPanelFragment)
```

---

## 7. NotificationPanelFragment — UI

### Layout

```
┌────────────────────────────────────────┐
│  🔔 Notifications          [Mark All] │
├────────────────────────────────────────┤
│ ┌────────────────────────────────────┐ │
│ │ 🔴 Panic Alert!                   │ │
│ │    Driver triggered panic #42     │ │
│ │    2 minutes ago                  │ │
│ └────────────────────────────────────┘ │
│ ┌────────────────────────────────────┐ │
│ │    Ride Accepted                  │ │
│ │    Your ride has been accepted    │ │
│ │    5 minutes ago  ────── [✕]     │ │
│ └────────────────────────────────────┘ │
│ ┌────────────────────────────────────┐ │
│ │    Support Reply                  │ │
│ │    Admin responded to your query  │ │
│ │    1 hour ago                     │ │
│ └────────────────────────────────────┘ │
│                                        │
│       - No more notifications -        │
├────────────────────────────────────────┤
│            [Clear All]                 │
└────────────────────────────────────────┘
```

### Features

| Feature | Implementation |
|---------|----------------|
| **Badge count** | `LiveData<Integer>` observed in `MainActivity.setupNotificationBell()` — updates toolbar badge |
| **Real-time list** | `LiveData<List<AppNotification>>` observed by `NotificationPanelFragment` |
| **Mark as read** | Tap notification → `NotificationStore.markAsRead(id)` |
| **Mark all read** | "Mark All" button → `NotificationStore.markAllAsRead()` |
| **Delete single** | Swipe or ✕ button → `NotificationStore.removeNotification(id)` |
| **Clear all** | "Clear All" button → `NotificationStore.clearAll()` |
| **Empty state** | Shows placeholder text when no notifications |
| **Adapter** | `BaseAdapter` with ViewHolder pattern (per project convention) |

### Badge System

```
NotificationStore.unreadCount (LiveData)
    │
    │ observe() in MainActivity
    ▼
┌───────────────────────────────┐
│ if (count > 0)                │
│   badge.setVisible(true)      │
│   badge.setText(count > 99    │
│     ? "99+" : String(count))  │
│ else                          │
│   badge.setVisible(false)     │
└───────────────────────────────┘
```

---

## 8. Coverage Matrix

| Event | App State | Channel | In-App Panel | System Tray | Deep-Link |
|-------|-----------|---------|-------------|-------------|-----------|
| Ride status change | Foreground | WebSocket | ✅ | ✅ | `active_ride` |
| Ride status change | Background | FCM | ✅ | ✅ | `active_ride` |
| Ride status change | Killed | FCM | ✅ | ✅ | `active_ride` |
| Panic alert | Foreground | WebSocket | ✅ | ✅ | `admin_panic` |
| Panic alert | Background | FCM | ✅ | ✅ | `admin_panic` |
| Support message | Foreground | WebSocket | ✅ | ✅ | `support` |
| Support message | Background | FCM | ✅ | ✅ | `support` |
| Active ride exists | Foreground | Polling (15s) | — | — | — |
| Vehicle location | Foreground | Polling (10s) | — | — | — |
