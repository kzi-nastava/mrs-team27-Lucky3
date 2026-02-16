package com.example.mobile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile.databinding.ActivityMainBinding;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.AppNotificationManager;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.NotificationHelper;
import com.example.mobile.utils.NotificationStore;
import com.example.mobile.utils.SharedPreferencesManager;

import android.view.View;
import android.widget.TextView;

import org.osmdroid.config.Configuration;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final long ACTIVE_RIDE_POLL_INTERVAL = 15_000; // 15 seconds

    private AppBarConfiguration mAppBarConfiguration;
    private SharedPreferencesManager sharedPreferencesManager;

    // Active ride polling
    private Handler activeRidePollHandler;
    private Runnable activeRidePollRunnable;
    private Long currentActiveRideId = null;
    private String currentRole = null;

    // Notification badge in navbar
    private TextView tvNotificationBadge;

    // Notification permission launcher (Android 13+)
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i(TAG, "Notification permission granted");
                } else {
                    Log.w(TAG, "Notification permission denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPrefs
        sharedPreferencesManager = new SharedPreferencesManager(this);

        // Initialize osmdroid configuration - MUST be done before any MapView usage
        Configuration.getInstance().load(getApplicationContext(), 
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Create notification channels for FCM push (safe to call multiple times)
        NotificationHelper.createNotificationChannels(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Wire notification bell and badge
        setupNotificationBell();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        NavigationView navigationView = binding.navView;
        if (navigationView != null) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_guest_home, R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings,
                    R.id.nav_admin_dashboard, R.id.nav_admin_reports, R.id.nav_admin_ride_history, R.id.nav_admin_drivers, R.id.nav_admin_pricing, R.id.nav_admin_profile, R.id.nav_admin_support, R.id.nav_admin_panic,
                    R.id.nav_passenger_home, R.id.nav_passenger_history, R.id.nav_passenger_profile, R.id.nav_passenger_support, R.id.nav_passenger_favorites,
                    R.id.nav_driver_dashboard, R.id.nav_driver_overview, R.id.nav_driver_profile, R.id.nav_driver_support,
                    R.id.nav_active_ride, R.id.nav_notifications)
                    .setOpenableLayout(binding.drawerLayout)
                    .build();
            NavigationUI.setupWithNavController(navigationView, navController);
            
            // Only navigate on fresh start — NOT on configuration changes (e.g. orientation)
            if (savedInstanceState == null) {
                // Restore session
                checkSession(navController);

                // Handle FCM deep-link if app was launched from a notification
                handleFcmDeepLink(getIntent());
            } else {
                // On config change, just restore the role-based drawer menu without navigating
                String role = sharedPreferencesManager.getUserRole();
                if (role != null) {
                    currentRole = role;
                    setupNavigationForRole(role);
                }
            }
        }
    }

    private void checkSession(NavController navController) {
        String token = sharedPreferencesManager.getToken();
        if (token != null && !token.isEmpty()) {
            String role = sharedPreferencesManager.getUserRole();
            if (role != null) {
                currentRole = role;
                setupNavigationForRole(role);
                // Navigate to the role's landing page
                if ("DRIVER".equals(role)) {
                    navController.navigate(R.id.nav_driver_dashboard);
                } else if ("ADMIN".equals(role)) {
                    navController.navigate(R.id.nav_admin_dashboard);
                } else if ("PASSENGER".equals(role)) {
                    navController.navigate(R.id.nav_passenger_home);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        NavigationView navView = findViewById(R.id.nav_view);
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            getMenuInflater().inflate(R.menu.overflow, menu);
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_settings) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_settings);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void setupNavigationForRole(String role) {
        currentRole = role;
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setItemIconTintList(null); // Allow custom icon colors
            navigationView.getMenu().clear();
            if ("DRIVER".equals(role)) {
                navigationView.inflateMenu(R.menu.menu_drawer_driver);
                navigationView.setCheckedItem(R.id.nav_driver_dashboard);
            } else if ("PASSENGER".equals(role)) {
                navigationView.inflateMenu(R.menu.menu_drawer_passenger);
            } else if ("ADMIN".equals(role)) {
                navigationView.inflateMenu(R.menu.menu_drawer_admin);
            }

            // Handle logout color
            MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
            if (logoutItem != null) {
                android.text.SpannableString s = new android.text.SpannableString(logoutItem.getTitle());
                s.setSpan(new android.text.style.ForegroundColorSpan(Color.RED), 0, s.length(), 0);
                logoutItem.setTitle(s);

                // Tint icon red
                if (logoutItem.getIcon() != null) {
                    android.graphics.drawable.Drawable icon = logoutItem.getIcon();
                    icon = DrawableCompat.wrap(icon);
                    DrawableCompat.setTint(icon.mutate(), Color.RED);
                    logoutItem.setIcon(icon);
                }
            }

            // Style active ride item as gray initially
            styleActiveRideMenuItem(false);

            // Style panic alerts menu item red for admins
            if ("ADMIN".equals(role)) {
                stylePanicMenuItem();
            }

            // Handle navigation item clicks
            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // Handle logout
                if (itemId == R.id.nav_logout) {
                    stopActiveRidePolling();
                    AppNotificationManager.getInstance().stop();
                    NotificationStore.getInstance().clearAll();
                    currentActiveRideId = null;
                    currentRole = null;
                    sharedPreferencesManager.logout();
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    navController.navigate(R.id.nav_guest_home);
                    closeDrawer();
                    return true;
                }

                // Handle Active Ride click (programmatic navigation with rideId)
                if (itemId == R.id.nav_driver_active_ride || itemId == R.id.nav_passenger_active_ride) {
                    if (currentActiveRideId != null) {
                        Bundle args = new Bundle();
                        args.putLong("rideId", currentActiveRideId);
                        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                        navController.navigate(R.id.nav_active_ride, args);
                    }
                    closeDrawer();
                    return true;
                }

                // Let NavigationUI handle other items if ids match destinations
                boolean handled = NavigationUI.onNavDestinationSelected(item, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
                if (handled) {
                    closeDrawer();
                }
                return handled;
            });

            // Start polling for active ride if applicable
            if ("DRIVER".equals(role) || "PASSENGER".equals(role)) {
                startActiveRidePolling();
            }

            // Request notification permission for all roles (Android 13+)
            requestNotificationPermission();

            // Start real-time notification manager
            Long userId = sharedPreferencesManager.getUserId();
            if (userId != null && userId > 0) {
                AppNotificationManager.getInstance().start(this, role, userId);
            }
        }
    }

    // ======================== Notification Bell & Badge ========================

    private void setupNotificationBell() {
        // Find badge view
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);

        // Wire bell click to open notification panel
        View btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_notifications);
            });
        }

        // Observe unread count and update badge dynamically
        NotificationStore.getInstance().getUnreadCount().observe(this, count -> {
            if (tvNotificationBadge != null) {
                if (count != null && count > 0) {
                    tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    // ======================== Active Ride Polling ========================

    private void startActiveRidePolling() {
        stopActiveRidePolling();
        activeRidePollHandler = new Handler(Looper.getMainLooper());
        activeRidePollRunnable = new Runnable() {
            @Override
            public void run() {
                pollForActiveRide();
                if (activeRidePollHandler != null) {
                    activeRidePollHandler.postDelayed(this, ACTIVE_RIDE_POLL_INTERVAL);
                }
            }
        };
        // Initial poll immediately
        activeRidePollHandler.post(activeRidePollRunnable);
    }

    private void stopActiveRidePolling() {
        if (activeRidePollHandler != null && activeRidePollRunnable != null) {
            activeRidePollHandler.removeCallbacks(activeRidePollRunnable);
        }
        activeRidePollHandler = null;
        activeRidePollRunnable = null;
    }

    private void pollForActiveRide() {
        String token = sharedPreferencesManager.getToken();
        Long userId = sharedPreferencesManager.getUserId();
        if (token == null || userId == null || userId <= 0) return;

        String bearerToken = "Bearer " + token;
        Long driverId = "DRIVER".equals(currentRole) ? userId : null;
        Long passengerId = "PASSENGER".equals(currentRole) ? userId : null;

        // Check IN_PROGRESS first (highest priority)
        checkRideStatus(driverId, passengerId, "IN_PROGRESS", bearerToken, () -> {
            // Then ACCEPTED
            checkRideStatus(driverId, passengerId, "ACCEPTED", bearerToken, () -> {
                // Then PENDING
                checkRideStatus(driverId, passengerId, "PENDING", bearerToken, () -> {
                    // Then SCHEDULED
                    checkRideStatus(driverId, passengerId, "SCHEDULED", bearerToken, () -> {
                        // No active ride found
                        runOnUiThread(() -> {
                            currentActiveRideId = null;
                            styleActiveRideMenuItem(false);
                        });
                    });
                });
            });
        });
    }

    private void checkRideStatus(Long driverId, Long passengerId, String status, String token, Runnable onNotFound) {
        ClientUtils.rideService.getActiveRides(driverId, passengerId, status, 0, 1, token)
                .enqueue(new Callback<PageResponse<RideResponse>>() {
                    @Override
                    public void onResponse(Call<PageResponse<RideResponse>> call,
                                           Response<PageResponse<RideResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getContent() != null
                                && !response.body().getContent().isEmpty()) {
                            RideResponse ride = response.body().getContent().get(0);
                            runOnUiThread(() -> {
                                currentActiveRideId = ride.getId();
                                styleActiveRideMenuItem(true);
                                // Bridge to WebSocket for real-time ride status updates
                                AppNotificationManager.getInstance().subscribeToRideUpdates(ride.getId());
                            });
                        } else {
                            onNotFound.run();
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to poll active rides (" + status + ")", t);
                        onNotFound.run();
                    }
                });
    }

    private void styleActiveRideMenuItem(boolean hasActiveRide) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView == null) return;

        int activeRideMenuId;
        if ("DRIVER".equals(currentRole)) {
            activeRideMenuId = R.id.nav_driver_active_ride;
        } else if ("PASSENGER".equals(currentRole)) {
            activeRideMenuId = R.id.nav_passenger_active_ride;
        } else {
            return;
        }

        MenuItem activeRideItem = navigationView.getMenu().findItem(activeRideMenuId);
        if (activeRideItem == null) return;

        int color = hasActiveRide ? Color.parseColor("#eab308") : Color.parseColor("#9CA3AF");

        // Tint icon
        if (activeRideItem.getIcon() != null) {
            android.graphics.drawable.Drawable icon = activeRideItem.getIcon();
            icon = DrawableCompat.wrap(icon);
            DrawableCompat.setTint(icon.mutate(), color);
            activeRideItem.setIcon(icon);
        }

        // Tint text
        android.text.SpannableString s = new android.text.SpannableString(
                hasActiveRide ? "Active Ride  ●" : "Active Ride");
        s.setSpan(new android.text.style.ForegroundColorSpan(color), 0, s.length(), 0);
        activeRideItem.setTitle(s);

        // Enable/disable clickability
        activeRideItem.setEnabled(hasActiveRide);
    }

    // ======================== Drawer Helpers ========================

    private void closeDrawer() {
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.close();
        }
    }

    public void openDrawer() {
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.open();
        }
    }

    // ======================== Notification Permission ========================

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void stylePanicMenuItem() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView == null) return;

        MenuItem panicItem = navigationView.getMenu().findItem(R.id.nav_admin_panic);
        if (panicItem == null) return;

        int redColor = Color.parseColor("#EF4444");

        // Tint icon red
        if (panicItem.getIcon() != null) {
            android.graphics.drawable.Drawable icon = panicItem.getIcon();
            icon = DrawableCompat.wrap(icon);
            DrawableCompat.setTint(icon.mutate(), redColor);
            panicItem.setIcon(icon);
        }

        // Tint text red
        android.text.SpannableString s = new android.text.SpannableString("Panic Alerts");
        s.setSpan(new android.text.style.ForegroundColorSpan(redColor), 0, s.length(), 0);
        panicItem.setTitle(s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveRidePolling();
        AppNotificationManager.getInstance().stop();
    }

    // ======================== FCM Deep-Link Handling ========================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Handle Navigation Component deep links (e.g., lucky3://review?token=...)
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.handleDeepLink(intent);
        // Handle FCM notification deep links
        handleFcmDeepLink(intent);
    }

    /**
     * Handles deep-link navigation from FCM notification taps.
     * Checks the intent for {@code navigate_to} extra and navigates accordingly.
     */
    private void handleFcmDeepLink(Intent intent) {
        if (intent == null || intent.getExtras() == null) return;

        String navigateTo = intent.getStringExtra("navigate_to");
        if (navigateTo == null) return;

        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

            switch (navigateTo) {
                case "active_ride":
                    long rideId = intent.getLongExtra("rideId", -1L);
                    if (rideId > 0) {
                        Bundle args = new Bundle();
                        args.putLong("rideId", rideId);
                        navController.navigate(R.id.nav_active_ride, args);
                    }
                    break;
                case "admin_panic":
                    navController.navigate(R.id.nav_admin_panic);
                    break;
                case "support":
                    // Navigate to role-specific support
                    if ("ADMIN".equals(currentRole)) {
                        long chatId = intent.getLongExtra("chatId", -1L);
                        if (chatId > 0) {
                            // Deep-link directly to the specific chat
                            Bundle chatArgs = new Bundle();
                            chatArgs.putLong("chatId", chatId);
                            navController.navigate(R.id.nav_admin_support_chat, chatArgs);
                        } else {
                            navController.navigate(R.id.nav_admin_support);
                        }
                    } else if ("DRIVER".equals(currentRole)) {
                        navController.navigate(R.id.nav_driver_support);
                    } else {
                        navController.navigate(R.id.nav_passenger_support);
                    }
                    break;
                case "notifications":
                    navController.navigate(R.id.nav_notifications);
                    break;
                default:
                    Log.d(TAG, "Unknown deep-link target: " + navigateTo);
                    break;
            }

            // Clear the extras to prevent re-navigation on config changes
            intent.removeExtra("navigate_to");
            intent.removeExtra("rideId");
            intent.removeExtra("chatId");
        } catch (Exception e) {
            Log.e(TAG, "FCM deep-link navigation failed: " + e.getMessage());
        }
    }
}