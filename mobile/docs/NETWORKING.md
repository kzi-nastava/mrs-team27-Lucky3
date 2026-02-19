# Networking

> Complete API reference for all REST endpoints, WebSocket/STOMP real-time communication, and network infrastructure.

---

## 1. Network Infrastructure

### ClientUtils — Retrofit Singleton

| Property | Value |
|----------|-------|
| **Base URL** | `http://<BuildConfig.IP_ADDR>:8081/` |
| **Connect timeout** | 30 seconds |
| **Read timeout** | 30 seconds |
| **Write timeout** | 30 seconds |
| **Logging** | `HttpLoggingInterceptor` at `Level.BODY` |
| **Gson date format** | `yyyy-MM-dd'T'HH:mm:ss` |
| **Gson extras** | Custom `LocalDateTime` serializer/deserializer via `DateTimeFormatter.ISO_LOCAL_DATE_TIME`, `setLenient()` |
| **IP Source** | `BuildConfig.IP_ADDR` from `local.properties` (emulator default: `10.0.2.2`) |

### Service Registry

```java
public class ClientUtils {
    public static UserService    userService;     // 12 endpoints
    public static DriverService  driverService;   //  9 endpoints
    public static VehicleService vehicleService;  //  1 endpoint
    public static RideService    rideService;     // 16 endpoints
    public static PanicService   panicService;    //  1 endpoint
    public static ReviewService  reviewService;   //  3 endpoints
    public static AdminService   adminService;    //  2 endpoints
}                                                 // ── 44 total
```

---

## 2. REST API Reference

### 2.1 UserService — Authentication & Profile

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `POST` | `api/auth/login` | `login` | `@Body LoginRequest` | `Call<TokenResponse>` | — |
| 2 | `POST` | `api/auth/register` | `register` | `@Part("data") RequestBody`, `@Part MultipartBody.Part profileImage` | `Call<UserResponse>` | — |
| 3 | `POST` | `api/auth/register` | `registerWithoutImage` | `@Part("data") RequestBody` | `Call<UserResponse>` | — |
| 4 | `POST` | `api/auth/logout` | `logout` | `@Header token` | `Call<Void>` | ✅ |
| 5 | `POST` | `api/auth/forgot-password` | `forgotPassword` | `@Body EmailRequest` | `Call<Void>` | — |
| 6 | `POST` | `api/auth/reset-password` | `resetPassword` | `@Body PasswordResetRequest` | `Call<Void>` | — |
| 7 | `GET` | `api/auth/reset-password/validate` | `validateResetToken` | `@Query("token")` | `Call<Void>` | — |
| 8 | `GET` | `api/auth/activate` | `activateAccount` | `@Query("token")` | `Call<Void>` | — |
| 9 | `POST` | `api/auth/resend-activation` | `resendActivation` | `@Body EmailRequest` | `Call<Void>` | — |
| 10 | `GET` | `api/users/{id}` | `getUserById` | `@Path("id") Long` | `Call<ProfileUserResponse>` | ✅ |
| 11 | `PUT` | `api/users/{id}` | `updatePersonalInfo` | `@Path("id") Long`, `@Part("user") RequestBody`, `@Part image` | `Call<ProfileUserResponse>` | ✅ |
| 12 | `PUT` | `api/users/{id}/fcm-token` | `updateFcmToken` | `@Path("id") Long`, `@Body FcmTokenRequest` | `Call<Void>` | ✅ |

