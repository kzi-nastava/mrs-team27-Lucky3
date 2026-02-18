# Architecture

> System architecture, module structure, and design patterns used in the Lucky3 Android client.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID APPLICATION                      │
│                                                             │
│  ┌─────────┐    ┌────────────┐     ┌─────────────────────┐  │
│  │   UI    │───▶│  ViewModel │───▶│  Service / Utils    │  │
│  │(Fragment)│◀──│  (LiveData)│◀── │ (Retrofit, WS, FCM) │  │
│  └─────────┘    └────────────┘     └──────────┬──────────┘  │
│                                               │             │
└───────────────────────────────────────────────┼─────────────┘
                                                │
                    ┌───────────────────────────┼──────┐
                    │           NETWORK                │
                    │                                  │
                    │  ┌──────┐  ┌──────┐  ┌───────┐  │
                    │  │ REST │  │ WS   │  │  FCM  │  │
                    │  │(HTTP)│  │(STOMP)│  │(Push) │  │
                    │  └──┬───┘  └──┬───┘  └──┬────┘  │
                    └─────┼────────┼────────┼─────────┘
                          │        │        │
                    ┌─────▼────────▼────────▼─────────┐
                    │       SPRING BOOT BACKEND        │
                    │   http://<IP>:8081/api/*          │
                    │   ws://<IP>:8081/ws               │
                    │   FCM via Google servers          │
                    └──────────────────────────────────┘
```

## Design Patterns

### MVVM (Model-View-ViewModel)

The app follows Android's recommended MVVM pattern:

```
Fragment (View)
    │ observes LiveData
    ▼
ViewModel
    │ calls service methods
    ▼
Retrofit Service / Repository
    │ HTTP request
    ▼
Backend API
```

- **View** — Fragments observe `LiveData` and update UI reactively
- **ViewModel** — Extends `AndroidViewModel`, holds UI state, survives configuration changes
- **Model** — DTOs in `models/` package, mapped from JSON via Gson

**16 ViewModels total:**

| ViewModel | Purpose |
|-----------|---------|
| `LoginViewModel` | Login form state, JWT extraction, role routing |
| `RegisterViewModel` | Registration with multipart image upload |
| `ForgotPasswordViewModel` | Password reset email request |
| `ResetPasswordViewModel` | Token validation + password reset |
| `UserProfileViewModel` | Passenger/Admin profile loading |
| `DriverProfileViewModel` | Driver profile + vehicle info loading |
| `PassengerHomeViewModel` | Vehicle map, active ride check, ride creation |
| `PassengerHistoryViewModel` | Passenger ride history (stub) |
| `PassengerFavoritesViewModel` | Favorite route CRUD |
| `AdminDriversViewModel` | Driver list, search, filter, create |
| `AdminRequestsViewModel` | Change request review (approve/reject) |
| `AdminRideHistoryViewModel` | Admin ride history search, filter, pagination |
| `ReflowViewModel` | Template placeholder |
| `SettingsViewModel` | Template placeholder |
| `SlideshowViewModel` | Template placeholder |
| `TransformViewModel` | Template placeholder |

### Singleton Pattern

Critical infrastructure uses thread-safe singletons (double-checked locking):

| Singleton | Purpose |
|-----------|---------|
| `ClientUtils` | Retrofit instance + 7 service stubs (static) |
| `WebSocketManager` | Shared STOMP WebSocket connection |
| `AppNotificationManager` | Real-time notification subscriptions |
| `NotificationStore` | In-memory notification state with LiveData |

### Observer Pattern (LiveData)

All reactive state flows through `LiveData`:

```
NotificationStore.unreadCount (LiveData<Integer>)
    └──▶ MainActivity.setupNotificationBell() — badge visibility
    └──▶ NotificationPanelFragment — unread counter text

NotificationStore.notifications (LiveData<List<AppNotification>>)
    └──▶ NotificationPanelFragment.NotificationAdapter — list updates
