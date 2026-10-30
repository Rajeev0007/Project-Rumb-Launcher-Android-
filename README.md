# Rumb Launcher Next 3.4.0

Rumb Launcher Next is a private, offline Android home-screen replacement built with native Kotlin and Jetpack Compose. V3 uses the supplied screenshots only as structural reference for a new AMOLED-first library, wallpaper-first Home, and safe quick-control launcher. Its surfaces, geometry, color treatment, and artwork are original rather than copies of another launcher or vendor interface. Android 8.0 (API 26) and newer are supported.

## What's new in 3.4: Rumb Expressive

A new design language, `ui/design/Expressive.kt`, takes cues from current phone software:

- **One UI-style large titles.** Every Settings page opens with a tall title that fades into a compact top bar as you scroll.
- **Material 3 Expressive controls:**
  - Connected button groups whose selected segment turns fully round and whose pressed segment briefly widens.
  - A thick-track slider with a bar thumb.
  - Switches with a check mark.
  - Springy press feedback.
- **OriginOS-style quick tiles and filled icons.** The Settings home has a live Home preview card with real icons, plus four quick tiles: Lock layout, Dots, Search bar, and Labels. A tile fills and tightens its corners when turned on. Rows use vivid filled squircle icons.
- **Visual pickers.** Theme, clock style, time format, alignment, and font are inline button groups. Icon shape is a row of shape previews. Long lists such as gestures and icon packs open a sheet of large options.
- **Quick Panel:** large clock, wide pill tiles for Internet and Bluetooth, and filled-icon action tiles, all following the app theme.
- **Customise tray:** theme surface, filled-icon action tiles, and a full-width Done button.
- **App library:** a sliding pill switcher for Apps and Widgets.

## What's new in 3.3

- **Redesigned Settings.** A category home page (Appearance, Home screen, App library, Clock, Gestures, Notifications & access, Backup & restore, About) leads to focused sub-pages. Each row has a coloured icon, a title, and a summary of the current value.
- **Proper controls.** Options open a radio-list dialog instead of scrolling rows of buttons. Grid columns, rows, and pages use − / + steppers that respect their limits. Icon size and clock size use sliders that commit on release, so icons re-render once instead of on every frame.
- **Live previews.** Appearance shows four of your real apps with the current icon shape, size, pack, and labels. Clock shows the clock as it will look on Home.
- **Hidden apps manager.** Each hidden app can be shown again individually. Unhiding works even while the layout is locked.
- **Default-launcher check.** Settings shows a banner when Rumb isn't your Home app, and About shows the current status with a button to change it.
- **Consistent colours.** Settings, the app sheet, and the folder popup follow the Light, Dark, AMOLED, or Material You theme, with complete surface colours, so text and controls always have proper contrast. Status bar icons switch to dark on light Settings.
- **Cleaner app sheet.** Actions sit in a three-column grid with full labels (Add to Home, Add to dock, Rename, Hide, App info, Uninstall) instead of a cramped row of truncated buttons.

## What's new in 3.2

- **Drag apps from the library onto Home.** Long-press an app in Apps and start moving: the library slides away and the icon follows your finger. Drop it on an empty cell, on an app (makes a folder), on a folder (adds to it), or on the dock. Hold near the left or right edge to move between pages. Long-press and release still opens the app sheet.
- **Notification dots** (opt-in). Turn them on in Settings → Home layout and allow notification access. Apps, folders, and dock icons show a dot while they have a notification that Android allows to badge. Group summaries and ongoing notifications (music, downloads, foreground services) are skipped.
- **Uninstall** from the app sheet. Android shows its own confirmation. System apps don't offer it, and work-profile apps open App info instead. Uninstalled apps are now removed from Home, folders, the dock, hidden apps, and custom names automatically.
- **Lock screen gesture** (Android 9+, opt-in). Choose "Lock screen" for double tap, swipe, or pinch, then turn on "Rumb screen lock" in Accessibility settings.

On Android 13 and newer, Android may block notification access and accessibility for apps installed outside an app store and show "Restricted setting". To allow it, open App info for Rumb Launcher Next, tap ⋮, choose **Allow restricted settings**, then turn the access on again.

The backup schema is now version 7 and includes the notification-dots choice.

## What's new in 3.1

### Smoother, lag-free interaction

- **Finger-following library.** Swiping up on Home drags the Apps/Widgets library with your finger and settles it with a spring, using fling velocity. Pull the list down from the top, or drag the header, to close it the same way.
- **Home stays alive.** Home, the library, Settings, and Quick Panel are now layers instead of swapped screens, so widgets are no longer torn down and rebuilt every time you open the library.
- **No icon flashing.** Already-loaded icons render on their first frame from a synchronous cache lookup. Icon loading reuses the app catalog's activity snapshot instead of a LauncherApps IPC call per icon, and bitmaps are uploaded to the GPU ahead of drawing.
- **Off-main-thread state.** Search ranking, label overrides, hidden-app filtering, and suggestion scoring run on a background dispatcher. Settings are written to disk in the background, coalesced, and flushed when the launcher stops.
- **Gesture fixes.** Slow swipes longer than the long-press timeout are no longer dropped. Widgets receive size updates only when their size actually changes, and neighbouring Home pages are kept composed for smooth paging.
- **Apps open from their icon** using Android's scale-up launch animation and source bounds.