### 2.2 DriverService — Driver Management

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `GET` | `api/drivers/{driverId}/stats` | `getStats` | `@Path driverId` | `Call<DriverStatsResponse>` | ✅ |
| 2 | `GET` | `api/drivers` | `getAllDrivers` | — | `Call<List<DriverResponse>>` | ✅ |
| 3 | `GET` | `api/drivers/{id}` | `getDriverById` | `@Path("id") Long` | `Call<DriverProfileResponse>` | ✅ |
| 4 | `PUT` | `api/drivers/{id}` | `updateDriverInfo` | `@Path("id") Long`, `@Part("request") RequestBody`, `@Part image` | `Call<DriverChangeRequestCreated>` | ✅ |
| 5 | `POST` | `api/drivers` | `createDriver` | `@Part("request") RequestBody`, `@Part image` | `Call<DriverResponse>` | ✅ |
| 6 | `GET` | `/api/driver-change-requests` | `getDriverChangeRequests` | `@Query("status")` | `Call<List<DriverChangeRequest>>` | ✅ |
| 7 | `PUT` | `/api/driver-change-requests/{requestId}/review` | `reviewDriverChangeRequest` | `@Path("requestId") Long`, `@Body ReviewDriverChangeRequest` | `Call<Void>` | ✅ |
| 8 | `GET` | `api/drivers/{driverId}/status` | `getDriverStatus` | `@Path driverId` | `Call<DriverStatusResponse>` | ✅ |
| 9 | `PUT` | `api/drivers/{driverId}/status` | `toggleDriverStatus` | `@Path driverId`, `@Query("active") boolean` | `Call<DriverStatusResponse>` | ✅ |

### 2.3 VehicleService — Vehicle Tracking

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `GET` | `api/vehicles/active` | `getActiveVehicles` | — | `Call<List<VehicleLocationResponse>>` | — |

### 2.4 RideService — Ride Operations

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `POST` | `api/rides/estimate` | `estimateRide` | `@Body CreateRideRequest` | `Call<RideEstimationResponse>` | — |
| 2 | `POST` | `api/rides` | `createRide` | `@Body CreateRideRequest` | `Call<RideResponse>` | ✅ |
| 3 | `GET` | `api/rides/{id}` | `getRide` | `@Path("id") long` | `Call<RideResponse>` | ✅ |
| 4 | `GET` | `api/rides` | `getRidesHistory` | `@Query driverId, passengerId, status, fromDate, toDate, page, size, sort` | `Call<PageResponse<RideResponse>>` | ✅ |
| 5 | `PUT` | `api/rides/{id}/cancel` | `cancelRide` | `@Path("id") long`, `@Body RideCancellationRequest` | `Call<RideResponse>` | ✅ |
| 6 | `GET` | `api/rides` | `getActiveRides` | `@Query driverId, passengerId, status, page, size` | `Call<PageResponse<RideResponse>>` | ✅ |
| 7 | `PUT` | `api/rides/{id}/panic` | `panicRide` | `@Path("id") long`, `@Body RidePanicRequest` | `Call<RideResponse>` | ✅ |
| 8 | `PUT` | `api/rides/{id}/end` | `endRide` | `@Path("id") long`, `@Body EndRideRequest` | `Call<RideResponse>` | ✅ |
| 9 | `PUT` | `api/rides/{id}/stop` | `stopRide` | `@Path("id") long`, `@Body RideStopRequest` | `Call<RideResponse>` | ✅ |
| 10 | `PUT` | `api/rides/{id}/stop/{stopIndex}/complete` | `completeStop` | `@Path("id") long`, `@Path("stopIndex") int` | `Call<RideResponse>` | ✅ |
| 11 | `POST` | `api/rides/{id}/inconsistencies` | `reportInconsistency` | `@Path("id") long`, `@Body InconsistencyRequest` | `Call<Void>` | ✅ |
| 12 | `GET` | `api/rides/{id}/favourite-routes` | `getFavoriteRoutes` | `@Path("id") Long` | `Call<List<FavoriteRouteResponse>>` | ✅ |
| 13 | `POST` | `api/rides/{id}/favourite-route` | `addFavouriteRoute` | `@Path("id") Long`, `@Body FavoriteRouteRequest` | `Call<Void>` | ✅ |
| 14 | `DELETE` | `api/rides/{passengerId}/favourite-routes/{favouriteRouteId}` | `removeFavouriteRoute` | `@Path passengerId, favouriteRouteId` | `Call<Void>` | ✅ |
| 15 | `GET` | `api/rides/active/all` | `getAllActiveRides` | `@Query page, size, sort, search, status, vehicleType` | `Call<PageResponse<RideResponse>>` | ✅ |
| 16 | `GET` | `api/admin/stats` | `getAdminStats` | — | `Call<AdminStatsResponse>` | ✅ |

