# Development Guide

> Build setup, environment configuration, coding conventions, testing, project structure, and contribution guidelines.

---

## 1. Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **Android Studio** | Ladybug+ | IDE (Arctic Fox minimum for AGP 8.x) |
| **JDK** | 11+ | Java compilation target |
| **Gradle** | 9.1.0 | Build system (via wrapper) |
| **Android Gradle Plugin** | 8.13.2 | Android build tooling |
| **Android SDK** | API 36 (compileSdk) | Compilation target |
| **Android SDK** | API 35 (minSdk/targetSdk) | Minimum supported version |
| **Git** | Latest | Version control |

---

## 2. Environment Setup

### 2.1 Clone & Configure IP

```bash
git clone <repo-url>
cd mobile
```

Edit `local.properties` to set the backend IP:

```properties
# For Android Emulator (default — backend running on host machine)
IP_ADDR=10.0.2.2

# For physical device (use your machine's LAN IP)
IP_ADDR=192.168.1.100
```

This IP is injected into `BuildConfig.IP_ADDR` at compile time for Retrofit and WebSocket URLs.

### 2.2 Firebase Configuration

The project includes `google-services.json` for FCM. To use your own Firebase project:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package `com.example.mobile`
3. Download `google-services.json` to `mobile/app/`
4. Ensure FCM API is enabled

### 2.3 Backend Dependencies

The mobile app requires the Spring Boot backend running at `http://<IP>:8081`:

```bash
cd ../backend
mvn spring-boot:run    # Starts on port 8081 (from .env)
```

Backend `.env` must have `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, etc.

---

## 3. Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Clean build
./gradlew clean assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Check dependencies
./gradlew dependencies
```

### Build Output

```
mobile/app/build/outputs/apk/debug/app-debug.apk
mobile/app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 4. Project Structure

```
mobile/
├── app/
│   ├── build.gradle                    # App-level build config
│   ├── google-services.json            # Firebase config
│   ├── proguard-rules.pro              # ProGuard rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml      # Permissions, activity, services
│           ├── java/com/example/mobile/
│           │   ├── MainActivity.java    # Single activity host
│           │   ├── models/              # 46 data models (DTOs)
│           │   │   ├── requests/        # API request bodies
│           │   │   └── responses/       # API response models
│           │   ├── services/            # 7 Retrofit interfaces + FCM service
│           │   ├── utils/               # 7 utility singletons
│           │   │   ├── ClientUtils.java          # Retrofit factory
│           │   │   ├── SharedPreferencesManager.java  # Persistent storage
│           │   │   ├── WebSocketManager.java     # WS connection manager
│           │   │   ├── StompClient.java          # STOMP protocol impl
│           │   │   ├── AppNotificationManager.java  # Notification hub
│           │   │   ├── NotificationStore.java    # In-memory notification state
│           │   │   └── MapUtils.java             # OSMDroid helpers
│           │   └── ui/                  # UI layer (fragments + viewmodels)
│           │       ├── admin/           # Admin screens
│           │       ├── auth/            # Password reset, verification
│           │       ├── driver/          # Driver screens
│           │       ├── guest/           # Unauthenticated home
│           │       ├── login/           # Login screen
│           │       ├── notifications/   # Notification panel
│           │       ├── passenger/       # Passenger screens
│           │       ├── profile/         # Profile screens (shared)
│           │       ├── register/        # Registration
│           │       ├── ride/            # Active ride (shared)
│           │       └── support/         # Support chat (shared)
│           └── res/
│               ├── drawable/            # 113 vector/raster assets
│               ├── layout/             # 74 XML layouts
│               ├── menu/               # 6 menu resources
│               ├── navigation/         # 1 nav graph (39 destinations)
│               ├── values/             # Colors, strings, themes, styles
│               └── xml/                # Network security config
├── build.gradle                        # Project-level build config
├── settings.gradle                     # Project settings
├── gradle/
│   ├── libs.versions.toml              # Version catalog
│   └── wrapper/                        # Gradle wrapper
└── docs/                               # This documentation
```

---

## 5. Dependencies

### Core Android

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `androidx.core:core` | 1.16.0 | Android core utilities |
| `androidx.appcompat:appcompat` | 1.7.1 | Backward-compatible UI |
| `com.google.android.material:material` | 1.12.0 | Material Design 3 |
| `androidx.constraintlayout` | 2.2.1 | Constraint-based layouts |
| `androidx.navigation:navigation-*` | 2.9.0 | Navigation Component |
| `androidx.lifecycle:lifecycle-*` | (BOM) | ViewModel + LiveData |

### Networking

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.squareup.retrofit2:retrofit` | 2.9.0 | REST HTTP client |
| `com.squareup.retrofit2:converter-gson` | 2.9.0 | JSON serialization |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP + WebSocket transport |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | Request/response logging |
| `com.google.code.gson:gson` | 2.10.1 | JSON parser |

