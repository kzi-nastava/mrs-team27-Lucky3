# Navigation

> Single-activity architecture with Navigation Component, role-based drawer menus, deep-link routing, and programmatic navigation.

---

## 1. Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  DrawerLayout                                              │  │
│  │  ┌─────────────┐  ┌─────────────────────────────────────┐ │  │
│  │  │ Navigation  │  │  CoordinatorLayout                   │ │  │
│  │  │ View        │  │  ┌─────────────────────────────────┐ │ │  │
│  │  │             │  │  │  MaterialToolbar                │ │ │  │
│  │  │ Menu items  │  │  │  [≡] Title        [🔔] [⚙]    │ │ │  │
│  │  │ per role    │  │  ├─────────────────────────────────┤ │ │  │
│  │  │             │  │  │                                 │ │ │  │
│  │  │ Dashboard   │  │  │  NavHostFragment                │ │ │  │
│  │  │ Active Ride │  │  │  (fragment container)           │ │ │  │
│  │  │ History     │  │  │                                 │ │ │  │
│  │  │ Profile     │  │  │  ┌─────────────────────────┐   │ │ │  │
│  │  │ Support     │  │  │  │  Current Fragment        │   │ │ │  │
│  │  │ ...         │  │  │  │  (from nav graph)        │   │ │ │  │
│  │  │ ─────────── │  │  │  └─────────────────────────┘   │ │ │  │
│  │  │ Logout      │  │  │                                 │ │ │  │
│  │  └─────────────┘  │  └─────────────────────────────────┘ │ │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Navigation Graph

**File**: `res/navigation/mobile_navigation.xml`  
**Start destination**: `nav_guest_home`  
**Total destinations**: 39 fragments, 0 dialogs

### 2.1 Guest Flow (Unauthenticated)

```
nav_guest_home ──┬── nav_login ──┬── nav_register ──── nav_register_verification
                 │               │
                 │               └── nav_forgot_password
                 │                        │
                 └── nav_register          ▼
                                   nav_forgot_password_sent
                                          │
                                          ▼
                                   nav_reset_password
                                          │
                                          ▼
                                   nav_reset_password_success
```

### 2.2 Driver Flow

```
nav_driver_dashboard ──── nav_active_ride (rideId)
         │
nav_driver_overview ──── nav_ride_details (rideId)
         │
nav_driver_history ──── nav_ride_details (rideId)
         │
nav_driver_profile
         │
nav_driver_support
```

### 2.3 Passenger Flow

```
nav_passenger_home ──── nav_active_ride (rideId)
         │
nav_passenger_history ──┬── nav_passenger_ride_detail (rideId)
                        │         │
                        │         └── nav_review (rideId, reviewToken)
                        │
                        └── nav_review (rideId, reviewToken)
         │
nav_passenger_profile
         │
nav_passenger_support
         │
nav_passenger_favorites
```

### 2.4 Admin Flow

```
nav_admin_dashboard ──── nav_admin_active_ride_detail (rideId)
         │
nav_admin_ride_history ──── nav_admin_ride_detail (rideId)
         │
nav_admin_drivers
         │
nav_admin_pricing
         │
nav_admin_reports
         │
nav_admin_profile
         │
nav_admin_support
         │
nav_admin_panic
         │
adminRequestsFragment
```

### 2.5 Shared

```
nav_notifications ──── NotificationPanelFragment
nav_review ──── ReviewFragment (deep-link: lucky3://review?token={reviewToken})
nav_active_ride ──── ActiveRideFragment (shared between Driver/Passenger)
```

---

## 3. All Destinations

