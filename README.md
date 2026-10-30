# Rumb Launcher Next 3.0.0

Rumb Launcher Next is a private, offline Android home-screen replacement built with native Kotlin and Jetpack Compose. V3 uses the supplied screenshots only as structural reference for a new AMOLED-first library, wallpaper-first Home, and safe quick-control launcher. Its surfaces, geometry, color treatment, and artwork are original rather than copies of another launcher or vendor interface. Android 8.0 (API 26) and newer are supported.

## Install identity and the package-conflict fix

V3 installs as **Rumb Launcher Next** with application ID `com.rumb.launcher.next`. The Kotlin namespace and source package remain `com.rumb.launcher`.

Android refuses to update an installed package when the incoming APK has the same application ID but a different signing certificate. That commonly appears as an app-not-installed or package-conflict error. Giving V3 a new application ID lets it install beside an older `com.rumb.launcher` build, including an old differently signed debug APK. Android treats the two IDs as separate apps, so select **Rumb Launcher Next** again in the system Home chooser. Private app data does not transfer automatically between package IDs; use Rumb's explicit JSON export/import where the older build supports it.

The Play Protect screenshot concerns the **old v2.0.1 debug APK**, with its old package identity and debug signing context. The current deliverable is **V3.0.0** (`versionCode 7`) under `com.rumb.launcher.next`. A locally built debug APK is still debug-signed; the hardened release is the unminified release variant signed only when all four release-signing variables are supplied. Package identity and release signing distinguish the current release from the old debug artifact, but no project can promise how Play Protect or an OEM scanner will classify a sideloaded APK.

## V3 Home

Normal Home puts the wallpaper first. The old central onboarding card is gone. Optional clock and user-placed apps, folders, and widgets remain, with a compact centered dock for pinned apps and the all-apps control. Readability gradients stay subtle and continue to follow the selected visual preset.

Long-pressing unconsumed empty Home space is the only entry to the customization tray. App taps, widgets, page swipes, item drag and drop, double tap, swipe gestures, and two-pointer pinch retain priority. Layout lock still blocks item, widget, folder, dock, page, and grid changes while leaving appearance, wallpaper, and Settings available.

`ClockStyle.HIDDEN` removes the clock receiver and timer work as well as the visible clock. Hidden is the default only for genuinely fresh V3 state. Existing persisted settings, legacy migrations, and older backups retain their previous clock and gesture fallbacks. The backup schema stores the new clock and gesture choices.

## AMOLED Apps and Widgets library

The full-screen black library has large **Apps** and **Widgets** tabs with a clear selected state.

### Apps

- Rounded dark search field matching app labels and package names
- Four columns by default on phones, with a wider responsive grid on tablets
- Locale-aware ascending or descending alphabetic sort
- Right-side `#`/A–Z index; selecting an available letter animates to the first matching app row
- Cached density-aware icons, personal/work profile labels, launch, long-press actions, loading, empty, refresh, and error handling
- Accessibility labels for navigation, sorting, index jumps, launch targets, and profile context

### Widgets

The Widgets tab queries `AppWidgetManager.installedProviders` off the main thread, groups and sorts entries by provider app label, and uses a responsive two-column phone layout. Each dark card shows provider preview art when Android supplies it, a badged provider-icon fallback otherwise, the widget name, and an approximate home-grid span. Preview conversion runs asynchronously and uses a bounded 24 MB bitmap cache.

Selecting a card asks the activity to bind that exact installed component. The existing host transaction remains responsible for allocating the widget ID, preserving the provider profile, requesting Android's bind approval when needed, launching provider configuration, validating the final provider, and deleting the allocated ID after cancellation or failure. The **System picker** action keeps the original generic Android widget-picker path. Widget IDs and bindings remain device-local and are excluded from portable backups.

## Safe Quick Panel

Fresh V3 state maps swipe-down to `OPEN_QUICK_PANEL`; existing and imported state keeps its saved choice. The gesture is also available in Settings.

The original dark panel shows current date and time, two large Internet and Bluetooth cards, and circular actions for Display, Sound, Battery, Wallpaper, and Home settings. Every action opens a supported Android Settings Panel or settings screen with API guards, intent resolution, fallback handling, and user-visible failure handling. Back returns to Home, and the panel uses the same short route transition as the library.

The panel is a launcher for Android-owned controls. It does not display invented toggle state, change protected radios or system settings, use reflection or hidden APIs, expand the notification shade, or request extra permissions.

## Visual styles and customization

Four persisted visual presets remain available and apply to Home accents, dock, customization, Settings, and library accents:

- **Aura Glass** — violet light with layered glossy surfaces
- **Soft Panel** — warm, softly elevated panels with calm contrast
- **Crystal Dock** — cool crystal edges and deeper dock treatment
- **Pure Minimal** — reduced chrome and tighter geometry

The customization tray still provides live style previews, Android's wallpaper picker, widget entry, layout and clock settings, and Done. Transitions animate Compose surfaces only; Rumb does not continuously blur or animate wallpaper bitmaps.