### 2.5 AdminService — Administration

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `GET` | `api/admin/vehicle-prices` | `getAllVehiclePrices` | — | `Call<List<VehiclePriceResponse>>` | ✅ |
| 2 | `PUT` | `api/admin/vehicle-prices` | `updateVehiclePrice` | `@Body UpdateVehiclePriceRequest` | `Call<VehiclePriceResponse>` | ✅ |

### 2.6 PanicService — Panic Alerts

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `GET` | `api/panic` | `getPanics` | `@Query("page") int`, `@Query("size") int` | `Call<PageResponse<PanicResponse>>` | ✅ |

### 2.7 ReviewService — Ride Reviews

| # | Method | Path | Java Method | Parameters | Returns | Auth |
|---|--------|------|-------------|------------|---------|------|
| 1 | `POST` | `api/reviews` | `createReview` | `@Body ReviewRequest` | `Call<RideResponse.ReviewInfo>` | ✅ |
| 2 | `GET` | `api/reviews/validate-token` | `validateReviewToken` | `@Query("token")` | `Call<ReviewTokenValidationResponse>` | — |
| 3 | `POST` | `api/reviews/with-token` | `createReviewWithToken` | `@Body ReviewRequest` | `Call<RideResponse.ReviewInfo>` | — |

---

## 3. Endpoint Statistics

| Category | Count |
|----------|-------|
| Total REST endpoints | **44** |
| Authenticated (`@Header`) | **31** |
| Public (no auth) | **13** |
| Multipart (`@Multipart`) | **5** |
| `GET` | 17 |
| `POST` | 13 |
| `PUT` | 11 |
| `DELETE` | 1 |

---

## 4. Request/Response Patterns

### 4.1 Standard JSON Request

```java
// In Fragment/ViewModel
String token = "Bearer " + SharedPreferencesManager.getToken();

ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
    @Override
    public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            // Update UI / LiveData
        } else {
            // Handle HTTP error (4xx/5xx)
        }
    }

    @Override
    public void onFailure(Call<RideResponse> call, Throwable t) {
        // Network failure
    }
});
```

### 4.2 Multipart Upload (Registration)

```java
// JSON part
Gson gson = ClientUtils.getGson();
String json = gson.toJson(registerRequest);
RequestBody data = RequestBody.create(json, MediaType.parse("application/json"));

// Image part (optional)
File imageFile = new File(imagePath);
RequestBody imageBody = RequestBody.create(imageFile, MediaType.parse("image/*"));
MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
    "profileImage", imageFile.getName(), imageBody
);

// Call
ClientUtils.userService.register(data, imagePart).enqueue(...);
```

### 4.3 Paginated Responses

```java
// PageResponse<T> structure
{
    "content": [...],       // List<T>
    "totalPages": 5,
    "totalElements": 47,
    "size": 10,
    "number": 0,           // current page (0-based)
    "numberOfElements": 10,
    "first": true,
    "last": false,
    "empty": false
}
```

---

## 5. WebSocket / STOMP Protocol

### 5.1 StompClient — Low-Level STOMP Implementation

Custom STOMP 1.1 client built on OkHttp `WebSocket`.

| Property | Value |
|----------|-------|
| **Transport** | OkHttp WebSocket (raw STOMP, not SockJS) |
| **URL suffix** | `/websocket` appended to bypass SockJS envelope |
| **STOMP version** | `1.1` |
| **Heartbeat** | `10,000 ms` send / `10,000 ms` receive |
| **Authentication** | JWT in STOMP `CONNECT` frame header |
| **Serialization** | Gson (shared instance from `ClientUtils`) |
| **Thread dispatch** | All callbacks via `Handler(Looper.getMainLooper())` |

#### Connection Handshake

