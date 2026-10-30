# Rumb Launcher 2.2.0

Rumb Launcher is a private, offline Android home-screen replacement built with native Kotlin and Jetpack Compose. Version 2.2 introduces an original visual system and home-customization flow while preserving the hardened standard edition. It supports Android 8.0 (API 26) and newer.

## Visual styles

Rumb includes four persisted visual presets. Each uses layered gradients, translucent color, edge highlights, borders, and restrained shadows instead of continuous wallpaper blur, so the launcher remains smooth and readable over arbitrary wallpaper.

- **Aura Glass** — violet light with layered glossy surfaces
- **Soft Panel** — warm, softly elevated panels with calm contrast
- **Crystal Dock** — cool crystal edges, brighter accents, and deeper dock treatment
- **Pure Minimal** — reduced chrome, quiet contrast, and tighter geometry

The selected style applies live to Home, the floating dock, App Library, customization tray, and Settings. The style is included in versioned export/import and defaults safely for older backups.

## Customize Home

Normal Home stays clean. Long-press an unconsumed empty area to enter Customize Home. App taps, widgets, page swipes, item drag and drop, double tap, swipe gestures, and two-pointer pinch retain priority over the background hold detector.

Customize Home raises and scales the live preview above a bottom tray with:

- **Visual style** — preview and apply any preset immediately
- **Wallpaper** — open Android's system wallpaper picker
- **Widgets** — start the existing Android widget selection and configuration flow
- **Layout & clock** — open the full grouped Settings screen
- **Done** — return to the clean Home screen

Back leaves customization before leaving Home. Layout lock continues to block item, widget, folder, dock, page, and grid mutations, while visual style, wallpaper, and Settings remain available with clear feedback.

## Highlights

- Callback-driven `LauncherApps` catalog with accessible personal and managed profiles
- Rounded edge-to-edge App Library panel with a floating search pill, responsive adaptive grid, profile labels, and long-press actions
- Multiple persisted home pages, configurable 4–8 column/row grids, page indicator, and a five-app floating dock
- Dock contains pinned apps and a dedicated all-apps affordance; it does not keep permanent wallpaper or settings actions
- Span-aware home placement for apps, folders, and Android AppWidgets without overlapping occupied cells
- Long-press app/folder drag placement with haptic feedback; dropping one app on another creates a folder
- Editable folder names and membership, with safe folder dissolution when only one app remains
- Layout lock, hidden apps, folders, and resilient loading, onboarding, empty, and error states
- Asynchronous density-aware icon rendering with a bounded 16 MB bitmap cache
- System, light, dark, and AMOLED themes; Material You colors on Android 12+
- System, circle, rounded, and squircle icon masks
- Optional themed monochrome icons on Android 13+ when an app provides artwork
- Locale- and time-zone-aware clocks with style, alignment, 12/24-hour format, font, size, date, and seconds controls
- Configurable swipe-up, swipe-down, background double-tap, and true two-pointer pinch actions
- Short route and edit-mode transitions that do not animate or blur wallpaper bitmaps
- App shortcuts when Rumb is the active shortcut host/default launcher
- Versioned JSON layout/settings export and validated import through the Storage Access Framework
- Automatic migration from older favorites/settings and schema defaults
- Original geometric R/orbit launcher mark with violet/blue adaptive, round, legacy, and Android 13 monochrome assets

## Hardened capability posture

Rumb requests no network, broad storage, notification-listener, or package-removal capability. It contains no analytics, advertising, or account system. The manifest has no `INTERNET` permission, cleartext traffic is disabled, automatic Android backup is disabled, and the launcher never sends `ACTION_DELETE`.

Explicit JSON export is used instead of Android backup because launcher layouts can contain device/profile-specific identities. Imports are capped at 1 MB and validated before state replacement. Release builds keep code minification and resource shrinking enabled and use the all-or-none environment-based signing configuration.

These choices reduce sensitive capability surface and common sideloading heuristics. Play Protect and other security products may still inspect or warn about sideloaded apps from unknown sources. This project does not claim approval by an antivirus or app-store security system.

## Using the launcher

1. Install and open Rumb Launcher.
2. Select Rumb when Android asks for a home app, or open **Settings → System → Choose default home app**.
3. Swipe up on an empty part of Home to open App Library.
4. Long-press empty Home space to change style, wallpaper, widgets, layout, or clock.
5. Long-press an app in App Library to add it to Home or the dock, hide it, open App info, or use an available app shortcut.
6. Long-press and drag a Home app or folder to move it. Drop an app on another app to create a folder, or on a folder to add it.
7. Add widgets from Customize Home or **Settings → Home layout**. Android handles widget selection and provider configuration; Rumb places the widget in the first free rectangle across available pages.
8. Open a folder to rename it or remove individual apps. Locking the layout disables folder edits and all Home-item changes.

Widget remove buttons appear while the layout is unlocked. If Android no longer provides a saved widget binding, Rumb keeps its occupied area visible as an unavailable placeholder so it can be removed explicitly.

## Backup format

Settings exports a versioned JSON document through Android's document picker. Imports validate the Rumb format, supported schema, grid bounds, unique item IDs, placement, and app membership before replacing state. Backups use portable personal/profile roles instead of device-local profile serials. Widgets and their device-local IDs are excluded and must be added again on the destination device. Visual style, gestures, clock, appearance, and layout preferences are included.

## Platform limitations

- Widget selection, binding approval, and provider configuration depend on system/provider activities. Rumb cancels and deletes the allocated widget ID when one of those activities is missing, rejected, or returns invalid data.
- Widget bindings are device-local Android state and cannot be restored from Rumb's portable JSON backup.
- Rumb does not replace, bypass, or weaken Android's secure system lock screen. Android owns lock-screen authentication and policy. Rumb's layout lock only protects launcher editing.

## Architecture

- `domain/` — launcher IDs, apps, span-aware items, occupancy helpers, and versioned settings
- `data/` — synchronized StateFlow-backed JSON persistence, validation, and migration
- `platform/apps/` — profile-aware `LauncherApps` catalog, launching, and shortcuts
- `platform/icons/` — asynchronous density/themed icon cache
- `ui/components/VisualMaterials.kt` — shared visual tokens, backdrops, glass surfaces, dock, drawer, and chips
- `feature/home` — clean Home, gesture arbitration, pages, widgets, drag/drop, and dock
- `feature/customize` — edit tray and visual-style previews
- `feature/drawer`, `feature/settings`, `feature/appactions` — App Library, grouped settings, and actions
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

To sign a release, provide all four environment variables: `RUMB_KEYSTORE_PATH`, `RUMB_KEYSTORE_PASSWORD`, `RUMB_KEY_ALIAS`, and `RUMB_KEY_PASSWORD`. Configuration fails if only some values are present. The build script does not log or embed the credentials; keep them in a secure CI secret store or local environment.