### New features

- **Item menu on Home and the dock.** Tap opens. Long-press and release opens the app sheet with shortcuts, Add/Remove Home, Dock, Rename, Hide, and App info. Long-press and move drags the item, with a live drop-target highlight that shows move, group-into-folder, or blocked.
- **Folder previews and editor.** Folders show a 2×2 preview of their apps. The folder popup shows an icon grid, inline rename, per-app remove, and folder removal.
- **Smarter search.** Results are ranked: exact, prefix, word-prefix, initials (for example "gm" for Google Maps), substring, then package name, with accents ignored. Enter launches the top result. "Search the web" and "Find in app store" hand the query to your own search, browser, or store app. Rumb still has no internet permission.
- **Suggested apps** at the top of the library, ranked by launch count with a three-day half-life plus a recent-launch boost. Counts are stored only on the device, never exported, and can be cleared in Settings.
- **Rename apps** from the app sheet. Custom names apply everywhere, including search, and are included in backups.
- **Icon packs.** ADW/Nova-compatible packs (`appfilter.xml`) are discovered through public package queries. Apps a pack doesn't cover keep their own icon.
- **Scrubbable A–Z index** with a letter bubble and haptic ticks. Letters without apps jump to the nearest available letter.
- **Home search bar** (default on for fresh installs) and a new **Search** gesture action.
- **Home button handling.** Pressing Home closes the library, panels, sheets, and edit mode. Pressing it again on Home returns to the first page.
- New settings: app labels on Home, search bar, suggestions, auto-open keyboard, icon pack, and haptic feedback.
- Widget remove buttons now appear only in edit mode, and tapping the Home preview finishes editing.

The backup schema is now version 6. Older backups still import, and the new fields fall back to values that keep an existing Home looking the same.

## Install identity and the package-conflict fix

V3 installs as **Rumb Launcher Next** with application ID `com.rumb.launcher.next`. The Kotlin namespace and source package remain `com.rumb.launcher`.

Android refuses to update an installed package when the incoming APK has the same application ID but a different signing certificate. That commonly appears as an app-not-installed or package-conflict error. Giving V3 a new application ID lets it install beside an older `com.rumb.launcher` build, including an old differently signed debug APK. Android treats the two IDs as separate apps, so select **Rumb Launcher Next** again in the system Home chooser. Private app data does not transfer automatically between package IDs; use Rumb's explicit JSON export/import where the older build supports it.

The Play Protect screenshot concerns the **old v2.0.1 debug APK**, with its old package identity and debug signing context. The current deliverable is **V3.4.0** (`versionCode 11`) under `com.rumb.launcher.next`. A locally built debug APK is still debug-signed; the hardened release is the unminified release variant signed only when all four release-signing variables are supplied. Package identity and release signing distinguish the current release from the old debug artifact, but no project can promise how Play Protect or an OEM scanner will classify a sideloaded APK.

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

## Capability posture

Rumb has **no internet permission**, analytics, advertising, or accounts. Optional capabilities stay off until you turn them on, and each one does one job:

| Capability | Why | What it can access |
| --- | --- | --- |
| `REQUEST_DELETE_PACKAGES` (normal permission) | Uninstall from the app sheet | Can only *ask*; Android shows its own confirmation every time |
| Notification listener (`RumbNotificationListener`) | Notification dots | Disabled in the manifest until you enable dots, and you must also grant notification access. Reads only the posting package, profile, notification flags, and Android's "can show badge" ranking. Titles, text, extras, and actions are never read or stored. |
| Accessibility service (`LockScreenService`) | "Lock screen" gesture on Android 9+ | Subscribes to no accessibility events and cannot retrieve window content. It only calls Android's public lock-screen global action. |

There is still no broad storage, Bluetooth, location, or settings-write permission. Cleartext traffic and automatic Android backup are disabled. Explicit JSON imports are capped at 1 MB and validated before state replacement. Portable backups use profile roles instead of device-local serials and exclude widget IDs. The release build keeps code and resources unminified and unshrunk. Release signing is enabled only when `RUMB_KEYSTORE_PATH`, `RUMB_KEYSTORE_PASSWORD`, `RUMB_KEY_ALIAS`, and `RUMB_KEY_PASSWORD` are all configured; partial signing configuration fails the build without logging credentials.

Sideloaded apps that declare a notification listener or an accessibility service can get extra scrutiny from Play Protect or OEM security tools. Both stay inactive until you grant access in system Settings, and no project can promise how a scanner will classify a sideloaded APK.

## Using the launcher

1. Install and open **Rumb Launcher Next**.
2. Select it when Android asks for a Home app, or use **Settings → System → Choose default home app**.
3. Swipe up on empty Home space to open the Apps/Widgets library.
4. Swipe down on fresh V3 state to open Quick Panel.
5. Long-press empty Home space to open customization.
6. Long-press an app in Apps and release to add it to Home or the dock, rename, hide, uninstall, open App info, or use a shortcut. Long-press and drag to place it on Home.
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
