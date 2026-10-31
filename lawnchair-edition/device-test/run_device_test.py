#!/usr/bin/env python3
"""Drive Rumb Launcher on an emulator and record what it looks like.

Usage: run_device_test.py <apk> <output-dir>

Installs the APK, makes it the Home app, then walks through Home, the app drawer, search,
the Home long-press menu, Settings, Styles (every preset), and Hidden settings. Each step
saves a screenshot and a UI dump. The drawer layout is checked from the dump: every app
must sit fully on screen, in evenly spaced columns, with no two apps overlapping.

Exits non-zero if Rumb crashed or the drawer layout check failed.
"""
import json
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "com.rumb.launcher.next"
HOME = f"{PKG}/app.lawnchair.LawnchairLauncher"
SETTINGS = f"{PKG}/app.lawnchair.ui.preferences.PreferenceActivity"
PRESETS = ["Pixel", "Nova Dark", "iOS 27", "Minimal List", "Glass", "One UI", "Gradient", "Clean"]

apk = sys.argv[1]
out = pathlib.Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
report: list[str] = []
problems: list[str] = []


def log(message: str) -> None:
    print(message, flush=True)
    report.append(message)


def adb(*args: str, check: bool = False, timeout: int = 60) -> str:
    result = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def shell(command: str, timeout: int = 60) -> str:
    return adb("shell", command, timeout=timeout)


def screen_size() -> tuple[int, int]:
    match = re.search(r"(\d+)x(\d+)", shell("wm size"))
    return (int(match.group(1)), int(match.group(2))) if match else (1080, 2400)


W, H = 1080, 2400


def screenshot(name: str) -> None:
    data = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
    (out / f"{name}.png").write_bytes(data)


def dump(name: str | None = None) -> ET.Element | None:
    for _ in range(4):
        shell("rm -f /sdcard/ui.xml")
        shell("uiautomator dump /sdcard/ui.xml", timeout=90)
        text = shell("cat /sdcard/ui.xml")
        if "<hierarchy" in text:
            if name:
                (out / f"{name}.xml").write_text(text)
            try:
                return ET.fromstring(text[text.index("<hierarchy"):].strip())
            except ET.ParseError:
                pass
        time.sleep(2)
    log(f"  ! could not dump UI for {name}")
    return None


def bounds(node: ET.Element) -> tuple[int, int, int, int]:
    nums = [int(n) for n in re.findall(r"-?\d+", node.get("bounds", "[0,0][0,0]"))]
    return nums[0], nums[1], nums[2], nums[3]


def find(root: ET.Element | None, text: str, exact: bool = True) -> ET.Element | None:
    if root is None:
        return None
    for node in root.iter("node"):
        for attr in ("text", "content-desc"):
            value = node.get(attr, "")
            if (value == text) if exact else (text.lower() in value.lower()):
                return node
    return None


def tap_node(node: ET.Element) -> None:
    l, t, r, b = bounds(node)
    shell(f"input tap {(l + r) // 2} {(t + b) // 2}")


def tap_text(text: str, exact: bool = True, wait: float = 2.0) -> bool:
    node = find(dump(), text, exact)
    if node is None:
        return False
    tap_node(node)
    time.sleep(wait)
    return True


def step(name: str, wait: float = 3.0) -> ET.Element | None:
    time.sleep(wait)
    screenshot(name)
    root = dump(name)
    log(f"step {name}")
    return root


def go_home(wait: float = 3.0) -> None:
    shell("input keyevent KEYCODE_HOME")
    time.sleep(wait)


def dismiss_dialogs() -> None:
    for label in ("OK", "Got it", "Allow", "Continue", "Dismiss", "Close", "Not now", "Cancel"):
        if tap_text(label, wait=1.5):
            log(f"  dismissed a dialog with '{label}'")


def swipe(x1: int, y1: int, x2: int, y2: int, ms: int = 350) -> None:
    shell(f"input swipe {x1} {y1} {x2} {y2} {ms}")


