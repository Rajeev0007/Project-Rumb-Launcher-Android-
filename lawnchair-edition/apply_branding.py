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

# Strings that name the app in prompts and hints, in every language. The copyright
# header and the sponsored-search revenue disclosure keep saying Lawnchair because
# they describe Lawnchair itself; licence credits live on the About screen.
RENAMED_STRINGS = [
    "managed_by_lawnchair",
    "accessibility_service_description",
    "debug_restart_launcher",
    "set_default_launcher_tip",
    "smartspace_widget_toggle_description",
    "restore_nova_subgrid_warning",
    "dt2s_admin_hint",
    "dt2s_a11y_hint",
    "recents_a11y_hint",
    "quickswitch_ignored_warning",
    "lawnchair_bug_report",
]

# Provider names shown in pickers (English only, not translatable upstream).
PROVIDER_NAMES = {
    "smartspace_mode_lawnchair": "Rumb",
    "hotseat_mode_lawnchair": "Rumb",
}

# Rumb's out-of-the-box look and behaviour, set through Lawnchair's config defaults.
RUMB_DEFAULTS = {
    "config_default_icon_shape": "squircle",
    "config_default_accent_color": "custom|#ff5b48c9",
    "config_default_live_information_enabled": "false",
    "config_default_live_information_show_announcements": "false",
    "config_default_enable_fuzzy_search": "true",
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
    replace_line(gradle, r"^final def majorVersion = .*$", 'final def majorVersion = "15"')
    if not version_code.isdigit() or int(version_code) > 2_100_000_000:
        fail(f"invalid version code {version_code}")
    replace_line(gradle, r"^(\s*)versionCode [0-9_]+\s*$", rf"\g<1>versionCode {version_code}")
    replace_line(
        gradle,
        r'^(\s*)outputFileName = "Lawnchair\..*$',
        r'\1outputFileName = "Rumb-Launcher-${channel}-${variant.buildType.name}.apk"',
    )


def rename_in(text: str, name: str, replacement: str, required: bool) -> str:
    pattern = re.compile(rf'(<string name="{name}"[^>]*>)(.*?)(</string>)', re.DOTALL)
    match = pattern.search(text)
    if match is None:
        if required:
            fail(f"string {name} not found")
        return text
    body = match.group(2).replace("Lawnchair", replacement)
    return text[: match.start()] + match.group(1) + body + match.group(3) + text[match.end():]


def brand_strings(root: pathlib.Path) -> None:
    res = root / "lawnchair" / "res"
    english = res / "values" / "strings.xml"
    translated = 0
    for strings in sorted(res.glob("values*/strings.xml")):
        text = strings.read_text(encoding="utf-8")
        original = text
        is_english = strings == english
        for name in RENAMED_STRINGS:
            text = rename_in(text, name, APP_NAME, required=is_english)
        if is_english:
            for name, label in PROVIDER_NAMES.items():
                pattern = re.compile(rf'(<string name="{name}"[^>]*>)Lawnchair(</string>)')
                text, count = pattern.subn(rf"\g<1>{label}\g<2>", text)
                if count != 1:
                    fail(f"provider string {name} not found")
        if text != original:
            strings.write_text(text, encoding="utf-8")
            translated += 0 if is_english else 1
    print(f"Renamed app strings in English and {translated} translations")


def apply_defaults(root: pathlib.Path) -> None:
    config = root / "lawnchair" / "res" / "values" / "config.xml"
    text = config.read_text(encoding="utf-8")
    for name, value in RUMB_DEFAULTS.items():
        pattern = re.compile(rf'(<(string|bool) name="{name}"[^>]*>)[^<]*(</\2>)')
        text, count = pattern.subn(rf"\g<1>{value}\g<3>", text)
        if count != 1:
            fail(f"default {name} not found in {config}")
    config.write_text(text, encoding="utf-8")


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
    copy_overlay(root)
    print(f"Branded {root} as {APP_NAME} ({APPLICATION_ID}), version {sys.argv[2]} ({sys.argv[3]})")


if __name__ == "__main__":
    main()
