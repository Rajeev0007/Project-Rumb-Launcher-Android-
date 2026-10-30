#!/usr/bin/env python3
"""Turn a Lawnchair checkout into Rumb Launcher.

Usage: apply_branding.py <lawnchair-checkout> <version-name>

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

# English strings that name the app in prompts and hints. Credits, the copyright
# header, provider names, and the sponsored-search disclosure keep saying Lawnchair.
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
]


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


def brand_gradle(root: pathlib.Path, version_name: str) -> None:
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
    replace_line(
        gradle,
        r'^(\s*)outputFileName = "Lawnchair\..*$',
        r'\1outputFileName = "Rumb-Launcher-${channel}-${variant.buildType.name}.apk"',
    )


def brand_strings(root: pathlib.Path) -> None:
    strings = root / "lawnchair" / "res" / "values" / "strings.xml"
    text = strings.read_text(encoding="utf-8")
    for name in RENAMED_STRINGS:
        pattern = re.compile(rf'(<string name="{name}"[^>]*>)(.*?)(</string>)', re.DOTALL)
        match = pattern.search(text)
        if match is None:
            fail(f"string {name} not found")
        body = match.group(2).replace("Lawnchair", APP_NAME)
        text = text[: match.start()] + match.group(1) + body + match.group(3) + text[match.end():]
    strings.write_text(text, encoding="utf-8")


def copy_overlay(root: pathlib.Path) -> None:
    overlay = pathlib.Path(__file__).resolve().parent / "overlay"
    for source in overlay.rglob("*"):
        if source.is_file():
            target = root / source.relative_to(overlay)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            print(f"overlay: {target.relative_to(root)}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: apply_branding.py <lawnchair-checkout> <version-name>")
    root = pathlib.Path(sys.argv[1]).resolve()
    if not (root / "lawnchair").is_dir():
        fail(f"{root} is not a Lawnchair checkout")
    brand_gradle(root, sys.argv[2])
    brand_strings(root)
    copy_overlay(root)
    print(f"Branded {root} as {APP_NAME} ({APPLICATION_ID}), version {sys.argv[2]}")


if __name__ == "__main__":
    main()
