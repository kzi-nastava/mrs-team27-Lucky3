# Data Flow

> Complete data flow diagrams for backend communication, WebSocket real-time updates, HTTP polling, FCM push notifications, and in-app state management.

---

## 1. System Overview — All Communication Channels

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                             ANDROID APP                                      │
│                                                                              │
│  ┌──────────────┐  ┌──────────────────┐  ┌───────────────┐  ┌────────────┐  │
│  │   Fragments   │  │   ViewModels     │  │  Singletons   │  │   FCM      │  │
│  │  (UI Layer)   │  │  (State Layer)   │  │  (Infra)      │  │  Service   │  │
│  │               │  │                  │  │               │  │            │  │
│  │ ┌───────────┐ │  │ ┌──────────────┐ │  │ ┌───────────┐ │  │ MyFirebase │  │
│  │ │ Active    │ │  │ │ LoginVM      │ │  │ │ ClientUtils│ │  │ Messaging  │  │
│  │ │ Ride      │ │  │ │ RegisterVM   │ │  │ │ (Retrofit) │ │  │ Service    │  │
│  │ │ Fragment  │ │  │ │ PassengerVM  │ │  │ ├───────────┤ │  │            │  │
│  │ ├───────────┤ │  │ │ AdminVM...   │ │  │ │ WebSocket │ │  └─────┬──────┘  │
│  │ │ Guest     │ │  │ └──────┬───────┘ │  │ │ Manager   │ │        │         │
│  │ │ Home      │ │  │        │         │  │ ├───────────┤ │        │         │
│  │ ├───────────┤ │  │        │         │  │ │ AppNotif  │ │        │         │
│  │ │ Notif     │ │  │        │         │  │ │ Manager   │ │        │         │
│  │ │ Panel     │◀┼──┼──LiveData────────┼──┤ ├───────────┤ │        │         │
│  │ └───────────┘ │  │        │         │  │ │ Notif     │◀┼────────┘         │
│  │               │  │        │         │  │ │ Store     │ │   addNotif()     │
│  └───────┬───────┘  └────────┼─────────┘  │ └─────┬─────┘ │                  │
│          │                   │             │       │       │                  │
│          │  User Actions     │ API Calls   │       │       │                  │
└──────────┼───────────────────┼─────────────┼───────┼───────┼──────────────────┘
           │                   │             │       │       │
           ▼                   ▼             ▼       │       │
┌──────────────────────────────────────────────┐     │       │
│              NETWORK LAYER                    │     │       │
│                                               │     │       │
│  ┌────────────┐ ┌────────────┐ ┌───────────┐ │     │       │
│  │  REST API  │ │  WebSocket │ │ OSRM API  │ │     │       │
│  │  (Retrofit)│ │  (STOMP)   │ │ (Routing) │ │     │       │
│  │  Port 8081 │ │  Port 8081 │ │ External  │ │     │       │
│  └─────┬──────┘ └─────┬──────┘ └─────┬─────┘ │     │       │
└────────┼──────────────┼──────────────┼────────┘     │       │
         │              │              │              │       │
         ▼              ▼              ▼              │       │
┌─────────────────────────────────────────────────────┤       │
│              SPRING BOOT BACKEND                     │       │
│                                                      │       │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────┐  │       │
│  │ REST     │  │ WebSocket    │  │ FCM Service   │──┼───────┘
│  │ Controllers│ │ Broadcasters │  │ (Server Push) │  │
│  │ 13 total │  │ (STOMP/SockJS)│  └───────────────┘  │
│  └──────────┘  └──────────────┘                      │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │            PostgreSQL Database                  │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

---

## 2. REST API Data Flow (Retrofit)

Standard HTTP request/response cycle for CRUD operations.

```
┌───────────┐        ┌──────────┐        ┌──────────┐       ┌─────────┐
│ Fragment   │        │ ViewModel│        │ Retrofit │       │ Backend │
│ (UI)       │        │          │        │ Service  │       │  API    │
└─────┬─────┘        └────┬─────┘        └────┬─────┘       └────┬────┘
      │                    │                   │                  │
      │  user action       │                   │                  │
      │ (button click)     │                   │                  │
      ├───────────────────▶│                   │                  │
      │                    │  Call<T>.enqueue() │                  │
      │                    ├──────────────────▶│                  │
      │                    │                   │  HTTP Request    │
      │                    │                   │  + Bearer JWT    │
      │                    │                   ├─────────────────▶│
      │                    │                   │                  │
      │                    │                   │  JSON Response   │
      │                    │                   │◀─────────────────┤
      │                    │  Callback.onResponse()              │
      │                    │◀──────────────────┤                  │
      │                    │                   │                  │
      │                    │  MutableLiveData  │                  │
      │                    │  .postValue()     │                  │
      │                    │                   │                  │
      │  LiveData.observe()│                   │                  │
      │◀───────────────────┤                   │                  │
      │                    │                   │                  │
      │  Update UI         │                   │                  │
      │                    │                   │                  │

Token attachment: @Header("Authorization") String token
    ├─ Retrieved from SharedPreferencesManager.getToken()
    └─ Prepended with "Bearer " at call site
```

