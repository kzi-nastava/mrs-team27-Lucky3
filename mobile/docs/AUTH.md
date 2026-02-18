# Authentication

> JWT-based authentication flow covering login, registration, token management, session persistence, FCM token synchronization, and logout cleanup.

---

## 1. Auth Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        AUTH SYSTEM OVERVIEW                          │
│                                                                      │
│  ┌───────────┐     ┌──────────┐     ┌──────────┐     ┌───────────┐  │
│  │  Login /   │────▶│ Backend  │────▶│ JWT      │────▶│ SharedPref│  │
│  │  Register  │     │ /api/auth│     │ Token    │     │ Manager   │  │
│  └───────────┘     └──────────┘     └──────────┘     └───────────┘  │
│                                                            │         │
│                                                            ▼         │
│  ┌───────────┐     ┌──────────────────────────────────────────────┐  │
│  │  Retrofit  │◀───│  "Bearer " + SharedPreferencesManager       │  │
│  │  @Header   │     │  .getToken()                                │  │
│  └───────────┘     └──────────────────────────────────────────────┘  │
│                                                                      │
│  Token Format: HS512 JWT                                             │
│  Subject: user email                                                 │
│  Claims: { role: "DRIVER", userId: 42 }                              │
│  Expiration: configurable via JWT_EXPIRATION env var                 │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. Login Flow

```
┌──────────────────────────────────────────────────────────────┐
│                         LOGIN FLOW                            │
│                                                              │
│  LoginFragment                                               │
│    │                                                         │
│    ├── 1. User enters email + password                       │
│    │                                                         │
│    ├── 2. POST /api/auth/login                               │
│    │      Body: { "email": "...", "password": "..." }        │
│    │      Response: { "token": "<JWT>", "refreshToken":...}  │
│    │                                                         │
│    ├── 3. Decode JWT claims (without verification):           │
│    │      ├── role = claims.get("role")                      │
│    │      ├── userId = claims.get("userId")                  │
│    │      └── email = JWT subject                            │
│    │                                                         │
│    ├── 4. Save to SharedPreferencesManager:                  │
│    │      ├── saveToken(jwt)                                 │
│    │      ├── saveUserId(userId)                             │
│    │      ├── saveUserEmail(email)                           │
│    │      ├── saveUserRole(role)                             │
│    │      └── setLoggedIn(true)                              │
│    │                                                         │
│    ├── 5. FCM token sync:                                    │
│    │      ├── FirebaseMessaging.getInstance().getToken()      │
│    │      ├── saveFcmToken(deviceToken)                      │
│    │      └── PUT /api/users/{id}/fcm-token                  │
│    │                                                         │
│    ├── 6. Fetch full user profile:                           │
│    │      ├── GET /api/users/{id}                            │
│    │      └── Save name, surname, phone, address, image      │
│    │                                                         │
│    ├── 7. For DRIVER: fetch driver status:                   │
│    │      ├── GET /api/drivers/{id}/status                   │
│    │      └── saveDriverStatus(active)                       │
│    │                                                         │
│    ├── 8. Setup navigation for role:                         │
│    │      └── MainActivity.setupNavigationForRole(role)      │
│    │                                                         │
│    └── 9. Navigate to role landing page:                     │
│           ├── DRIVER    → nav_driver_dashboard               │
│           ├── ADMIN     → nav_admin_dashboard                │
│           └── PASSENGER → nav_passenger_home                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Registration Flow

```
┌──────────────────────────────────────────────────────────────┐
│                      REGISTRATION FLOW                        │
│                                                              │
│  RegisterFragment                                            │
│    │                                                         │
│    ├── 1. User fills form:                                   │
│    │      name, surname, email, phone, address, password     │
│    │      + optional profile image (camera/gallery)          │
│    │                                                         │
│    ├── 2. Client-side validation:                            │
│    │      ├── All fields required                            │
│    │      ├── Email format                                   │
│    │      ├── Password ≥ 8 chars                             │
│    │      └── Passwords match                                │
│    │                                                         │
│    ├── 3. POST /api/auth/register (Multipart):               │
│    │      ├── Part "data": JSON blob (RegisterRequest)       │
│    │      └── Part "profileImage": image file (optional)     │
│    │                                                         │
│    ├── 4. On success (201):                                  │
│    │      └── Navigate to RegisterVerificationFragment       │
│    │           "Check your email to verify your account"     │
│    │                                                         │
│    └── 5. On error (409):                                    │
│           └── "Email already in use"                         │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. Password Reset Flow