```

### Adapter Pattern (ListView + BaseAdapter)

All lists use `ListView` with `BaseAdapter` and the ViewHolder pattern (project convention — no RecyclerView):

```java
class NotificationAdapter extends BaseAdapter {
    static class ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        ImageView ivIcon;
        View unreadDot;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_notification, parent, false);
            vh = new ViewHolder();
            vh.tvTitle = convertView.findViewById(R.id.tv_notification_title);
            // ... bind views
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }
        // populate from data
        return convertView;
    }
}
```

## Module Structure

### `models/` — 46 DTOs

Data Transfer Objects that mirror backend request/response shapes. All use Gson `@SerializedName` where field names differ from JSON keys.

**Categories:**
- **Auth**: `LoginRequest`, `TokenResponse`, `RegistrationRequest`, `EmailRequest`, `PasswordResetRequest`, `FcmTokenRequest`
- **User**: `UserResponse`, `ProfileUserResponse`, `User`, `UserRole`
- **Driver**: `DriverResponse`, `DriverProfileResponse`, `DriverStatsResponse`, `DriverStatusResponse`, `DriverChangeRequest`, `DriverChangeRequestCreated`, `DriverChangeStatus`, `CreateDriverRequest`, `ReviewDriverChangeRequest`
- **Ride**: `CreateRideRequest`, `RideResponse` (385 lines, largest model), `RideEstimationResponse`, `RideCancellationRequest`, `RideStopRequest`, `RidePanicRequest`, `EndRideRequest`, `RideRequirements`, `RoutePointResponse`, `InconsistencyRequest`
- **Vehicle**: `VehicleInformation`, `VehicleLocationResponse`, `VehiclePriceResponse`, `VehicleType`, `UpdateVehiclePriceRequest`
- **Support**: `SupportMessageResponse`, `SupportChatListItemResponse`
- **Notification**: `AppNotification` (with `Type` enum)
- **Other**: `PageResponse<T>`, `PanicResponse` (with nested types), `LocationDto`, `FavoriteRouteRequest/Response`, `ReviewRequest`, `ReviewTokenValidationResponse`, `ErrorResponse`, `AdminStatsResponse`

### `services/` — 7 Retrofit Interfaces + 1 FCM Service

| Interface | Endpoints | Purpose |
|-----------|-----------|---------|
| `UserService` | 12 | Auth, profile CRUD, FCM token |
| `RideService` | 15 | Ride lifecycle, history, favorites, admin stats |
| `DriverService` | 10 | Driver CRUD, stats, status toggle |
| `VehicleService` | 1 | Active vehicle locations (public) |
| `PanicService` | 1 | Admin panic alerts |
| `ReviewService` | 3 | Ride reviews (auth + token-based) |
| `AdminService` | 2 | Vehicle pricing CRUD |
| `MyFirebaseMessagingService` | — | FCM push handler (not Retrofit) |

**Total: 44 API endpoints**

### `utils/` — 7 Utility Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `ClientUtils` | ~120 | Retrofit singleton, Gson config, 7 service stubs |
| `SharedPreferencesManager` | ~200 | 14 stored keys, 23 accessor methods |
| `StompClient` | 361 | STOMP 1.1 protocol over OkHttp WebSocket |
| `WebSocketManager` | 163 | Singleton wrapper for StompClient |
| `AppNotificationManager` | 444 | WebSocket subscription hub + system notifications |
| `NotificationStore` | 151 | In-memory notification state (LiveData, max 100) |
| `NotificationHelper` | ~60 | Android notification channel definitions (3 channels) |
| `ValidationUtils` | ~80 | Email, password, phone, required field validation |
| `ListViewHelper` | ~30 | ListView height calculation for ScrollView embedding |

### `ui/` — 30+ Fragments by Feature

| Package | Fragments | Dialogs | Purpose |
|---------|-----------|---------|---------|
| `admin/` | 9 | 1 (AddDriverDialog) | Admin screens: dashboard, rides, drivers, pricing, panic, change requests |
| `auth/` | 5 | 0 | Forgot/reset password flow, registration verification |
| `driver/` | 4 | 0 | Driver dashboard, overview, ride history, ride details |
| `guest/` | 1 | 0 | Landing page with map and estimation |
| `login/` | 1 | 0 | Login screen |
| `maps/` | 1 (utility) | 0 | Map rendering helper (not a Fragment) |
| `notifications/` | 1 | 0 | Notification center panel |
| `passenger/` | 5 | 4 (OrderRide, RequestRideForm, LinkPassengers, RideCreated) | Ride request, history, details, review, favorites |
| `profile/` | 2 | 2 (ChangePersonalInfo, ChangeDriverInfo) | User and driver profile editing |
| `register/` | 1 | 0 | Registration form |
| `ride/` | 1 | 6 (Cancel, Panic, Stop, Finish, ReportInconsistency) | Active ride screen with live tracking |
| `support/` | 1 | 0 | Support page (placeholder) |

## Single Activity Architecture

The app uses a **single `MainActivity`** with Navigation Component:

```
MainActivity
├── DrawerLayout
│   ├── NavHostFragment (content) ← all 39 Fragment destinations live here
│   └── NavigationView (drawer) ← role-specific menu:
│       ├── menu_drawer_admin.xml (10 items)
│       ├── menu_drawer_driver.xml (6 items)
│       └── menu_drawer_passenger.xml (7 items)
└── Custom Navbar (view_custom_navbar.xml)
    ├── Hamburger menu button
    ├── Title text
    ├── Notification bell + dynamic badge
    └── Settings gear
```

`MainActivity` responsibilities:
- Session restoration on cold start
- Role-based drawer menu inflation
- Navigation item click handling
- Active ride polling (15s, Driver/Passenger)
- Notification manager lifecycle (start on login, stop on logout)
- Badge count observation via LiveData
- FCM deep-link routing
- Notification permission management (Android 13+)

## Threading Model

| Task | Mechanism |
|------|-----------|
| Network calls | Retrofit `Call<T>.enqueue()` — background thread, callback on main |
| WebSocket messages | OkHttp WebSocket listener (background) → `Handler(mainLooper).post()` |
| FCM messages | `onMessageReceived()` on FCM thread → `NotificationStore.postValue()` |
| Image decoding | `Thread` + `Runnable` → `runOnUiThread()` for `setImageBitmap()` |
| Polling | `Handler.postDelayed()` on main looper |
| UI updates | Always on main thread via `LiveData.observe()` or `runOnUiThread()` |

**Prohibited** (per project conventions): Kotlin Coroutines, RxJava, ExecutorService, Glide/Picasso/Coil, RecyclerView.