| # | ID | Label | Fragment Class | Arguments |
|---|-----|-------|---------------|-----------|
| 1 | `nav_guest_home` | Home | `GuestHomeFragment` | — |
| 2 | `nav_login` | Login | `LoginFragment` | — |
| 3 | `nav_register` | Register | `RegisterFragment` | — |
| 4 | `nav_register_verification` | Verify Email | `RegisterVerificationFragment` | — |
| 5 | `nav_forgot_password` | Forgot Password | `ForgotPasswordFragment` | — |
| 6 | `nav_forgot_password_sent` | Forgot Password Sent | `ForgotPasswordSentFragment` | — |
| 7 | `nav_reset_password` | Reset Password | `ResetPasswordFragment` | — |
| 8 | `nav_reset_password_success` | Reset Password Success | `ResetPasswordSuccessFragment` | — |
| 9 | `nav_driver_dashboard` | Dashboard | `DriverDashboardFragment` | — |
| 10 | `nav_driver_overview` | Driver Overview | `DriverOverviewFragment` | — |
| 11 | `nav_driver_history` | Ride History | `DriverRideHistoryFragment` | — |
| 12 | `nav_driver_profile` | Profile | `DriverProfileFragment` | — |
| 13 | `nav_driver_support` | Support | `SupportFragment` | — |
| 14 | `nav_ride_details` | Ride Details | `RideDetailsFragment` | `rideId: Long` |
| 15 | `nav_active_ride` | Active Ride | `ActiveRideFragment` | `rideId: Long` |
| 16 | `nav_passenger_home` | Passenger Home | `PassengerHomeFragment` | — |
| 17 | `nav_passenger_history` | Ride History | `PassengerHistoryFragment` | — |
| 18 | `nav_passenger_ride_detail` | Ride Detail | `PassengerRideDetailFragment` | `rideId: Long` |
| 19 | `nav_passenger_profile` | Profile | `UserProfileFragment` | — |
| 20 | `nav_passenger_support` | Support | `SupportFragment` | — |
| 21 | `nav_passenger_favorites` | Favorites | `PassengerFavoritesFragment` | — |
| 22 | `nav_admin_dashboard` | Admin Dashboard | `AdminDashboardFragment` | — |
| 23 | `nav_admin_ride_history` | Ride History | `AdminRideHistoryFragment` | — |
| 24 | `nav_admin_ride_detail` | Ride Details | `AdminRideDetailFragment` | `rideId: Long` |
| 25 | `nav_admin_active_ride_detail` | Active Ride | `AdminActiveRideDetailFragment` | `rideId: Long` |
| 26 | `nav_admin_drivers` | Drivers | `AdminDriversFragment` | — |
| 27 | `nav_admin_pricing` | Pricing | `AdminPricingFragment` | — |
| 28 | `nav_admin_reports` | Reports | `AdminReportsFragment` | — |
| 29 | `nav_admin_profile` | Admin Profile | `UserProfileFragment` | — |
| 30 | `nav_admin_support` | Support | `SupportFragment` | — |
| 31 | `nav_admin_panic` | Panic Alerts | `AdminPanicFragment` | — |
| 32 | `adminRequestsFragment` | Requests | `AdminRequestsFragment` | — |
| 33 | `nav_review` | Review | `ReviewFragment` | `rideId: Long`, `reviewToken: String` |
| 34 | `nav_notifications` | Notifications | `NotificationPanelFragment` | — |
| 35 | `nav_user_profile` | User Profile | `UserProfileFragment` | — |
| 36 | `nav_admin_profile` | Admin Profile | `UserProfileFragment` | — |
| 37 | `nav_transform` | Transform | `TransformFragment` | — |
| 38 | `nav_reflow` | Reflow | `ReflowFragment` | — |
| 39 | `nav_slideshow` | Slideshow | `SlideshowFragment` | — |

> `UserProfileFragment` is reused across 3 destinations. `SupportFragment` is reused across 3 destinations.

---

## 4. Navigation Actions

| Source | Destination | Action ID | Arguments | Pop Behavior |
|--------|-------------|-----------|-----------|-------------|
| `nav_guest_home` | `nav_login` | `action_nav_guest_home_to_nav_login` | — | — |
| `nav_guest_home` | `nav_register` | `action_nav_guest_home_to_nav_register` | — | — |
| `nav_login` | `nav_register` | `action_nav_login_to_nav_register` | — | — |
| `nav_login` | `nav_forgot_password` | `action_nav_login_to_nav_forgot_password` | — | — |
| `nav_register` | `nav_register_verification` | `action_nav_register_to_nav_register_verification` | — | — |
| `nav_register` | `nav_login` | `action_nav_register_to_nav_login` | — | `popUpTo=nav_guest_home` |
| `nav_forgot_password` | `nav_forgot_password_sent` | `action_nav_forgot_password_to_nav_forgot_password_sent` | — | — |
| `nav_forgot_password_sent` | `nav_reset_password` | `action_nav_forgot_password_sent_to_nav_reset_password` | — | — |
| `nav_reset_password` | `nav_reset_password_success` | `action_nav_reset_password_to_nav_reset_password_success` | — | — |
| `nav_driver_overview` | `nav_ride_details` | `action_nav_driver_overview_to_nav_ride_details` | `rideId` | — |
| `nav_driver_dashboard` | `nav_active_ride` | `action_nav_driver_dashboard_to_nav_active_ride` | `rideId` | — |
| `nav_driver_history` | `nav_ride_details` | `action_nav_driver_history_to_nav_ride_details` | `rideId` | — |
| `nav_admin_dashboard` | `nav_admin_active_ride_detail` | `action_admin_dashboard_to_admin_ride_detail` | `rideId` | — |
| `nav_admin_ride_history` | `nav_admin_ride_detail` | `action_admin_ride_history_to_admin_ride_detail` | `rideId` | — |
| `nav_passenger_home` | `nav_active_ride` | `action_nav_passenger_home_to_nav_active_ride` | `rideId` | — |
| `nav_passenger_history` | `nav_passenger_ride_detail` | `action_passenger_history_to_passenger_ride_detail` | `rideId` | — |
| `nav_passenger_history` | `nav_review` | `action_passenger_history_to_review` | `rideId`, `reviewToken` | — |
| `nav_passenger_ride_detail` | `nav_review` | `action_passenger_ride_detail_to_review` | `rideId`, `reviewToken` | — |

---

## 5. Drawer Menus by Role

### Admin (10 items)