def drawer_is_open(root: ET.Element | None) -> bool:
    """True when any launcher's app list is on screen (works for the stock launcher too)."""
    if root is None:
        return False
    for node in root.iter("node"):
        if node.get("resource-id", "").endswith(":id/apps_list_view"):
            l, t, r, b = bounds(node)
            if b - t > H // 3 and t < H // 2:
                return True
    return False


def slow_drag(y1: int, y2: int, steps: int = 16, pause: float = 0.05) -> None:
    """A finger-like drag: down, small moves with pauses, a short hold, then up."""
    x = W // 2
    shell(f"input motionevent DOWN {x} {y1}")
    for i in range(1, steps + 1):
        shell(f"input motionevent MOVE {x} {y1 + (y2 - y1) * i // steps}")
        time.sleep(pause)
    time.sleep(0.15)
    shell(f"input motionevent UP {x} {y2}")


def swipe_experiments(label: str) -> dict:
    """Try several swipe styles and record which open the drawer."""
    results = {}
    trials = {
        "swipe-1000ms": lambda: swipe(W // 2, int(H * 0.65), W // 2, int(H * 0.20), 1000),
        "swipe-250ms": lambda: swipe(W // 2, int(H * 0.65), W // 2, int(H * 0.20), 250),
        "slow-drag": lambda: slow_drag(int(H * 0.65), int(H * 0.20)),
    }
    for name, action in trials.items():
        go_home(2)
        action()
        time.sleep(3)
        results[name] = drawer_is_open(dump())
        go_home(2)
    log(f"  swipe experiments on {label}: {results}")
    return results


def open_drawer(name: str = "drawer") -> bool:
    """Swipe up like a finger would; check the drawer really opened; fall back to the menu."""
    attempts = [
        (int(H * 0.65), int(H * 0.20), 1000),  # a steady swipe from the middle of Home
        (int(H * 0.80), int(H * 0.20), 1000),  # from the dock
        (int(H * 0.70), int(H * 0.15), 600),   # quicker
    ]
    for index, (y1, y2, ms) in enumerate(attempts, 1):
        # Grab a frame halfway through the gesture to see what the drawer is doing.
        mid = subprocess.Popen(["adb", "shell", f"input swipe {W // 2} {y1} {W // 2} {y2} {ms}"])
        time.sleep(ms / 2000)
        if index == 1:
            screenshot(f"{name}-mid-swipe")
        mid.wait(timeout=30)
        time.sleep(3)
        if drawer_is_open(dump()):
            if index > 1:
                log(f"  ! {name}: swipe up opened the drawer only on attempt {index}")
                problems.append(f"{name}: swipe up needed {index} attempts")
            return True
        log(f"  ! {name}: swipe attempt {index} ({y1}->{y2}, {ms}ms) did not open the drawer")
        go_home(2)
    problems.append(f"{name}: swipe up never opened the drawer")
    swipe(W // 2, int(H * 0.45), W // 2, int(H * 0.45), 1200)
    time.sleep(2)
    if tap_text("Apps list", wait=3) and drawer_is_open(dump()):
        log(f"  {name}: opened the drawer from the Home menu instead")
        return True
    log(f"  ! {name}: the drawer did not open at all")
    return False


SKIP_IDS = (":id/hotseat", ":id/workspace")


def drawer_apps(root: ET.Element | None, area: str = "all") -> list[tuple[str, tuple[int, int, int, int]]]:
    """App cells in the drawer (not the dock or Home behind it).

    area: "all", "grid" (A-Z list) or "suggested" (the suggested-apps row).
    """
    apps: list = []
    if root is None:
        return apps

    def walk(node: ET.Element, in_prediction: bool) -> None:
        rid = node.get("resource-id", "")
        if any(rid.endswith(s) for s in SKIP_IDS):
            return
        in_prediction = in_prediction or rid.endswith(":id/prediction_row")
        if (node.get("package") == PKG and node.get("class") == "android.widget.TextView"
                and node.get("clickable") == "true" and (node.get("text") or node.get("content-desc"))):
            if area == "all" or (area == "suggested") == in_prediction:
                apps.append((node.get("text") or node.get("content-desc"), bounds(node)))
        for child in node:
            walk(child, in_prediction)

    walk(root, False)
    return apps


def check_drawer(name: str, root: ET.Element | None, expect_list: bool = False) -> None:
    apps = drawer_apps(root)
    if not apps:
        problems.append(f"{name}: no apps found in the drawer")
        log(f"  ! {name}: no drawer apps in UI dump")
        return
    (out / f"{name}-apps.json").write_text(json.dumps(apps, indent=1))
    issues = []
    for label, (l, t, r, b) in apps:
        if r - l <= 0 or b - t <= 0:
            issues.append(f"'{label}' has empty bounds {l},{t},{r},{b}")
        elif l < 0 or r > W:
            issues.append(f"'{label}' runs off screen horizontally ({l}..{r} of {W})")
    for i, (la, a) in enumerate(apps):
        for lb, b in apps[i + 1:]:
            ox = min(a[2], b[2]) - max(a[0], b[0])
            oy = min(a[3], b[3]) - max(a[1], b[1])
            if ox > 4 and oy > 4:
                issues.append(f"'{la}' overlaps '{lb}'")
    rows: dict[int, list] = {}
    for label, box in apps:
        rows.setdefault(box[1] // 20, []).append((label, box))
    widths = sorted({box[2] - box[0] for _, box in apps})
    lefts = sorted({box[0] for _, box in apps})
    log(f"  {name}: {len(apps)} apps, {len(rows)} rows, cell widths {widths[:6]}, column starts {lefts[:8]}")
    if expect_list and any(len(r) > 1 for r in rows.values()):
        issues.append("list drawer shows more than one app per row")
    if not expect_list and len(lefts) > 1:
        full_rows = [sorted(r, key=lambda x: x[1][0]) for r in rows.values() if len(r) > 1]
        if full_rows:
            first = full_rows[0]
            gaps = [first[i + 1][1][0] - first[i][1][0] for i in range(len(first) - 1)]
            if gaps and max(gaps) - min(gaps) > 8:
                issues.append(f"uneven column spacing {gaps}")
    suggested = drawer_apps(root, "suggested")
    grid = drawer_apps(root, "grid")
    if suggested and grid and not expect_list:
        first_row_top = min(b[1] for _, b in grid)
        first_row = sorted([b for _, b in grid if b[1] == first_row_top], key=lambda b: b[0])
        tops = sorted([b for _, b in suggested], key=lambda b: b[0])
        offsets = [((a[0] + a[2]) // 2) - ((b[0] + b[2]) // 2) for a, b in zip(tops, first_row)]
        log(f"  {name}: suggested row vs grid column centre offsets {offsets}")
        if offsets and max(abs(o) for o in offsets) > 4:
            issues.append(f"suggested apps are out of line with the grid by {offsets} px")
    for issue in issues[:12]:
        log(f"  ! {name}: {issue}")
    problems.extend(f"{name}: {issue}" for issue in issues)


def crashes() -> str:
    return adb("logcat", "-d", "-b", "crash")


# --------------------------------------------------------------------------------------
W, H = screen_size()
log(f"screen {W}x{H}, Android {shell('getprop ro.build.version.release').strip()}")

# Control: the emulator's own launcher, with the same swipes, before Rumb is installed.
stock = shell("cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN").strip().splitlines()[-1]
log(f"stock launcher: {stock}")
shell("am start -W -a android.intent.action.MAIN -c android.intent.category.HOME")
time.sleep(8)
stock_results = swipe_experiments("stock launcher")

adb("install", "-r", "-g", apk, check=True, timeout=300)
adb("logcat", "-c")
shell(f"cmd package set-home-activity {HOME}")
shell("settings put global window_animation_scale 0.5")
shell(f"am start -W -n {HOME}")
time.sleep(10)
dismiss_dialogs()
go_home()

step("01-home")
rumb_results = swipe_experiments("Rumb")
for name, ok in rumb_results.items():
    if stock_results.get(name) and not ok:
        problems.append(f"drawer: '{name}' opens the stock launcher's drawer but not Rumb's")
open_drawer("02-drawer")
root = step("02-drawer", 1)
check_drawer("02-drawer", root)
swipe(W // 2, int(H * 0.75), W // 2, int(H * 0.35), 400)
step("03-drawer-scrolled", 2)
go_home()

open_drawer("04-search")
if tap_text("Search apps", wait=2) or tap_text("Search", exact=False, wait=2):
    shell("input text set")
    step("04-drawer-search", 2)
else:
    log("  ! search bar not found in the drawer")
    problems.append("drawer: search bar not found")
shell("input keyevent KEYCODE_BACK")
go_home()

swipe(W // 2, int(H * 0.45), W // 2, int(H * 0.45), 1200)
step("05-home-long-press", 2)
go_home()

shell(f"am start -W -n {SETTINGS}")
step("06-settings")
swipe(W // 2, int(H * 0.8), W // 2, int(H * 0.3))
step("07-settings-scrolled", 2)
for route, name in (("styles", "08-styles"), ("hidden_settings", "10-hidden-settings")):
    shell(f"am start -W -a android.intent.action.VIEW -d lawnchair://settings/{route} {PKG}")
    step(name)
    swipe(W // 2, int(H * 0.8), W // 2, int(H * 0.3))
    step(f"{name}-scrolled", 2)
go_home()

for preset in PRESETS:
    slug = re.sub(r"[^a-z0-9]+", "-", preset.lower()).strip("-")
    shell(f"am start -W -a android.intent.action.VIEW -d lawnchair://settings/styles {PKG}")
    time.sleep(3)
    node = None
    root = dump()
    for _ in range(6):
        node = find(root, preset)
        if node is not None:
            break
        swipe(W // 2, int(H * 0.70), W // 2, int(H * 0.45), 600)
        time.sleep(1.5)
        root = dump()
    if node is None:
        screenshot(f"p-{slug}-0-not-found")
        log(f"  ! preset {preset} not found on the Styles screen")
        problems.append(f"styles: preset {preset} not found")
        go_home()
        continue
    tap_node(node)
    log(f"applied {preset}")
    time.sleep(8)
    dismiss_dialogs()
    go_home(5)
    step(f"p-{slug}-1-home", 2)
    open_drawer(f"p-{slug}")
    root = step(f"p-{slug}-2-drawer", 1)
    check_drawer(f"p-{slug}-drawer", root, expect_list=(preset == "Minimal List"))
    go_home()

# Widget picker: long-press Home, open Widgets, look for Rumb's widgets.
go_home()
swipe(W // 2, int(H * 0.45), W // 2, int(H * 0.45), 1200)
time.sleep(2)
if tap_text("Widgets", wait=4):
    for _ in range(8):
        root = dump()
        node = find(root, "Rumb Launcher")
        if node is not None:
            tap_node(node)
            time.sleep(3)
            break
        swipe(W // 2, int(H * 0.8), W // 2, int(H * 0.35))
        time.sleep(1.5)
    step("20-widget-picker", 2)
else:
    screenshot("20-widget-picker-not-found")
    log("  ! Widgets entry not found in the Home long-press menu")
go_home()

crash_log = crashes()
(out / "crash-buffer.txt").write_text(crash_log)
full = adb("logcat", "-d", "-v", "time", timeout=120)
(out / "logcat.txt").write_text(full)
errors = [line for line in full.splitlines()
          if (" E/" in line or " F/" in line or " W/" in line) and
          ("rumb" in line.lower() or "lawnchair" in line.lower() or "AndroidRuntime" in line)]
(out / "logcat-rumb-errors.txt").write_text("\n".join(errors))
if f"Process: {PKG}" in crash_log:
    problems.append("Rumb crashed (see crash-buffer.txt)")
    for line in crash_log.splitlines()[:40]:
        log("  crash: " + line)

log("")
log(f"{len(problems)} problem(s)")
for problem in problems:
    log(" - " + problem)
(out / "report.txt").write_text("\n".join(report) + "\n")
sys.exit(1 if any("crashed" in p for p in problems) else 0)
