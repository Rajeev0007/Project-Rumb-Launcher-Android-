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
        '    val rumbDockGradient = BoolPref("pref_rumbDockGradient", false, recreate)\n',
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