```
ForgotPasswordFragment
    │
    ├── POST /api/auth/forgot-password
    │     Body: { "email": "user@example.com" }
    │
    ▼
ForgotPasswordSentFragment
    │   "Reset link sent to your email"
    │
    ├── User clicks link in email
    │     URL: https://<FRONTEND_URL>/reset-password?token=<UUID>
    │
    ▼
ResetPasswordFragment
    │
    ├── GET /api/auth/reset-password/validate?token=<UUID>
    │     (validates token is still valid)
    │
    ├── POST /api/auth/reset-password
    │     Body: { "token": "<UUID>", "newPassword": "..." }
    │
    ▼
ResetPasswordSuccessFragment
      "Password reset successfully. You can now login."
```

---

## 5. Token Attachment Pattern

Android mobile uses **explicit header injection** — no OkHttp interceptor.

```java
// Every authenticated API call follows this pattern:
String token = "Bearer " + SharedPreferencesManager.getToken();

ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
    @Override
    public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
        if (response.code() == 401) {
            // Token expired → force logout
            handleSessionExpired();
        }
        // ... handle response
    }
    // ...
});
```

### Retrofit Interface Pattern

```java
public interface RideService {
    // Authenticated — includes @Header("Authorization")
    @GET("api/rides/{id}")
    Call<RideResponse> getRide(
        @Path("id") long id,
        @Header("Authorization") String token  // "Bearer <JWT>"
    );

    // Public — no @Header
    @POST("api/rides/estimate")
    Call<RideEstimationResponse> estimateRide(
        @Body CreateRideRequest request
    );
}
```

### Why No OkHttp Interceptor?

Per project convention (documented in `copilot-instructions.md`), auth tokens are **not** attached via OkHttp interceptors. This provides:
- Explicit control over which endpoints receive auth headers
- Clear distinction between public and authenticated endpoints
- Simpler debugging (token visible at call site)

---

## 6. SharedPreferencesManager — Auth Storage

**Preferences file**: `"RideSharePrefs"` (Context.MODE_PRIVATE)

### Stored Keys

| Key | Type | Written At | Description |
|-----|------|-----------|-------------|
| `jwt_token` | String | Login | JWT access token |
| `refresh_token` | String | Login | Refresh token |
| `is_logged_in` | Boolean | Login/Logout | Auth state flag |
| `user_id` | Long | Login | User database ID |
| `user_email` | String | Login | Email address |
| `user_name` | String | Login, Profile update | First name |
| `user_surname` | String | Login, Profile update | Last name |
| `user_role` | String | Login | `PASSENGER` / `DRIVER` / `ADMIN` |
| `user_phone` | String | Login, Profile update | Phone number |
| `user_address` | String | Login, Profile update | Street address |
| `user_profile_image` | String | Login, Profile update | Profile image URL |
| `driver_status` | Boolean | Login (driver), Toggle | Online/offline state |
| `fcm_token` | String | FCM onNewToken, Login | Firebase device token |
| `fcm_token_synced` | Boolean | FCM sync success/failure | Whether FCM token was sent to backend |

### API Methods

```java
public class SharedPreferencesManager {
    static void init(Context context);          // Call once in Application/Activity

    // Auth
    static void saveToken(String token);
    static String getToken();
    static void saveRefreshToken(String token);
    static String getRefreshToken();
    static void setLoggedIn(boolean loggedIn);
    static boolean isLoggedIn();

    // User identity
    static void saveUserId(long id);
    static long getUserId();
    static void saveUserEmail(String email);
    static String getUserEmail();
    static void saveUserName(String name);
    static String getUserName();
    static void saveUserSurname(String surname);
    static String getUserSurname();
    static void saveUserRole(String role);
    static String getUserRole();
    static void saveUserPhone(String phone);
    static String getUserPhone();
    static void saveUserAddress(String address);
    static String getUserAddress();
    static void saveUserProfileImage(String url);
    static String getUserProfileImage();

    // Driver
    static void saveDriverStatus(boolean active);
    static boolean getDriverStatus();

    // FCM
    static void saveFcmToken(String token);
    static String getFcmToken();
    static void setFcmTokenSynced(boolean synced);
    static boolean isFcmTokenSynced();

    // Cleanup
    static void clearAll();                     // Removes all keys
}
```

