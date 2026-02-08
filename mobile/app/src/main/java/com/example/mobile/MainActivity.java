package com.example.mobile;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile.databinding.ActivityMainBinding;
import com.example.mobile.utils.SharedPreferencesManager;

import org.osmdroid.config.Configuration;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    // Helper to check session
    private SharedPreferencesManager sharedPreferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPrefs
        sharedPreferencesManager = new SharedPreferencesManager(this);

        // Initialize osmdroid configuration - MUST be done before any MapView usage
        Configuration.getInstance().load(getApplicationContext(), 
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        NavigationView navigationView = binding.navView;
        if (navigationView != null) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_guest_home, R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings,
                    R.id.nav_admin_dashboard, R.id.nav_admin_reports, R.id.nav_admin_drivers, R.id.nav_admin_pricing, R.id.nav_admin_profile, R.id.nav_admin_support,
                    R.id.nav_passenger_home, R.id.nav_passenger_history, R.id.nav_passenger_profile, R.id.nav_passenger_support,
                    R.id.nav_driver_overview, R.id.nav_driver_profile, R.id.nav_driver_support)
                    .setOpenableLayout(binding.drawerLayout)
                    .build();
            NavigationUI.setupWithNavController(navigationView, navController);
            
            // Restore session
            checkSession(navController);
        }
    }

    private void checkSession(NavController navController) {
        String token = sharedPreferencesManager.getToken();
        if (token != null && !token.isEmpty()) {
            String role = sharedPreferencesManager.getUserRole();
            if (role != null) {
                setupNavigationForRole(role);
                // Optionally navigate to dashboard if on start destination
                // For now, we just setup the menu so the user can navigate.
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
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setItemIconTintList(null); // Allow custom icon colors
            navigationView.getMenu().clear();
            if ("DRIVER".equals(role)) {
                navigationView.inflateMenu(R.menu.menu_drawer_driver);
                // Set Overview as checked by default for driver
                navigationView.setCheckedItem(R.id.nav_driver_overview);
            } else if ("PASSENGER".equals(role)) {
                navigationView.inflateMenu(R.menu.menu_drawer_passenger);
            } else if ("ADMIN".equals(role)) {
                navigationView.inflateMenu(R.menu.menu_drawer_admin);
            }

            // Handle logout color
            MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
            if (logoutItem != null) {
                android.text.SpannableString s = new android.text.SpannableString(logoutItem.getTitle());
                s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.RED), 0, s.length(), 0);
                logoutItem.setTitle(s);

                // Tint icon red
                if (logoutItem.getIcon() != null) {
                    android.graphics.drawable.Drawable icon = logoutItem.getIcon();
                    icon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(icon.mutate(), android.graphics.Color.RED);
                    logoutItem.setIcon(icon);
                }
            }

            // Handle logout
            navigationView.setNavigationItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_logout) {
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    navController.navigate(R.id.nav_guest_home);
                    androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
                    if (drawer != null) {
                        drawer.close();
                    }
                    return true;
                }
                // Let NavigationUI handle other items if ids match destinations
                boolean handled = NavigationUI.onNavDestinationSelected(item, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
                if (handled) {
                    androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
                    if (drawer != null) {
                        drawer.close();
                    }
                }
                return handled;
            });
        }
    }

    public void openDrawer() {
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.open();
        }
    }
}