## Retained launcher features

- Callback-driven `LauncherApps` catalog for accessible personal and managed profiles
- Multiple persisted Home pages and configurable 4–8 column/row grids
- Span-aware placement for apps, folders, and Android AppWidgets without overlap
- Long-press app/folder movement, app-to-folder creation, editable folder names, and safe folder dissolution
- Five-app dock capacity, hidden apps, layout lock, and resilient loading/empty/error states
- Bounded 16 MB app-icon cache with density, user badge, mask, and optional Android 13 monochrome handling
- System, light, dark, AMOLED, and Material You color options
- Locale/time-zone-aware clocks with style, alignment, format, font, size, date, and seconds controls
- Configurable swipe-up, swipe-down, double-tap, and two-pointer pinch actions
- App shortcuts when Rumb is the active shortcut host/default launcher
- Versioned JSON export and validated import through Android's Storage Access Framework
- Legacy favorites/settings migration and strict normalization of imported or persisted layouts

## Hardened capability posture

Rumb requests **no permissions**. In particular, the manifest has no `INTERNET`, broad storage, notification-listener, package-removal, Bluetooth, location, or settings-write permission. There is no notification-listener service and no `ACTION_DELETE` uninstall path. The app contains no analytics, advertising, or account system.

Cleartext traffic and automatic Android backup are disabled. Explicit JSON imports are capped at 1 MB and validated before state replacement. Portable backups use profile roles instead of device-local serials and exclude widget IDs. The release build keeps code and resources unminified and unshrunk. Release signing is enabled only when `RUMB_KEYSTORE_PATH`, `RUMB_KEYSTORE_PASSWORD`, `RUMB_KEY_ALIAS`, and `RUMB_KEY_PASSWORD` are all configured; partial signing configuration fails the build without logging credentials.

These choices reduce sensitive capability surface and common sideloading concerns. Play Protect and OEM security products can still inspect or warn about apps installed outside their stores.

## Using the launcher

1. Install and open **Rumb Launcher Next**.
2. Select it when Android asks for a Home app, or use **Settings → System → Choose default home app**.
3. Swipe up on empty Home space to open the Apps/Widgets library.
4. Swipe down on fresh V3 state to open Quick Panel.
5. Long-press empty Home space to open customization.
6. Long-press an app in Apps to add it to Home or the dock, hide it, open App info, or use a shortcut.
7. Open Widgets and select a provider card, or choose System picker. Android owns binding approval and provider configuration.
8. Unlock the layout before moving, combining, adding, or removing Home items.

## Backup format

Settings exports a versioned JSON document through Android's document picker. Imports validate the format, supported schema, grid bounds, unique item IDs, placement, and app membership before replacing state. Backups use portable personal/profile roles. Widgets and device-local host IDs are excluded. Visual style, gestures including Quick Panel, hidden clock choice, appearance, and layout preferences are included.

## Android and OEM limitations

- Widget discovery is limited to providers Android exposes to this launcher and its accessible profiles. Work-profile policy or OEM restrictions can hide providers.
- Providers may omit preview art, report imperfect minimum dimensions, reject binding, provide no configuration activity, or ship a configuration activity that an OEM cannot launch. Rumb falls back to icons and cleans allocated IDs when setup cannot complete.
- Internet Settings Panels require Android 10 or newer. Older releases use the supported wireless settings screen. Any settings intent can be absent or replaced on customized OEM builds; Rumb checks resolution and shows a failure message.
- Widget bindings are Android-managed, device-local state and cannot be restored from portable JSON.
- Rumb's layout lock protects launcher editing only. It does not replace or weaken Android's secure lock screen.

## Architecture

- `domain/` — profile-aware IDs, app/home models, placement helpers, and versioned settings
- `data/` — synchronized StateFlow persistence, fresh/legacy migration, backup validation, and normalization
- `platform/apps/` — profile-aware app catalog, launching, and shortcuts
- `platform/icons/` — asynchronous app-icon cache
- `platform/widgets/` — installed-provider catalog and asynchronous preview cache
- `feature/home/` — wallpaper-first Home, gestures, pages, widgets, drag/drop, and compact dock
- `feature/drawer/` — AMOLED Apps/Widgets library, sort, search, and fast index
- `feature/quickpanel/` — callback-only safe Android settings launcher
- `feature/customize/`, `feature/settings/`, `feature/appactions/` — customization, grouped settings, and app/folder actions
- `MainActivity.kt` — Storage Access Framework results and lifecycle-owned `AppWidgetHost` transaction flow
- `LauncherViewModel.kt` — combined immutable UI state, destinations, and launcher actions

## Build

Requirements: Android SDK 35, JDK 17, and Android 8.0/API 26 or newer at runtime.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

The debug APK is written under `app/build/outputs/apk/debug/`. Without signing variables, Gradle intentionally creates an unsigned local release APK under `app/build/outputs/apk/release/`. Configure all four release variables above to produce a signed release.