---

## 7. Session Restore

On app cold start, `MainActivity.checkSession()` restores the session:

```
App Launch → MainActivity.onCreate()
    │
    ▼
SharedPreferencesManager.init(this)
    │
    ▼
checkSession(navController)
    │
    ├── isLoggedIn() == true
    │     │
    │     ├── token = getToken()         // may be expired
    │     ├── role = getUserRole()
    │     │
    │     ├── setupNavigationForRole(role)
    │     │     ├── Inflate role-specific drawer menu
    │     │     ├── Start active ride polling (DRIVER/PASSENGER)
    │     │     ├── Start AppNotificationManager
    │     │     └── Setup notification bell
    │     │
    │     └── Navigate to landing page
    │
    └── isLoggedIn() == false
          └── Stay on GuestHomeFragment
```

### Token Expiry Handling

- No automatic token refresh mechanism in the mobile app
- Expired JWT → backend returns `401 Unauthorized`
- App shows error message; user must re-login
- Future improvement: implement refresh token rotation via `refresh_token`

---

## 8. FCM Token Synchronization

```
┌──────────────────────────────────────────────────────────────┐
│                   FCM TOKEN SYNC FLOW                        │
│                                                              │
│  Scenario 1: Token rotation (app running)                    │
│    │                                                         │
│    ├── MyFirebaseMessagingService.onNewToken(newToken)        │
│    ├── SharedPreferencesManager.saveFcmToken(newToken)        │
│    ├── SharedPreferencesManager.setFcmTokenSynced(false)      │
│    └── if (isLoggedIn())                                     │
│          PUT /api/users/{userId}/fcm-token                   │
│          Body: { "fcmToken": "<newToken>" }                  │
│          → setFcmTokenSynced(true)                           │
│                                                              │
│  Scenario 2: Login                                           │
│    │                                                         │
│    ├── FirebaseMessaging.getInstance().getToken()              │
│    ├── saveFcmToken(token)                                   │
│    └── if (!isFcmTokenSynced())                              │
│          PUT /api/users/{userId}/fcm-token                   │
│          → setFcmTokenSynced(true)                           │
│                                                              │
│  Scenario 3: Logout                                          │
│    │                                                         │
│    └── Token NOT unregistered from backend                   │
│        (overwritten on next user's login)                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 9. Logout Flow

```
User taps "Logout" in drawer menu
    │
    ├── 1. POST /api/auth/logout (with token)
    │
    ├── 2. Stop active ride polling
    │      └── ridePollingHandler.removeCallbacks(pollingRunnable)
    │
    ├── 3. AppNotificationManager.stop()
    │      ├── Unsubscribe all WebSocket topics
    │      └── WebSocketManager.disconnect()
    │
    ├── 4. SharedPreferencesManager.clearAll()
    │      └── Removes all 14 keys from SharedPreferences
    │
    ├── 5. NotificationStore.getInstance().clearAll()
    │      └── Clears in-memory notification list
    │
    ├── 6. Reset navigation:
    │      ├── NavigationView.getMenu().clear()
    │      ├── Inflate default menu (navigation_drawer.xml)
    │      └── NavController.navigate(nav_guest_home)
    │           with options: popUpTo(nav_graph, inclusive=true)
    │
    └── 7. User sees GuestHomeFragment
```

---

## 10. Security Considerations

| Concern | Implementation | Notes |
|---------|----------------|-------|
| Token storage | `SharedPreferences` (MODE_PRIVATE) | Not encrypted; consider `EncryptedSharedPreferences` for production |
| Token in transit | HTTPS recommended | Development uses HTTP (`http://<IP>:8081`) |
| JWT validation | Server-side only | Client decodes claims without signature verification (for role extraction) |
| Password | Not stored locally | Only JWT persisted after login |
| Logout cleanup | `clearAll()` removes all data | Covers token, user info, FCM status |
| Session fixation | JWT is stateless | New JWT issued per login; old tokens expire naturally |
