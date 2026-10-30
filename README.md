# Rumb Launcher

Rumb Launcher is a lightweight Android home-screen replacement built with Kotlin and Jetpack Compose.

## Features

- Searchable installed-app drawer
- Persistent home-screen favorites
- Long-press actions for pinning, app info, and uninstall
- Configurable 4–6 column grid and icon size
- System, light, and dark themes
- System wallpaper picker and wallpaper-backed home screen
- Shortcut to Android's default-home-app settings
- No analytics, advertising, network permission, or account requirement

## Requirements

- Android 8.0 (API 26) or newer
- Android SDK 35
- JDK 17

## Build

```bash
./gradlew assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Install and select the launcher

Install the APK, open **Rumb Launcher**, then choose **Rumb Launcher** when Android asks for a home app. You can also open Rumb settings and tap **Choose default home app**.