### Firebase

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.google.firebase:firebase-bom` | 33.8.0 | Firebase version management |
| `com.google.firebase:firebase-messaging` | (BOM) | FCM push notifications |

### Maps

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.osmdroid:osmdroid-android` | 6.1.20 | OpenStreetMap MapView |
| `com.github.MKergworker:osmbonuspack` | 6.9.0 | Routes, POIs, overlays |

### Image Loading

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `de.hdodenhof:circleimageview` | 3.1.0 | Circular profile images |

> **No Glide/Picasso/Coil** — images loaded via `BitmapFactory.decodeStream()` on background threads.

---

## 6. Coding Conventions

### Must Use

| Convention | Details |
|-----------|---------|
| **ListView + BaseAdapter** | ViewHolder pattern with `convertView` recycling |
| **Retrofit `Call<T>.enqueue()`** | For all network calls (auto-async) |
| **`Thread` + `Handler`** | For background work (image loading, etc.) |
| **View Binding** | `FragmentXxxBinding.inflate()` in all fragments |
| **MVVM** | ViewModel + LiveData for state management |
| **Service interface + `@Header`** | Explicit auth header per endpoint |
| **Java** | All code in Java (project does not use Kotlin) |

### Must NOT Use

| Anti-Pattern | Reason |
|-------------|--------|
| **RecyclerView** | Project standardized on ListView + BaseAdapter |
| **Glide / Picasso / Coil** | Manual image loading with BitmapFactory |
| **Kotlin Coroutines** | No `suspend`, `launch`, `viewModelScope` |
| **RxJava** | No `Observable`, `Single`, `Flowable` |
| **ExecutorService** | Use `Thread` + `Runnable` instead |
| **OkHttp Auth Interceptor** | Auth via explicit `@Header("Authorization")` |
| **NgModules** | N/A (Not an Angular project) |

### Code Style

