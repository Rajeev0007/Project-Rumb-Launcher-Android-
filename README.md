# Rumb Launcher V2

Rumb Launcher is a private, offline Android home-screen replacement built with native Kotlin and Jetpack Compose. V2 keeps Android 8.0 (API 26) support while moving the launcher to a reactive, callback-driven architecture.

## Highlights

- Callback-driven `LauncherApps` catalog with accessible personal and managed profiles
- Responsive searchable app drawer with stable item keys and profile labels
- Multiple persisted home pages, configurable 4–8 column/row grids, and a five-app dock
- Long-press drag placement; dropping one app on another creates a folder
- Layout lock, hidden apps, folders, and resilient loading/empty/error states
- Asynchronous density-aware icon rendering with a bounded 16 MB bitmap cache
- System, light, dark, and AMOLED themes; Material You colors on Android 12+
- System, circle, rounded, and squircle icon masks
- Optional themed monochrome icons on Android 13+ when an app provides artwork
- Large, compact, and minimal clocks that follow the device 12/24-hour setting
- Configurable swipe-up, swipe-down, and background double-tap actions
- Optional notification badges through Android notification-listener access
- App shortcuts when Rumb is the active shortcut host/default launcher
- Versioned JSON layout/settings export and validated import through the Storage Access Framework
- Automatic one-time migration from V1 favorites, grid, icon-size, and theme preferences
- Redesigned adaptive R icon with Android 13 monochrome artwork

Rumb requests no network or broad storage permission. It includes no analytics, advertising, account system, or notification-content persistence. Automatic Android backup is disabled because launcher layouts can contain device/profile-specific identities; use the explicit JSON export instead.

## Using the launcher

1. Install and open Rumb Launcher.
2. Select Rumb when Android asks for a home app, or open **Settings → System → Choose default home app**.
3. Swipe up on an empty part of the home screen to open all apps.
4. Long-press an app in the drawer to add it to home or the dock, hide it, open app info, uninstall it, or use an available app shortcut.
5. Long-press and drag a home app to move it. Drop it on another app to create a folder.

Notification badges are off by default. Enabling them still requires explicit approval in Android's notification-access settings. Rumb only keeps eligible per-package counts in memory.

## Backup format

**Settings → Backup** exports a versioned JSON document through Android's document picker. Imports are limited to 1 MB and validate the Rumb format/version before replacing state. Backups use portable personal/profile roles instead of copying device-local profile serials. Widget IDs are never exported.

## Intentional limitations

- **Widgets are disabled in this build.** Correct widget hosting requires provider-aware spans, resize negotiation, binding recovery, and strict widget-ID cleanup. The settings screen explains this instead of exposing an unreliable picker.
- **Pinch gestures are disabled.** They conflict with page and app touch handling in the current interaction model.
- **Opening the notification shade is not offered as a gesture.** Android has no supported public API for third-party launchers to expand it.
- Rumb does not implement or claim to replace Android's system lock screen.
- Folder names are currently fixed to “Folder”; folders can be opened or removed.

## Architecture

- `domain/` — Android-light launcher IDs, apps, layout items, and settings
- `data/` — synchronized StateFlow-backed versioned JSON persistence and V1 migration
- `platform/apps/` — `LauncherApps` catalog, profile launching, and shortcuts
- `platform/icons/` — asynchronous density/themed icon cache
- `platform/notifications/` — optional in-memory notification badge service
- `feature/home`, `feature/drawer`, `feature/settings`, `feature/appactions` — split Compose features
- `LauncherViewModel.kt` — immutable combined UI state and launcher actions

## Requirements

- Android 8.0 (API 26) or newer
- Android SDK 35
- JDK 17

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
