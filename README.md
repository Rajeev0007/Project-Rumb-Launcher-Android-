# Rumb Launcher 2.1.0

Rumb Launcher is the hardened standard edition of a private, offline Android home-screen replacement built with native Kotlin and Jetpack Compose. It supports Android 8.0 (API 26) and newer.

## Highlights

- Callback-driven `LauncherApps` catalog with accessible personal and managed profiles
- Responsive searchable app drawer with stable item keys and profile labels
- Multiple persisted home pages, configurable 4–8 column/row grids, and a five-app dock
- Span-aware home placement for apps, folders, and Android AppWidgets without overlapping occupied cells
- Long-press app/folder drag placement; dropping one app on another creates a folder
- Editable folder names and membership, with safe folder dissolution when only one app remains
- Layout lock, hidden apps, folders, and resilient loading/empty/error states
- Asynchronous density-aware icon rendering with a bounded 16 MB bitmap cache
- System, light, dark, and AMOLED themes; Material You colors on Android 12+
- System, circle, rounded, and squircle icon masks
- Optional themed monochrome icons on Android 13+ when an app provides artwork
- Locale- and time-zone-aware clocks with style, alignment, 12/24-hour format, font, size, date, and seconds controls
- Configurable swipe-up, swipe-down, background double-tap, and true two-pointer pinch actions
- App shortcuts when Rumb is the active shortcut host/default launcher
- Versioned JSON layout/settings export and validated import through the Storage Access Framework
- Automatic one-time migration from V1 favorites, grid, icon-size, and theme preferences
- Adaptive launcher icon with Android 13 monochrome artwork

Rumb requests no network or broad storage permission. It includes no analytics, advertising, or account system. Automatic Android backup is disabled because launcher layouts can contain device/profile-specific identities; use the explicit JSON export instead.

## Hardened standard edition

This edition removes notification access and its associated indicators, settings, service, and persisted preference. It also removes the launcher's direct package-removal action. Long-press actions still provide Android's App info screen, where the operating system controls any package-management choices.

The application explicitly disables cleartext traffic and retains no `INTERNET` permission. Release builds enable code minification and resource shrinking with Android's optimized defaults.

These changes reduce sensitive capability surface and common sideloading heuristics. Play Protect and other security products may still inspect or warn about sideloaded apps from unknown sources. This project does not claim approval by any antivirus or app-store security system.

## Using the launcher

1. Install and open Rumb Launcher.
2. Select Rumb when Android asks for a home app, or open **Settings → System → Choose default home app**.
3. Swipe up on an empty part of the home screen to open all apps.
4. Long-press an app in the drawer to add it to home or the dock, hide it, open App info, or use an available app shortcut.
5. Long-press and drag a home app or folder to move it. Drop an app on another app to create a folder, or on a folder to add it.
6. Use **Settings → Home layout → Add widget** to open Android's widget picker. Rumb runs provider configuration when required and places the widget in the first free rectangle across available pages.
7. Open a folder to rename it or remove individual apps. Locking the layout disables folder edits and all home-item changes.

Widget remove buttons appear while the layout is unlocked. If Android no longer provides a saved widget binding, Rumb keeps its occupied area visible as an unavailable placeholder so it can be removed explicitly.

## Backup format

**Settings → Backup** exports a versioned JSON document through Android's document picker. Imports are limited to 1 MB and validate the Rumb format/version before replacing state. Backups use portable personal/profile roles instead of copying device-local profile serials. Widgets and their device-local widget IDs are excluded; widgets must be added again on the destination device. Gesture and clock preferences are included.

## Platform limitations

- Widget selection, binding approval, and provider configuration depend on system/provider activities. Rumb cancels and deletes the allocated widget ID when one of those activities is missing, rejected, or returns invalid data.
- Widget bindings are device-local Android state and cannot be restored from Rumb's portable JSON backup.
- Rumb does not replace, bypass, or weaken Android's secure system lock screen. The layout-lock gesture only protects launcher item editing.

## Architecture

- `domain/` — Android-light launcher IDs, apps, span-aware layout items, occupancy helpers, and settings
- `data/` — synchronized StateFlow-backed versioned JSON persistence and V1 migration
- `platform/apps/` — `LauncherApps` catalog, profile launching, and shortcuts
- `platform/icons/` — asynchronous density/themed icon cache
- `feature/home`, `feature/drawer`, `feature/settings`, `feature/appactions` — split Compose features
- `MainActivity.kt` — activity-result handling and lifecycle-owned `AppWidgetHost`
- `LauncherViewModel.kt` — immutable combined UI state and launcher actions

## Requirements

- Android 8.0 (API 26) or newer
- Android SDK 35
- JDK 17

## Build

Build a local debug APK with:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Build the hardened release variant with:

```bash
./gradlew assembleRelease
```

Without signing environment variables, Gradle intentionally creates an unsigned local release APK under `app/build/outputs/apk/release/`.

To sign the release, provide all four environment variables before running the release build:

- `RUMB_KEYSTORE_PATH` — path to the release keystore
- `RUMB_KEYSTORE_PASSWORD` — keystore password
- `RUMB_KEY_ALIAS` — key alias
- `RUMB_KEY_PASSWORD` — key password

The build script does not log or embed these values. Configuration fails with a clear error if only some variables are present. Keep signing credentials in a secure CI secret store or local environment, never in the repository.
