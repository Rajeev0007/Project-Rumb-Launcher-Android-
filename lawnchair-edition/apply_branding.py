#!/usr/bin/env python3
"""Turn a Lawnchair checkout into Rumb Launcher.

Usage: apply_branding.py <lawnchair-checkout> <version-name> <version-code>

Only identity changes are made: package name, app name, version label, output file
name, launcher icon, and the English strings that name the app. Lawnchair's code and
its credits stay as they are. Every edit is anchored, so the build fails loudly if
upstream changes shape instead of silently shipping a half-branded app.
"""
import pathlib
import re
import shutil
import sys

APP_NAME = "Rumb Launcher"
APPLICATION_ID = "com.rumb.launcher.next"

# Every user-visible string that says "Lawnchair" is renamed, in every language.
# Short provider names (a string whose whole text is "Lawnchair") become "Rumb".
# No search provider is sponsored at the pinned commit, so no disclosure is kept.
# The licence notice lives in Settings > About > Open-source licences (overlay).
KEEP_STRINGS: set[str] = set()

# Code patches: (file, anchor, replacement). Each anchor must appear exactly once.
CODE_PATCHES = [
    # A preference for the gradient dock.
    (
        "lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt",
        '    val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 100, recreate)\n',
        '    val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 100, recreate)\n'
        '    val rumbDockGradient = BoolPref("pref_rumbDockGradient", false, recreate)\n'
        '    val rumbListDrawer = BoolPref("pref_rumbListDrawer", false, recreate)\n',
    ),
    # List drawer: in the app drawer (not the taskbar), lay each app out as a row with a
    # small icon at the start and the name beside it. Styles sets one column and short rows.
    (
        "src/com/android/launcher3/BubbleTextView.java",
        "        mIconSize = a.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride,\n"
        "                defaultIconSize);\n",
        "        final boolean rumbListRow = mDisplay == DISPLAY_ALL_APPS && mActivity instanceof Launcher\n"
        "                && PreferenceManager.getInstance(context).getRumbListDrawer().get();\n"
        "        mIconSize = rumbListRow ? Math.round(defaultIconSize * 0.62f)\n"
        "                : a.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride,\n"
        "                defaultIconSize);\n",
    ),
    (
        "src/com/android/launcher3/BubbleTextView.java",
        "        setAccessibilityDelegate(mActivity.getAccessibilityDelegate());\n"
        "        setTextAlpha(1f);\n",
        "        setAccessibilityDelegate(mActivity.getAccessibilityDelegate());\n"
        "        setTextAlpha(1f);\n"
        "        if (rumbListRow) {\n"
        "            float density = getResources().getDisplayMetrics().density;\n"
        "            mLayoutHorizontal = true;\n"
        "            mCenterVertically = false;\n"
        "            setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);\n"
        "            setCompoundDrawablePadding(Math.round(16 * density));\n"
        "            setPaddingRelative(Math.round(24 * density), 0, Math.round(16 * density), 0);\n"
        "            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);\n"
        "        }\n",
    ),
    # Bug fix: themed icons switched themselves off every time the launcher restarted unless
    # a separate themed icon pack was installed. Android 13+ apps ship their own monochrome
    # icons, so only apply that old migration on Android 12 and below.
    (
        "lawnchair/src/app/lawnchair/LawnchairLauncher.kt",
        "            prefs.themedIcons.get() &&\n"
        "            packageManager.getThemedIconPacksInstalled(this).isEmpty()\n",
        "            android.os.Build.VERSION.SDK_INT < 33 &&\n"
        "            prefs.themedIcons.get() &&\n"
        "            packageManager.getThemedIconPacksInstalled(this).isEmpty()\n",
    ),
    # Draw the dock as a dock-colour to accent-colour gradient when enabled.
    (
        "src/com/android/launcher3/Hotseat.java",
        "        background.setColor(finalColor);\n",
        "        if (preferenceManager.getRumbDockGradient().get()) {\n"
        "            int accent = com.android.launcher3.util.Themes.getAttrColor(getContext(), android.R.attr.colorAccent);\n"
        "            int accentColor = Color.argb(alphaValue, Color.red(accent), Color.green(accent), Color.blue(accent));\n"
        "            background.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);\n"
        "            background.setColors(new int[] {finalColor, accentColor});\n"
        "        } else {\n"
        "            background.setColor(finalColor);\n"
        "        }\n",
    ),
    # Styles screen: route, navigation, and a dashboard entry right after General.
    (
        "lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceRoutes.kt",
        "@Serializable\ndata object HomeScreen : PreferenceRootRoute, PreferenceDeepLink {\n",
        "@Serializable\ndata object RumbStyles : PreferenceRootRoute, PreferenceDeepLink {\n"
        '    override val deepLink = "$URI/styles"\n}\n\n'
        "@Serializable\ndata object HomeScreen : PreferenceRootRoute, PreferenceDeepLink {\n",
    ),
    (
        "lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceNavigation.kt",
        "        composable<General>(\n",
        "        composable<RumbStyles>(\n"
        "            deepLinks = getDeepLink(RumbStyles),\n"
        "        ) { app.lawnchair.ui.preferences.destinations.RumbStylesPreferences() }\n"
        "        composable<General>(\n",
    ),
    (
        "lawnchair/src/app/lawnchair/ui/preferences/destinations/PreferencesDashboard.kt",
        "                onNavigate = { onNavigate(General) },\n"
        "                isSelected = currentRoute is General,\n"
        "            )\n",
        "                onNavigate = { onNavigate(General) },\n"
        "                isSelected = currentRoute is General,\n"
        "            )\n\n"
        "            PreferenceCategory(\n"
        '                label = "Styles",\n'
        '                description = "Glass, gradient, blur and one-tap looks",\n'
        "                iconResource = R.drawable.ic_palette,\n"
        "                onNavigate = { onNavigate(app.lawnchair.ui.preferences.navigation.RumbStyles) },\n"
        "                isSelected = currentRoute is app.lawnchair.ui.preferences.navigation.RumbStyles,\n"
        "            )\n",
    ),
    # Hidden settings screen: route, navigation, and a dashboard entry before Backup.
    (
        "lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceRoutes.kt",
        "@Serializable\ndata object HomeScreen : PreferenceRootRoute, PreferenceDeepLink {\n",
        "@Serializable\ndata object RumbHiddenSettings : PreferenceRootRoute, PreferenceDeepLink {\n"
        '    override val deepLink = "$URI/hidden_settings"\n}\n\n'
        "@Serializable\ndata object HomeScreen : PreferenceRootRoute, PreferenceDeepLink {\n",
    ),
    (
        "lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceNavigation.kt",
        "        composable<General>(\n",
        "        composable<RumbHiddenSettings>(\n"
        "            deepLinks = getDeepLink(RumbHiddenSettings),\n"
        "        ) { app.lawnchair.ui.preferences.destinations.RumbHiddenSettingsPreferences() }\n"
        "        composable<General>(\n",
    ),
    (
        "lawnchair/src/app/lawnchair/ui/preferences/destinations/PreferencesDashboard.kt",
        "            PreferenceCategory(\n"
        "                label = stringResource(R.string.backup_and_restore_label),\n",
        "            PreferenceCategory(\n"
        '                label = "Hidden settings",\n'
        '                description = "Android screens with no menu entry: developer, notification log, permissions",\n'
        "                iconResource = R.drawable.rumb_ic_hidden_settings,\n"
        "                onNavigate = { onNavigate(app.lawnchair.ui.preferences.navigation.RumbHiddenSettings) },\n"
        "                isSelected = currentRoute is app.lawnchair.ui.preferences.navigation.RumbHiddenSettings,\n"
        "            )\n\n"
        "            PreferenceCategory(\n"
        "                label = stringResource(R.string.backup_and_restore_label),\n",
    ),
    # Bug fix: the drawer defaulted to 50% opacity, so Home screen icons showed through
    # it whenever wallpaper blur was off. Rumb's drawer is opaque unless you choose otherwise.
    (
        "lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt",
        'val drawerOpacity = FloatPref("pref_drawerOpacity", .5f, recreate)',
        'val drawerOpacity = FloatPref("pref_drawerOpacity", 1f, recreate)',
    ),
    # Bug fix: Home screen icons stayed fully drawn behind the app drawer. Fade the
    # workspace out as the drawer opens (progress 1 = closed, 0 = open), so a translucent
    # or blurred drawer shows only the wallpaper, like a real glass panel. The dock is left
    # to the launcher's own state animation, which already hides it on phones; forcing its
    # alpha here fought that animation (dock flicker going from the drawer to recents) and
    # hid the dock on tablets, where it is meant to stay visible.
    (
        "src/com/android/launcher3/allapps/AllAppsTransitionController.java",
        "        getAppsViewProgressTranslationY().setValue(mProgress * shiftRange);\n"
        "        mLauncher.onAllAppsTransition(1 - progress);\n",
        "        getAppsViewProgressTranslationY().setValue(mProgress * shiftRange);\n"
        "        mLauncher.onAllAppsTransition(1 - progress);\n"
        "        float rumbHomeAlpha = Math.max(0f, Math.min(1f, (progress - 0.15f) / 0.35f));\n"
        "        mLauncher.getWorkspace().setAlpha(rumbHomeAlpha);\n",
    ),
    # Bug fix: the Personal/Work tab used the system accent (green on many phones)
    # instead of Rumb's accent, clashing with the rest of the theme.
    (
        "lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt",
        '        key = stringPreferencesKey(name = "work_profile_tab_background_color"),\n'
        "        parse = ColorOption::fromString,\n"
        "        save = ColorOption::toString,\n"
        "        onSet = { reloadHelper.recreate() },\n"
        "        defaultValue = ColorOption.SystemAccent,\n",
        '        key = stringPreferencesKey(name = "work_profile_tab_background_color"),\n'
        "        parse = ColorOption::fromString,\n"
        "        save = ColorOption::toString,\n"
        "        onSet = { reloadHelper.recreate() },\n"
        "        defaultValue = ColorOption.fromString(context.getString(R.string.config_default_accent_color)),\n",
    ),
    # Bug fix: the drawer search bar showed no text until tapped. Show "Search apps".
    (
        "lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt",
        "        input = ViewCompat.requireViewById(this, R.id.input)\n",
        "        input = ViewCompat.requireViewById(this, R.id.input)\n"
        "        input.setHint(R.string.all_apps_search_bar_hint)\n",
    ),
    (
        "lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt",
        '                input.setHint("")\n',
        "                input.setHint(R.string.all_apps_search_bar_hint)\n",
    ),
    (
        "lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt",
        'if (input.text.toString() == "/lawnchairdebug") {',
        'if (input.text.toString() == "/rumbdebug") {',
    ),
    # Searches from Rumb should not be attributed to Lawnchair's partner segment.
    (
        "lawnchair/src/app/lawnchair/qsb/providers/Startpage.kt",
        'website = "https://startpage.com/?segment=startpage.lawnchair",',
        'website = "https://startpage.com/",',
    ),
    (
        "lawnchair/src/app/lawnchair/qsb/providers/StartpageEU.kt",
        'website = "https://eu.startpage.com/?segment=startpage.lawnchair",',
        'website = "https://eu.startpage.com/",',
    ),
]