| # | ID | Title | Icon |
|---|-----|-------|------|
| 1 | `nav_admin_dashboard` | Dashboard | `ic_menu_dashboard` |
| 2 | `nav_admin_reports` | Reports | `ic_menu_reports` |
| 3 | `nav_admin_ride_history` | Ride History | `ic_menu_history` |
| 4 | `nav_admin_drivers` | Drivers | `ic_menu_drivers` |
| 5 | `nav_admin_pricing` | Pricing | `ic_menu_pricing` |
| 6 | `nav_admin_profile` | Profile | `ic_menu_profile` |
| 7 | `adminRequestsFragment` | Requests | `ic_requests` |
| 8 | `nav_admin_support` | Support | `ic_menu_support` |
| 9 | `nav_admin_panic` | Panic Alerts | `ic_menu_panic` |
| 10 | `nav_logout` | **Logout** | `ic_menu_logout` |

### Driver (6 items)

| # | ID | Title | Icon |
|---|-----|-------|------|
| 1 | `nav_driver_dashboard` | Dashboard | `ic_menu_dashboard` |
| 2 | `nav_driver_active_ride` | Active Ride | `ic_menu_active_ride` |
| 3 | `nav_driver_overview` | Overview | `ic_menu_overview` |
| 4 | `nav_driver_profile` | Profile | `ic_menu_profile` |
| 5 | `nav_driver_support` | Support | `ic_menu_support` |
| 6 | `nav_logout` | **Logout** | `ic_menu_logout` |

### Passenger (7 items)

| # | ID | Title | Icon |
|---|-----|-------|------|
| 1 | `nav_passenger_home` | Home | `ic_menu_home` |
| 2 | `nav_passenger_active_ride` | Active Ride | `ic_menu_active_ride` |
| 3 | `nav_passenger_history` | Ride History | `ic_menu_history` |
| 4 | `nav_passenger_profile` | Profile | `ic_menu_profile` |
| 5 | `nav_passenger_support` | Support | `ic_menu_support` |
| 6 | `nav_passenger_favorites` | Favorites | `ic_menu_favourites` |
| 7 | `nav_logout` | **Logout** | `ic_menu_logout` |

### Menu Item Styling

| Item | Style | Condition |
|------|-------|-----------|
| **Logout** | Red text + red icon tint | Always |
| **Active Ride** | Gray text (default) | No active ride |
| **Active Ride** | Yellow (`#eab308`) + `●` prefix | Active ride discovered by polling |
| **Panic Alerts** | Red text + red icon tint | Always (admin only) |

---

## 6. Deep Links

### XML Deep Links

| Destination | URI Pattern |
|-------------|-------------|
| `nav_review` | `lucky3://review?token={reviewToken}` |

### FCM Deep Links (Programmatic)

Handled in `MainActivity.handleFcmDeepLink()` via intent extras:

| `navigate_to` Value | Target | Extras |
|---------------------|--------|--------|
| `active_ride` | `nav_active_ride` | `rideId` (Long) |
| `admin_panic` | `nav_admin_panic` | — |
| `support` | Role-aware: `nav_admin_support` / `nav_passenger_support` / `nav_driver_support` | — |
| `notifications` | `nav_notifications` | — |

---

## 7. Top-Level Destinations

These destinations show the hamburger icon (☰) instead of the back arrow (←):

```
Guest:     nav_guest_home
Admin:     nav_admin_dashboard, nav_admin_reports, nav_admin_ride_history,
           nav_admin_drivers, nav_admin_pricing, nav_admin_profile,
           nav_admin_support, nav_admin_panic, adminRequestsFragment
Driver:    nav_driver_dashboard, nav_driver_overview, nav_driver_profile,
           nav_driver_support
Passenger: nav_passenger_home, nav_passenger_history, nav_passenger_profile,
           nav_passenger_support, nav_passenger_favorites
Shared:    nav_active_ride, nav_notifications
Legacy:    nav_transform, nav_reflow, nav_slideshow, nav_settings
```

---

## 8. Session-Based Navigation

### Login → Landing Page

```
Login Success
    │
    ├── Role == DRIVER    → nav_driver_dashboard
    ├── Role == ADMIN     → nav_admin_dashboard
    └── Role == PASSENGER → nav_passenger_home
```

### Session Restore (App Cold Start)

```
MainActivity.onCreate()
    │
    ▼
checkSession(navController)
    │
    ├── SharedPreferencesManager.isLoggedIn() == true
    │     ├── Read stored role
    │     ├── setupNavigationForRole(role)
    │     └── Navigate to role landing page
    │
    └── Not logged in
          └── Stay on nav_guest_home (start destination)
```

### Logout Flow

```
Logout menu item tapped
    │
    ├── Stop ride polling
    ├── AppNotificationManager.stop()
    ├── SharedPreferencesManager.clearAll()
    ├── NotificationStore.clearAll()
    ├── NavigationView.getMenu().clear()
    ├── NavigationView.inflateMenu(R.menu.navigation_drawer)  // reset to default
    └── NavController.navigate(nav_guest_home)
         with popUpTo(nav_graph, inclusive=true)
```
