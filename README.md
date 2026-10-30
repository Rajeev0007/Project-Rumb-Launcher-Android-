# Rumb Launcher

A Pixel-style Android home screen by **Rajeev0007**, built on [Lawnchair](https://github.com/LawnchairLauncher/lawnchair). Android 8.0+.

**Download:** [latest release](https://github.com/Rajeev0007/Project-Rumb-Launcher-Android-/releases/latest)

## Rumb style
- Rumb name, icon, and About page (developer, GitHub, updates, feedback)
- Rumb violet accent, squircle icons, rounder cards and dialogs, fuzzy app search
- No upstream announcements or contributor feeds; nothing fetched for the About page

## Features
- Pixel launcher look and smoothness, based on Google's Launcher3
- At a Glance, app drawer search, folders, widgets, dock, and themed icons
- Icon packs, icon shapes, fonts, grid and dock size, and colours
- Gestures such as double-tap to sleep and swipes, plus notification dots
- Backup and restore, including Nova Launcher backups

## Install
1. Download the APK from Releases and open it.
2. Choose **Rumb Launcher** when Android asks for a Home app.

Coming from an older Rumb build? Uninstall it once first. Older builds were signed with different keys, so Android can't update them in place.

## How it's built
`.github/workflows/build-rumb-lawnchair.yml` checks out Lawnchair at a pinned commit and runs `lawnchair-edition/apply_branding.py`. The script changes the package name, app name, icon, About page, default style, and the strings that name the app, then CI builds and signs the APK. To move to a newer Lawnchair, change `LAWNCHAIR_REF` in the workflow.

Releases are signed with the repository secrets `RUMB_KEYSTORE_BASE64`, `RUMB_KEYSTORE_PASSWORD`, `RUMB_KEY_ALIAS`, and `RUMB_KEY_PASSWORD`, so each version updates the last.

The earlier Compose version of Rumb remains in `app/` for reference.

## Licence and credit
Rumb Launcher is a rebranded build of Lawnchair. Lawnchair is licensed under GPL-3.0 and builds on the Android Open Source Project (Apache-2.0). All credit for the launcher goes to the [Lawnchair team](https://github.com/LawnchairLauncher/lawnchair). Each release links the exact Lawnchair source and branding it was built from. Lawnchair's own permissions and network features (such as At a Glance weather and icon downloads) apply.