### Request Types

| Type | Content-Type | Example |
|------|-------------|---------|
| JSON body | `application/json` | `@Body LoginRequest` |
| Multipart | `multipart/form-data` | `@Part("request") RequestBody` (JSON blob) + `@Part MultipartBody.Part` (image) |
| Query params | URL-encoded | `@Query("status") String status` |
| Path params | URL path | `@Path("id") Long id` |
| No body | — | `GET` requests with `@Header` auth only |

---

## 3. WebSocket Data Flow (STOMP over OkHttp)

Real-time bidirectional communication for live updates.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        WebSocket Connection Lifecycle                    │
└─────────────────────────────────────────────────────────────────────────┘

  Login                                                       Logout
    │                                                           │
    ▼                                                           ▼
┌──────────┐     ┌───────────────┐     ┌──────────┐     ┌───────────┐
│ AppNotif │────▶│ WebSocket     │────▶│ STOMP    │     │ Disconnect│
│ Manager  │     │ Manager       │     │ Client   │     │ + Unsub   │
│ .start() │     │ .connect()    │     │ .connect()│     │ All       │
└──────────┘     └───────────────┘     └────┬─────┘     └───────────┘
                                            │
                 OkHttp WebSocket           │
                 ws://<IP>:8081/ws/websocket │
                                            │
                                            ▼
                                  ┌──────────────────┐
                                  │  STOMP CONNECT    │
                                  │  Headers:         │
                                  │    accept-version:│
                                  │      1.1          │
                                  │    Authorization: │
                                  │      Bearer <JWT> │
                                  │    heart-beat:    │
                                  │      10000,10000  │
                                  └────────┬─────────┘
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │  STOMP CONNECTED  │
                                  │  (Server ACK)     │
                                  └────────┬─────────┘
                                           │
                              ┌────────────┼────────────┐
                              │            │            │
                              ▼            ▼            ▼
                        ┌──────────┐ ┌──────────┐ ┌──────────┐
                        │SUBSCRIBE │ │SUBSCRIBE │ │SUBSCRIBE │
                        │/topic/   │ │/topic/   │ │/topic/   │
                        │panic     │ │ride/{id} │ │support/* │
                        └──────────┘ └──────────┘ └──────────┘
```

### STOMP Message Flow

```
┌─────────┐          ┌───────────┐          ┌───────────────┐
│ Backend │          │ StompClient│          │ AppNotif      │
│ Broad-  │          │            │          │ Manager       │
│ caster  │          │            │          │ (Handlers)    │
└────┬────┘          └─────┬──────┘          └──────┬────────┘
     │                     │                        │
     │  STOMP MESSAGE      │                        │
     │  destination:       │                        │
     │  /topic/panic       │                        │
     │  body: JSON         │                        │
     ├────────────────────▶│                        │
     │                     │                        │
     │                     │  Gson.fromJson(         │
     │                     │    body,                │
     │                     │    PanicResponse.class)  │
     │                     │                        │
     │                     │  Handler(mainLooper)   │
     │                     │  .post(callback)       │
     │                     ├───────────────────────▶│
     │                     │                        │
     │                     │               ┌────────┤
     │                     │               │ 1. Create AppNotification
     │                     │               │ 2. NotificationStore.addNotification()
     │                     │               │ 3. postSystemNotification()
     │                     │               └────────┤
     │                     │                        │
     │                     │                        ▼
     │                     │               ┌──────────────────┐
     │                     │               │ NotificationStore│
     │                     │               │ .notifications   │
     │                     │               │ (LiveData)       │
     │                     │               └────────┬─────────┘
     │                     │                        │
     │                     │                        │ observe()
     │                     │                        ▼
     │                     │               ┌──────────────────┐
     │                     │               │ UI Updates:      │
     │                     │               │ - Badge count    │
     │                     │               │ - Panel list     │
     │                     │               └──────────────────┘
```

### Reconnection Strategy

```
Disconnect detected
    │
    ▼
┌──────────────────────────────────────────────┐
│            Exponential Backoff                │
│                                              │
│  Attempt 1: wait 3s   → try connect         │
│  Attempt 2: wait 6s   → try connect         │
│  Attempt 3: wait 12s  → try connect         │
│  Attempt 4: wait 24s  → try connect         │
│  Attempt 5: wait 30s  → try connect (cap)   │
│  ...                                         │
│  Attempt 10: wait 30s → try connect          │
│  Attempt 11: GIVE UP (max retries reached)   │
│                                              │
│  On successful reconnect:                    │
│    → Re-subscribe ALL active subscriptions   │
│    → Callbacks resume automatically          │
└──────────────────────────────────────────────┘
```

### Topic Subscriptions by Role

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADMIN                                         │
│                                                                 │
│  /topic/panic ──────────────────────▶ onPanicAlert()            │
│  /topic/support/admin/messages ─────▶ onAdminSupportMessage()   │
│  /topic/support/admin/chats ────────▶ onAdminChatListUpdate()   │
│  /topic/ride/{id} ──────────────────▶ onRideUpdate() (dynamic)  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    PASSENGER                                     │
│                                                                 │
│  /topic/support/user/{userId}/notification ──▶ onUserSupportNotification()
│  /topic/ride/{id} ──────────────────────────▶ onRideUpdate() (dynamic)
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    DRIVER                                        │
│                                                                 │
│  /topic/support/user/{userId}/notification ──▶ onUserSupportNotification()
│  /topic/ride/{id} ──────────────────────────▶ onRideUpdate() (dynamic)
└─────────────────────────────────────────────────────────────────┘

Dynamic subscription: /topic/ride/{id} is subscribed when active ride polling
discovers an active ride. Auto-unsubscribes on terminal statuses (FINISHED,
CANCELLED, REJECTED).
```

---

## 4. FCM Push Notification Data Flow

Server-initiated push for background/killed app states.

```
┌──────────┐        ┌──────────┐        ┌──────────────┐     ┌──────────┐
│ Backend  │        │ Google   │        │ FCM Service  │     │ Notif    │
│ FcmSvc   │        │ FCM      │        │ (Android)    │     │ Store    │
└────┬─────┘        └────┬─────┘        └──────┬───────┘     └────┬─────┘
     │                   │                     │                  │
     │  data-only msg    │                     │                  │
     │  {title, body,    │                     │                  │
     │   type, rideId}   │                     │                  │
     ├──────────────────▶│                     │                  │
     │                   │  Push to device     │                  │
     │                   ├────────────────────▶│                  │
     │                   │                     │                  │
     │                   │   onMessageReceived()                  │
     │                   │                     │                  │
     │                   │                     │──── Build system │
     │                   │                     │     notification │
     │                   │                     │     with deep-link
     │                   │                     │                  │
     │                   │                     │──── Map FCM type │
     │                   │                     │     to AppNotif  │
     │                   │                     │     Type enum    │
     │                   │                     │                  │
     │                   │                     │  addNotification()
     │                   │                     ├─────────────────▶│
     │                   │                     │                  │
     │                   │                     │            ┌─────┤
     │                   │                     │            │LiveData
     │                   │                     │            │postValue()
     │                   │                     │            └─────┤
     │                   │                     │                  │
     │                   │                     │        Badge + Panel
     │                   │                     │        update reactively
```

### FCM Token Lifecycle

```
┌────────────────────────────────────────────────────────┐
│                   Token Lifecycle                       │
│                                                        │
│  App Install / Token Rotation                          │
│      │                                                 │
│      ▼                                                 │
│  onNewToken(token)                                     │
│      │                                                 │
│      ├──▶ SharedPreferencesManager.saveFcmToken(token) │
│      ├──▶ SharedPreferencesManager.setFcmTokenSynced(false)
│      │                                                 │
│      ├──▶ if (isLoggedIn)                              │
│      │       syncTokenWithBackend()                    │
│      │       PUT /api/users/{id}/fcm-token             │
│      │                                                 │
│  Login (LoginFragment)                                 │
│      │                                                 │
│      ├──▶ FirebaseMessaging.getInstance().getToken()    │
│      ├──▶ if (!isFcmTokenSynced)                       │
│      │       syncTokenWithBackend()                    │
│      │       PUT /api/users/{id}/fcm-token             │
│      │                                                 │
│  Logout                                                │
│      │                                                 │
│      └──▶ Token stays on backend (not unregistered)    │
│           Next login overwrites with new user's token  │
└────────────────────────────────────────────────────────┘
```

---

## 5. HTTP Polling Data Flow

Periodic REST calls for state discovery.

### Active Ride Polling (Driver/Passenger, 15s)

```
┌───────────────────────────────────────────────────────────────────┐
│                 Active Ride Polling (15s interval)                 │
│                                                                   │
│  Timer fires ────▶ pollForActiveRide()                            │
│                         │                                         │
│    ┌────────────────────┼────────────────────┐                    │
│    │                    ▼                    │                    │
│    │  GET /api/rides?status=IN_PROGRESS      │                    │
│    │    └── Found? ──▶ YES ──▶ Set activeRideId                  │
│    │                           Style menu item (yellow)           │
│    │                           subscribeToRideUpdates(id) ◀── NEW │
│    │                   NO                                         │
│    │                    ▼                                         │
│    │  GET /api/rides?status=ACCEPTED                              │
│    │    └── Found? ──▶ YES ──▶ Set activeRideId                  │
│    │                           Style menu item (yellow)           │
│    │                           subscribeToRideUpdates(id) ◀── NEW │
│    │                   NO                                         │
│    │                    ▼                                         │
│    │  GET /api/rides?status=PENDING                               │
│    │    └── Found? ──▶ YES ──▶ (same)                            │
│    │                   NO                                         │
│    │                    ▼                                         │
│    │  GET /api/rides?status=SCHEDULED                             │
│    │    └── Found? ──▶ YES ──▶ (same)                            │
│    │                   NO ──▶ Clear activeRideId                  │
│    │                           Style menu item (gray)             │
│    └─────────────────────────────────────────┘                    │
│                                                                   │
│  Lifecycle:                                                       │
│    Started: setupNavigationForRole() for DRIVER/PASSENGER         │
│    Stopped: logout, onDestroy()                                   │
└───────────────────────────────────────────────────────────────────┘
```

### Active Ride Screen Polling (ActiveRideFragment)

```
┌───────────────────────────────────────────────────────────────────┐
│           ActiveRideFragment — Hybrid WebSocket + Polling         │
│                                                                   │
│  Primary: WebSocket subscription to /topic/ride/{id}              │
│    └── Receives real-time status changes                          │
│    └── Fallback: if WS disconnected, polling takes over           │
│                                                                   │
│  Polling (runs regardless):                                       │
│    Every 5s:  GET /api/rides/{id}          → ride status/cost     │
│    Every 10s: GET /api/vehicles/location   → vehicle position     │
│                                                                   │
│  Map Updates:                                                     │
│    Vehicle marker position updated on each poll/WS message        │
│    Route drawn via OSRM: yellow dashed (approach) + green (ride)  │
│    Cost badge updated with live cost from RideCostTrackingService │
└───────────────────────────────────────────────────────────────────┘
```

---

## 6. Notification Delivery Matrix

Which channel delivers what, and where the data ends up:

```
┌──────────────────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│      Event           │ WebSocket│   FCM    │ Polling  │ In-App   │ System   │
│                      │          │          │          │ Panel    │ Notif    │
├──────────────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Ride status change   │    ✅    │    ✅    │    —     │    ✅    │    ✅    │
│ Panic alert (admin)  │    ✅    │    ✅    │    —     │    ✅    │    ✅    │
│ Support message      │    ✅    │    ✅*   │    —     │    ✅    │    ✅    │
│ Active ride exists   │    —     │    —     │    ✅    │    —     │    —     │
│ Vehicle location     │    —     │    —     │    ✅    │    —     │    —     │
├──────────────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ App foreground       │    ✅    │    ✅    │    ✅    │    ✅    │    ✅    │
│ App background       │    ❌    │    ✅    │    ❌    │    ✅**  │    ✅    │
│ App killed           │    ❌    │    ✅    │    ❌    │    ✅**  │    ✅    │
└──────────────────────┴──────────┴──────────┴──────────┴──────────┴──────────┘

*  If backend sends FCM for support messages
** FCM feeds NotificationStore which persists in-memory until process death
```

---

## 7. State Management Flow

### NotificationStore — Central Notification State

```
                    ┌───────────────────────────────────────┐
                    │         NotificationStore             │
                    │         (Singleton)                    │
                    │                                       │
  WebSocket ─────── │  ┌─────────────────────────────────┐ │
  (AppNotifMgr)     │  │  List<AppNotification>          │ │
        │           │  │  (max 100, newest first, FIFO)  │ │──── LiveData
        ├──────────▶│  └─────────────────────────────────┘ │     observe()
        │           │                                       │        │
  FCM ──┤           │  ┌─────────────────────────────────┐ │        ▼
  (MyFirebase       │  │  Integer unreadCount            │ │  ┌──────────┐
   MsgService)      │  │  (auto-computed on mutation)    │ │  │ Badge    │
        │           │  └─────────────────────────────────┘ │  │ Panel    │
        ├──────────▶│                                       │  │ Fragment │
        │           │  Methods:                             │  └──────────┘
        │           │    addNotification(AppNotification)   │
        │           │    markAsRead(long id)                │
        │           │    markAllAsRead()                    │
        │           │    removeNotification(long id)        │
        │           │    clearAll()                         │
        │           │                                       │
        │           │  Thread safety: postValue() (any      │
        │           │  thread → main thread delivery)       │
                    └───────────────────────────────────────┘
```

### SharedPreferencesManager — Persistent User State

```
┌──────────────────────────────────────────────────┐
│         SharedPreferencesManager                  │
│         ("RideSharePrefs")                        │
│                                                  │
│  Auth:                                           │
│    jwt_token ──────────── JWT access token        │
│    refresh_token ──────── Refresh token           │
│    is_logged_in ───────── Boolean                 │
│                                                  │
│  User Identity:                                  │
│    user_id ────────────── Long                    │
│    user_email ─────────── String                  │
│    user_name ──────────── String                  │
│    user_surname ───────── String                  │
│    user_role ──────────── PASSENGER|DRIVER|ADMIN  │
│    user_phone ─────────── String                  │
│    user_address ───────── String                  │
│    user_profile_image ─── URL String              │
│                                                  │
│  Driver State:                                   │
│    driver_status ──────── Boolean (online/offline)│
│                                                  │
│  FCM:                                            │
│    fcm_token ──────────── Device push token       │
│    fcm_token_synced ───── Boolean                 │
│                                                  │
│  Written at: Login, Profile update, FCM refresh   │
│  Cleared at: Logout (clearAll)                    │
└──────────────────────────────────────────────────┘
```

---

## 8. Ride Lifecycle Data Flow

Complete ride state machine from request to completion:

```
                    ┌────────────┐
         create ───▶│  PENDING   │
                    └─────┬──────┘
                          │
               ┌──────────┼──────────┐
               │          │          │
               ▼          ▼          ▼
          ┌─────────┐ ┌────────┐ ┌──────────┐
          │SCHEDULED│ │ACCEPTED│ │CANCELLED │
          │(future) │ │        │ │BY_PASSENGER
          └────┬────┘ └───┬────┘ └──────────┘
               │          │
               │          ▼
               │    ┌───────────┐
               └───▶│IN_PROGRESS│◀─── Vehicle tracking starts
                    └─────┬─────┘     Cost tracking (5s)
                          │
           ┌──────────────┼──────────────┐
           │              │              │
           ▼              ▼              ▼
     ┌──────────┐   ┌──────────┐   ┌──────────┐
     │ FINISHED │   │ PANIC    │   │CANCELLED │
     │          │   │          │   │BY_DRIVER │
     └──────────┘   └──────────┘   └──────────┘

WebSocket subscription (/topic/ride/{id}) fires on EVERY state change.
AppNotificationManager creates in-app + system notification for each.
Auto-unsubscribes on terminal states (FINISHED, CANCELLED*, REJECTED).
```

---

## 9. Map Data Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    Map Rendering Pipeline                     │
│                                                              │
│  1. Init: OSMDroid MapView                                   │
│     └── Tile source: CartoDB Dark Matter (HTTPS)             │
│     └── Center: Novi Sad (45.2517, 19.8369), zoom 14         │
│                                                              │
│  2. Vehicle Markers:                                         │
│     └── Source: GET /api/vehicles/active (10s polling)        │
│     └── OR: /topic/vehicles (WebSocket, admin ride detail)   │
│     └── Icons: ic_vehicle_available / ic_vehicle_occupied    │
│     └── Tint: Green (available), Red (panic), Gray (occupied)│
│                                                              │
│  3. Route Drawing:                                           │
│     └── OSRM API: http://router.project-osrm.org            │
│     └── Approach route: Yellow dashed line (driver → pickup) │
│     └── Ride route: Green solid line (pickup → destination)  │
│     └── Via OSMBonusPack RoadManager overlay                 │
│                                                              │
│  4. Custom Markers:                                          │
│     └── Pickup: ic_map_pickup (green)                        │
│     └── Destination: ic_map_pin (red)                        │
│     └── Stops: numbered markers                              │
│     └── Vehicle: ic_navigation_icon (real-time position)     │
└──────────────────────────────────────────────────────────────┘
```