```
Client                                    Server
  │                                         │
  │──── WebSocket CONNECT ─────────────────▶│
  │     ws://<IP>:8081/ws/websocket          │
  │                                         │
  │──── STOMP CONNECT ─────────────────────▶│
  │     accept-version:1.1                   │
  │     Authorization:Bearer <JWT>           │
  │     heart-beat:10000,10000               │
  │                                         │
  │◀──── STOMP CONNECTED ──────────────────│
  │      version:1.1                         │
  │      heart-beat:10000,10000              │
  │                                         │
  │──── STOMP SUBSCRIBE ───────────────────▶│
  │     destination:/topic/panic             │
  │     id:sub-0                             │
  │                                         │
```

#### API Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `connect` | `connect(String wsUrl, String token, ConnectionCallback cb)` | Opens WS at `wsUrl + "/websocket"`, sends STOMP CONNECT |
| `disconnect` | `disconnect()` | Sends DISCONNECT frame, closes WebSocket (code 1000) |
| `subscribe` | `<T> String subscribe(String dest, Type type, MessageCallback<T> cb)` | Returns subscription ID `"sub-N"` |
| `unsubscribe` | `unsubscribe(String subscriptionId)` | Sends UNSUBSCRIBE, removes from map |
| `isConnected` | `boolean isConnected()` | Returns connection state |

### 5.2 WebSocketManager — High-Level Singleton

| Property | Value |
|----------|-------|
| **Pattern** | Thread-safe singleton (double-checked locking) |
| **WS URL** | `ws://<BuildConfig.IP_ADDR>:8081/ws` |
| **Connect timeout** | 15 seconds |
| **Read timeout** | 0 (infinite — keeps connection alive) |
| **Write timeout** | 15 seconds |
| **Token source** | `SharedPreferencesManager.getToken()` |

#### Reconnection Strategy

```
Initial delay:    3 seconds
Backoff factor:   2x (exponential)
Maximum delay:    30 seconds
Maximum retries:  10

Timeline:  3s → 6s → 12s → 24s → 30s → 30s → 30s → 30s → 30s → 30s → GIVE UP

On successful reconnect:
  → All active subscriptions are automatically re-sent
  → Callbacks resume without client-side logic
```

### 5.3 STOMP Topics

| Topic | Payload | Subscriber | Purpose |
|-------|---------|------------|---------|
| `/topic/panic` | `PanicResponse` | Admin | Real-time panic alerts |
| `/topic/ride/{id}` | `RideResponse` | Driver, Passenger, Admin | Ride status changes |
| `/topic/support/admin/messages` | `ChatMessage` | Admin | New support messages |
| `/topic/support/admin/chats` | `ChatListUpdate` | Admin | Chat list changes |
| `/topic/support/user/{userId}/notification` | `SupportNotification` | Driver, Passenger | Support reply notifications |

---

## 6. Error Handling

### Backend Error Response Format

```json
{
    "timestamp": "2024-01-15T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Email is required",
    "path": "/api/auth/login"
}
```

### HTTP Status Codes

| Status | Exception | Meaning |
|--------|-----------|---------|
| 400 | `IllegalArgumentException` / Validation | Bad request or invalid input |
| 401 | Unauthorized | Missing or invalid JWT |
| 403 | Forbidden | Insufficient role permissions |
| 404 | `ResourceNotFoundException` | Entity not found |
| 409 | `IllegalStateException` / `EmailAlreadyUsedException` | Business rule violation |
| 500 | Unhandled exception | Internal server error |

### Client-Side Error Handling

```java
// Pattern used across all ViewModels
if (response.isSuccessful() && response.body() != null) {
    data.postValue(response.body());
} else if (response.errorBody() != null) {
    // Parse ErrorResponse from JSON
    String errorJson = response.errorBody().string();
    ErrorResponse error = gson.fromJson(errorJson, ErrorResponse.class);
    errorMessage.postValue(error.getMessage());
} else {
    errorMessage.postValue("Unknown error occurred");
}

// Network failure
@Override
public void onFailure(Call<T> call, Throwable t) {
    errorMessage.postValue("Network error: " + t.getMessage());
}
```
