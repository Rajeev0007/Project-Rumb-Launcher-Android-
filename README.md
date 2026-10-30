# Rumb Launcher Next

A fast, private Android home screen built with Kotlin and Jetpack Compose. Android 8.0+.

**Download:** [latest release](https://github.com/Rajeev0007/Project-Rumb-Launcher-Android-/releases/latest)

## Features
- Swipe-up app library that follows your finger, with ranked search, suggestions, and an A–Z scrubber
- Home pages, folders with previews, widgets, dock, and drag apps straight from the library
- Long-press menus: shortcuts, rename, hide, uninstall, app info
- Icon packs, icon shapes, themed icons, Material You, light / dark / AMOLED
- Notification dots and a lock-screen gesture (both opt-in)
- Gestures, Quick Panel, layout lock, and JSON backup / restore

## Privacy
No internet permission, analytics, ads, or accounts. Notification dots read only which app posted, never the content. The screen-lock service can only lock the screen.

## Install
1. Download the APK from Releases and open it.
2. Choose **Rumb Launcher Next** when Android asks for a Home app.

If Android says *App not installed*, uninstall the old version first. That only happens when releases were signed with different keys (see below).

## Build
```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

## Release signing
CI signs releases with these repository secrets so each version updates the last one:
`RUMB_KEYSTORE_BASE64`, `RUMB_KEYSTORE_PASSWORD`, `RUMB_KEY_ALIAS`, `RUMB_KEY_PASSWORD`.
If they are missing, CI uses a one-off key and warns that the APK can't update older installs.
