# Lucky3 Mobile — Android Client

> Ride-sharing Android application for passengers, drivers, and administrators.

## Quick Links

| Document | Description |
|----------|-------------|
| [Architecture](ARCHITECTURE.md) | System architecture, module structure, design patterns |
| [Data Flow](DATA_FLOW.md) | Data flow diagrams — backend, WebSocket, polling, FCM |
| [Networking](NETWORKING.md) | REST API layer, WebSocket/STOMP, Retrofit services |
| [Notifications](NOTIFICATIONS.md) | Notification system — WebSocket, FCM, in-app panel |
| [Navigation](NAVIGATION.md) | Navigation graph, screens, role-based drawer menus |
| [Authentication](AUTH.md) | Auth flow — login, JWT, token lifecycle, FCM registration |
| [Development](DEVELOPMENT.md) | Build setup, environment config, coding conventions |

---

## Overview

Lucky3 Mobile is a native Android application (Java, compileSdk 36) that provides three distinct user experiences within a single APK:

- **Passenger** — Request rides, track live ride progress, review drivers, manage favorite routes
- **Driver** — Go online/offline, accept rides, navigate to passengers, earn and track stats
- **Admin** — Monitor all active rides, manage drivers, handle pricing, respond to panic alerts

The app communicates with a **Spring Boot backend** over REST (Retrofit 2.9), receives real-time updates via **STOMP-over-WebSocket** (custom OkHttp client), and handles background push via **Firebase Cloud Messaging** (data-only messages).

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 (target compatibility: Java 11) |
| Min SDK | 35 (Android 15) |
| Target SDK | 36 |
| Build | Gradle 9.1.0, AGP 8.13.2 |
| UI Framework | Android Views, ViewBinding, Material Design 3 |
| Architecture | MVVM (ViewModel + LiveData), Navigation Component |
| Networking | Retrofit 2.9 + OkHttp 4.12 + Gson 2.10.1 |
| Real-time | Custom STOMP 1.1 client over OkHttp WebSocket |
| Push | Firebase Cloud Messaging (data-only messages) |
| Maps | OSMDroid 6.1.20 + OSMBonusPack 6.9.0 (OSRM routing) |
| Lists | ListView + BaseAdapter (ViewHolder pattern) |
| Navigation | Navigation Component with DrawerLayout |

## Project Structure

```
mobile/
├── docs/                          ← You are here
│   ├── README.md                  ← This file
│   ├── ARCHITECTURE.md
│   ├── DATA_FLOW.md
│   ├── NETWORKING.md
│   ├── NOTIFICATIONS.md
│   ├── NAVIGATION.md
│   ├── AUTH.md
│   └── DEVELOPMENT.md
├── app/
│   ├── build.gradle               ← Module-level build config
│   ├── google-services.json       ← Firebase config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/mobile/
│       │   ├── MainActivity.java       ← Single Activity host
│       │   ├── models/                 ← 46 DTOs and models
│       │   ├── services/              ← 7 Retrofit interfaces + FCM service
│       │   ├── utils/                 ← Singletons, helpers, WebSocket
│       │   ├── viewmodels/            ← 12 application ViewModels
│       │   └── ui/                    ← 30+ Fragments by feature
│       │       ├── admin/
│       │       ├── auth/
│       │       ├── driver/
│       │       ├── guest/
│       │       ├── login/
│       │       ├── maps/
│       │       ├── notifications/
│       │       ├── passenger/
│       │       ├── profile/
│       │       ├── register/
│       │       ├── ride/
│       │       └── support/
│       └── res/
│           ├── layout/                ← 74 layout files
│           ├── drawable/              ← 113 drawables
│           ├── menu/                  ← 6 menu definitions
│           ├── navigation/            ← Navigation graph (39 destinations)
│           └── values/                ← Colors, strings, themes, styles
├── build.gradle                   ← Project-level build config
├── local.properties               ← Backend IP address
└── gradlew / gradlew.bat          ← Gradle wrapper
```

## Screens at a Glance

| Role | Screens |
|------|---------|
| **Guest** | Home (map + vehicles + estimation), Login, Register, Forgot/Reset Password |
| **Passenger** | Home (request ride), Active Ride (live tracking), Ride History, Ride Detail, Review, Favorites, Profile, Support, Notifications |
| **Driver** | Dashboard (stats + toggle), Active Ride (live tracking), Overview (filtered stats), Ride History, Ride Details, Profile, Support, Notifications |
| **Admin** | Dashboard (active rides + stats), Active Ride Detail (live admin view), Ride History, Ride Detail, Drivers (CRUD), Change Requests, Pricing, Panic Alerts, Reports, Profile, Support, Notifications |