```java
// ViewModel pattern
public class MyViewModel extends AndroidViewModel {
    private final MutableLiveData<MyData> data = new MutableLiveData<>();

    public LiveData<MyData> getData() { return data; }

    public void loadData() {
        String token = "Bearer " + SharedPreferencesManager.getToken();
        ClientUtils.myService.getData(token).enqueue(new Callback<MyData>() {
            @Override
            public void onResponse(Call<MyData> call, Response<MyData> response) {
                if (response.isSuccessful()) {
                    data.postValue(response.body());
                }
            }
            @Override
            public void onFailure(Call<MyData> call, Throwable t) {
                // Handle error
            }
        });
    }
}

// Fragment pattern
public class MyFragment extends Fragment {
    private FragmentMyBinding binding;
    private MyViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        binding = FragmentMyBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);

        viewModel.getData().observe(getViewLifecycleOwner(), data -> {
            binding.textView.setText(data.getName());
        });

        viewModel.loadData();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

### Image Loading Pattern

```java
// Background thread image loading (no Glide!)
new Thread(() -> {
    try {
        URL url = new URL(imageUrl);
        InputStream in = url.openStream();
        Bitmap bitmap = BitmapFactory.decodeStream(in);
        in.close();

        // Post to UI thread
        new Handler(Looper.getMainLooper()).post(() -> {
            imageView.setImageBitmap(bitmap);
        });
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();

// Base64 profile picture decoding
byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
imageView.setImageBitmap(bitmap);
```

---

## 7. Permissions

Declared in `AndroidManifest.xml`:

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | Network access (REST, WebSocket, FCM) |
| `ACCESS_NETWORK_STATE` | Network availability check |
| `ACCESS_FINE_LOCATION` | GPS for maps (driver tracking) |
| `ACCESS_COARSE_LOCATION` | Approximate location |
| `READ_EXTERNAL_STORAGE` | Profile image selection from gallery |
| `POST_NOTIFICATIONS` | System notification display (API 33+) |

### Runtime Permission Request

```java
// POST_NOTIFICATIONS (Android 13+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            NOTIFICATION_PERMISSION_CODE
        );
    }
}
```

---

## 8. Build Configuration

### app/build.gradle Key Settings

```groovy
android {
    namespace 'com.example.mobile'
    compileSdk 36

    defaultConfig {
        applicationId "com.example.mobile"
        minSdk 35
        targetSdk 36
        versionCode 1
        versionName "1.0"

        // IP injected from local.properties
        buildConfigField "String", "IP_ADDR",
            "\"${project.findProperty('IP_ADDR') ?: '10.0.2.2'}\""
    }

    buildFeatures {
        viewBinding true
        buildConfig true
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
}
```

### Version Catalog (`gradle/libs.versions.toml`)

Centralized version management — all dependency versions declared once, referenced in `build.gradle` as `libs.xxx`.

---

## 9. Debugging Tips

### Network Debugging

- OkHttp logging interceptor at `Level.BODY` — all requests/responses logged to Logcat
- Filter Logcat by `OkHttp` tag for network traffic
- Check `BuildConfig.IP_ADDR` value if connection fails

### WebSocket Debugging

- StompClient logs connection state changes
- Filter Logcat by `StompClient` or `WebSocketManager`
- Verify JWT token is not expired
- Check that backend WebSocket endpoint is reachable: `ws://<IP>:8081/ws/websocket`

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Connection refused | Wrong IP in `local.properties` | Set to `10.0.2.2` for emulator, LAN IP for device |
| 401 on all requests | Expired JWT | Re-login to get a new token |
| WebSocket won't connect | Token null/empty | Ensure `SharedPreferencesManager.getToken()` returns a value |
| FCM not working | Missing `google-services.json` | Add Firebase config file to `app/` |
| Build fails on `LocalDateTime` | Wrong Java version | Ensure `sourceCompatibility = JavaVersion.VERSION_11` |
| Map tiles not loading | No internet | OSMDroid uses online tile source (CartoDB Dark Matter) |

---

## 10. Map Configuration

| Property | Value |
|----------|-------|
| **Tile source** | CartoDB Dark Matter (HTTPS) |
| **Default center** | Novi Sad (45.2517, 19.8369) |
| **Default zoom** | 14 |
| **Routing engine** | OSRM (https://router.project-osrm.org) |
| **Route rendering** | OSMBonusPack `RoadManager` overlay |
| **Vehicle icons** | Custom SVG drawables with color tinting |

```java
// OSMDroid initialization
Configuration.getInstance().setUserAgentValue(getPackageName());
mapView.setTileSource(new XYTileSource(
    "CartoDB Dark Matter",
    0, 19, 256, ".png",
    new String[]{"https://a.basemaps.cartocdn.com/dark_all/"}
));
mapView.getController().setZoom(14.0);
mapView.getController().setCenter(new GeoPoint(45.2517, 19.8369));
```

---

## 11. Git Workflow

```
main
  └── develop
        ├── feature/xxx
        ├── bugfix/xxx
        └── hotfix/xxx
```

### Commit Convention

```
type(scope): description

feat(mobile): add notification panel with real-time badge
fix(mobile): correct deep-link routing for support notifications
docs(mobile): add comprehensive architecture documentation
refactor(mobile): extract WebSocket manager to singleton
```

### Branch Naming

```
feature/mobile-notification-system
bugfix/mobile-login-token-expiry
hotfix/mobile-crash-on-null-ride
```