# Rumb's out-of-the-box look and behaviour, set through Lawnchair's config defaults.
RUMB_DEFAULTS = {
    # Look
    "config_default_icon_shape": "squircle",
    "config_default_accent_color": "custom|#ff5b48c9",
    "config_default_live_information_enabled": "false",
    "config_default_live_information_show_announcements": "false",
    "config_default_enable_fuzzy_search": "true",
    # Customisation within reach: long-press an app to change its icon, and the
    # Home long-press menu gets edit, lock, and system-settings shortcuts.
    "config_default_enable_icon_selection": "true",
    "config_default_edit_home_screen_on_popup": "true",
    "config_default_lock_home_screen_on_popup": "true",
    "config_default_show_system_settings_entry_on_popup": "true",
    # Smoothness: by default the launcher reloaded every icon each time it was created,
    # which stalls the Home screen after theme, accent or rotation changes. Icons are
    # still refreshed when the icon pack, shape or theme changes. Can be turned back on
    # in Settings > General > Icons > Advanced.
    "config_default_always_reload_icons": "false",
}


def fail(message: str) -> None:
    sys.exit(f"Rumb branding failed: {message}")


def replace_once(path: pathlib.Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if text.count(old) != 1:
        fail(f"expected exactly one {old!r} in {path}, found {text.count(old)}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def replace_line(path: pathlib.Path, pattern: str, new_line: str) -> None:
    text = path.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, new_line, text, count=1, flags=re.MULTILINE)
    if count != 1:
        fail(f"pattern {pattern!r} not found in {path}")
    path.write_text(updated, encoding="utf-8")


def brand_gradle(root: pathlib.Path, version_name: str, version_code: str) -> None:
    gradle = root / "build.gradle"
    replace_once(gradle, "applicationId 'app.lawnchair'\n", f"applicationId '{APPLICATION_ID}'\n")
    replace_once(
        gradle,
        'resValue("string", "derived_app_name", "Lawnchair")',
        f'resValue("string", "derived_app_name", "{APP_NAME}")',
    )
    replace_once(
        gradle,
        'resValue("string", "derived_app_name", "Lawnchair (Debug)")',
        f'resValue("string", "derived_app_name", "{APP_NAME} (Debug)")',
    )
    replace_line(gradle, r"^final def versionDisplayName = .*$", f'final def versionDisplayName = "{version_name}"')
    replace_line(gradle, r"^final def majorVersion = .*$", 'final def majorVersion = "16"')
    if not version_code.isdigit() or int(version_code) > 2_100_000_000:
        fail(f"invalid version code {version_code}")
    replace_line(gradle, r"^(\s*)versionCode [0-9_]+\s*$", rf"\g<1>versionCode {version_code}")
    replace_line(
        gradle,
        r'^(\s*)output\.outputFileName\.set\("Lawnchair\..*$',
        r'\1output.outputFileName.set("Rumb-Launcher-${channel}-${variant.buildType}.apk")',
    )


STRING_ELEMENT = re.compile(r'(<string\b([^>]*)>)(.*?)(</string>)', re.DOTALL)
NAME_ATTR = re.compile(r'name="([^"]+)"')


def rebrand_text(text: str) -> tuple[str, int]:
    changes = 0

    def replace(match: re.Match) -> str:
        nonlocal changes
        name = NAME_ATTR.search(match.group(2) or "")
        body = match.group(3)
        if "Lawnchair" not in body or body.lstrip().startswith("@") or (name and name.group(1) in KEEP_STRINGS):
            return match.group(0)
        changes += 1
        new_body = "Rumb" if body.strip() == "Lawnchair" else body.replace("Lawnchair", APP_NAME)
        return match.group(1) + new_body + match.group(4)

    return STRING_ELEMENT.sub(replace, text), changes


def brand_strings(root: pathlib.Path) -> None:
    files = 0
    strings = 0
    for res in (root / "lawnchair" / "res", root / "res", root / "quickstep" / "res"):
        if not res.is_dir():
            continue
        for path in sorted(res.glob("values*/strings.xml")):
            text = path.read_text(encoding="utf-8")
            if "Lawnchair" not in text:
                continue
            updated, count = rebrand_text(text)
            if count:
                path.write_text(updated, encoding="utf-8")
                files += 1
                strings += count
    english = (root / "lawnchair" / "res" / "values" / "strings.xml").read_text(encoding="utf-8")
    leftover = [m.group(1) for m in re.finditer(r'<string name="([^"]+)"[^>]*>[^<]*Lawnchair', english)
                if m.group(1) not in KEEP_STRINGS]
    if leftover:
        fail(f"English strings still name Lawnchair: {leftover}")
    print(f"Rebranded {strings} strings in {files} resource files")


def apply_defaults(root: pathlib.Path) -> None:
    config = root / "lawnchair" / "res" / "values" / "config.xml"
    text = config.read_text(encoding="utf-8")
    for name, value in RUMB_DEFAULTS.items():
        pattern = re.compile(rf'(<(string|bool) name="{name}"[^>]*>)[^<]*(</\2>)')
        text, count = pattern.subn(rf"\g<1>{value}\g<3>", text)
        if count != 1:
            fail(f"default {name} not found in {config}")
    config.write_text(text, encoding="utf-8")


def apply_code_patches(root: pathlib.Path) -> None:
    for relative, anchor, replacement in CODE_PATCHES:
        replace_once(root / relative, anchor, replacement)
    print(f"Applied {len(CODE_PATCHES)} code patches")


def copy_overlay(root: pathlib.Path) -> None:
    overlay = pathlib.Path(__file__).resolve().parent / "overlay"
    for source in overlay.rglob("*"):
        if source.is_file():
            target = root / source.relative_to(overlay)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            print(f"overlay: {target.relative_to(root)}")


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: apply_branding.py <lawnchair-checkout> <version-name> <version-code>")
    root = pathlib.Path(sys.argv[1]).resolve()
    if not (root / "lawnchair").is_dir():
        fail(f"{root} is not a Lawnchair checkout")
    brand_gradle(root, sys.argv[2], sys.argv[3])
    brand_strings(root)
    apply_defaults(root)
    apply_code_patches(root)
    copy_overlay(root)
    print(f"Branded {root} as {APP_NAME} ({APPLICATION_ID}), version {sys.argv[2]} ({sys.argv[3]})")


if __name__ == "__main__":
    main()
