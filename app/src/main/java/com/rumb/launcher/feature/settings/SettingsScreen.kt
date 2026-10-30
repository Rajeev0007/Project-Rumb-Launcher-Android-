package com.rumb.launcher.feature.settings

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.rumb.launcher.BuildConfig
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.LauncherViewModel
import com.rumb.launcher.domain.ClockAlignment
import com.rumb.launcher.domain.ClockFont
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.ClockTimeFormat
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.ThemePreference
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.domain.iconAppearance
import com.rumb.launcher.feature.customize.VisualStylePreview
import com.rumb.launcher.platform.icons.IconPack
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.LauncherClock
import com.rumb.launcher.ui.components.displayName
import com.rumb.launcher.ui.components.visualTokens

private enum class SettingsPage(val title: String, val parent: SettingsPage?) {
    ROOT("Settings", null),
    APPEARANCE("Appearance", ROOT),
    HOME("Home screen", ROOT),
    LIBRARY("App library", ROOT),
    HIDDEN_APPS("Hidden apps", LIBRARY),
    CLOCK("Clock", ROOT),
    GESTURES("Gestures", ROOT),
    ACCESS("Notifications & access", ROOT),
    BACKUP("Backup & restore", ROOT),
    ABOUT("About", ROOT),
}

@Composable
fun SettingsScreen(
    state: LauncherUiState,
    model: LauncherViewModel,
    onBack: () -> Unit,
    onAddWidget: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
    BackHandler(page != SettingsPage.ROOT) { page = page.parent ?: SettingsPage.ROOT }
    SystemBarAppearance()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal && targetState.parent != null
                val enter = slideInHorizontally(tween(280)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(220))
                val exit = slideOutHorizontally(tween(220)) { if (forward) -it / 5 else it / 5 } + fadeOut(tween(160))
                enter togetherWith exit
            },
            label = "settings page",
        ) { current ->
            SettingsPageScaffold(
                title = current.title,
                onBack = { if (current == SettingsPage.ROOT) onBack() else page = current.parent ?: SettingsPage.ROOT },
            ) {
                val settings = state.settings
                when (current) {
                    SettingsPage.ROOT -> rootPage(state) { page = it }
                    SettingsPage.APPEARANCE -> appearancePage(state, model)
                    SettingsPage.HOME -> homePage(settings, model, onAddWidget)
                    SettingsPage.LIBRARY -> libraryPage(state, model) { page = SettingsPage.HIDDEN_APPS }
                    SettingsPage.HIDDEN_APPS -> hiddenAppsPage(state, model)
                    SettingsPage.CLOCK -> clockPage(settings, model)
                    SettingsPage.GESTURES -> gesturesPage(state, model)
                    SettingsPage.ACCESS -> accessPage(state, model)
                    SettingsPage.BACKUP -> backupPage(onExport, onImport)
                    SettingsPage.ABOUT -> aboutPage(state, model)
                }
            }
        }
    }
}

/** Settings has an opaque surface, so status bar icons follow its brightness while it is shown. */
@Composable
private fun SystemBarAppearance() {
    val view = LocalView.current
    val lightSurface = MaterialTheme.colorScheme.surface.luminance() > .5f
    DisposableEffect(view, lightSurface) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = lightSurface
        controller?.isAppearanceLightNavigationBars = lightSurface
        onDispose {
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
        }
    }
}

@Composable
private fun SettingsPageScaffold(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 28.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            content = {
                content()
                item(key = "nav-spacer") { Spacer(Modifier.navigationBarsPadding()) }
            },
        )
    }
}

private fun LazyListScope.group(key: String, title: String? = null, content: @Composable () -> Unit) {
    item(key = key) {
        SettingsGroup(title) { content() }
    }
}

// ---------------------------------------------------------------------------------------------
// Root

private fun LazyListScope.rootPage(state: LauncherUiState, open: (SettingsPage) -> Unit) {
    val settings = state.settings
    if (!state.isDefaultLauncher) {
        item(key = "default-banner") {
            SettingsBanner(
                title = "Make Rumb your Home app",
                body = "Pressing Home still opens another launcher. Choose Rumb Launcher Next as the default to use it everywhere.",
                action = "Set as default",
                onAction = { open(SettingsPage.ABOUT) },
            )
        }
    }
    group("root-look") {
        SettingsNavRow("Appearance", "${settings.visualStyle.displayName()} · ${settings.theme.label()} theme", Icons.Default.Palette, SettingsTint.Purple) { open(SettingsPage.APPEARANCE) }
        SettingsDivider()
        SettingsNavRow("Home screen", homeSummary(settings), Icons.Default.Home, SettingsTint.Blue) { open(SettingsPage.HOME) }
        SettingsDivider()
        SettingsNavRow("App library", librarySummary(settings), Icons.Default.Apps, SettingsTint.Teal) { open(SettingsPage.LIBRARY) }
        SettingsDivider()
        SettingsNavRow("Clock", clockSummary(settings), Icons.Default.Schedule, SettingsTint.Orange) { open(SettingsPage.CLOCK) }
    }
    group("root-behaviour") {
        SettingsNavRow("Gestures", "Swipe up: ${settings.swipeUpAction.label()} · Double tap: ${settings.doubleTapAction.label()}", Icons.Default.Gesture, SettingsTint.Pink) { open(SettingsPage.GESTURES) }
        SettingsDivider()
        SettingsNavRow(
            "Notifications & access",
            if (settings.notificationDots) "Notification dots on" else "Notification dots off",
            Icons.Default.Notifications,
            SettingsTint.Red,
        ) { open(SettingsPage.ACCESS) }
    }
    group("root-system") {
        SettingsNavRow("Backup & restore", "Export or import your layout", Icons.Default.Backup, SettingsTint.Green) { open(SettingsPage.BACKUP) }
        SettingsDivider()
        SettingsNavRow("About", "Version ${BuildConfig.VERSION_NAME}", Icons.Default.Info, SettingsTint.Slate) { open(SettingsPage.ABOUT) }
    }
}

private fun homeSummary(settings: LauncherSettings): String {
    val pages = if (settings.pageCount == 1) "1 page" else "${settings.pageCount} pages"
    val lock = if (settings.layoutLocked) " · Locked" else ""
    return "${settings.columns} × ${settings.rows} grid · $pages$lock"
}

private fun librarySummary(settings: LauncherSettings): String = buildList {
    add(if (settings.drawerSuggestions) "Suggestions on" else "Suggestions off")
    if (settings.hiddenApps.isNotEmpty()) add("${settings.hiddenApps.size} hidden")
}.joinToString(" · ")

private fun clockSummary(settings: LauncherSettings): String =
    if (settings.clockStyle == ClockStyle.HIDDEN) "Hidden" else "${settings.clockStyle.label()} · ${settings.clockTimeFormat.label()}"

// ---------------------------------------------------------------------------------------------
// Appearance

private fun LazyListScope.appearancePage(state: LauncherUiState, model: LauncherViewModel) {
    val settings = state.settings
    item(key = "preview") { IconPreview(state, model) }
    item(key = "styles") {
        SettingsGroup("Style") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(VisualStyle.entries, key = { it.name }) { style ->
                    VisualStylePreview(style, style == settings.visualStyle, model::updateVisualStyle)
                }
            }
        }
    }
    group("colors", "Colours") {
        SettingsChoiceRow(
            title = "Theme",
            options = ThemePreference.entries,
            selected = settings.theme,
            label = { it.label() },
            onSelect = model::updateTheme,
            icon = Icons.Default.DarkMode,
            tint = SettingsTint.Purple,
            description = {
                when (it) {
                    ThemePreference.SYSTEM -> "Follow the device setting"
                    ThemePreference.AMOLED -> "Pure black surfaces for OLED screens"
                    else -> null
                }
            },
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Material You colours",
            summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Match menus to your wallpaper" else "Needs Android 12 or newer",
            checked = settings.dynamicColor,
            onCheckedChange = model::updateDynamicColor,
            icon = Icons.Default.ColorLens,
            tint = SettingsTint.Purple,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
    }
    group("icons", "Icons") {
        SettingsChoiceRow(
            title = "Icon shape",
            options = IconShape.entries,
            selected = settings.iconShape,
            label = { it.label() },
            onSelect = model::updateIconShape,
            icon = Icons.Default.Category,
            tint = SettingsTint.Blue,
        )
        SettingsDivider()
        SettingsSliderRow(
            title = "Icon size",
            value = settings.iconSize,
            range = 42..72,
            step = 2,
            onValueCommitted = model::updateIconSize,
            icon = Icons.Default.PhotoSizeSelectLarge,
            tint = SettingsTint.Blue,
            valueLabel = { "$it dp" },
        )
        SettingsDivider()
        IconPackRow(settings, state.iconPacks, model::updateIconPack)
        SettingsDivider()
        SettingsSwitchRow(
            title = "Themed icons",
            summary = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> "Needs Android 13 or newer"
                settings.iconPack != null -> "Not available while an icon pack is in use"
                else -> "Single-colour icons for apps that provide them"
            },
            checked = settings.themedIcons,
            onCheckedChange = model::updateThemedIcons,
            icon = Icons.Default.AutoAwesome,
            tint = SettingsTint.Blue,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && settings.iconPack == null,
        )
    }
}

@Composable
private fun IconPackRow(settings: LauncherSettings, packs: List<IconPack>, onSelect: (String?) -> Unit) {
    val selected = packs.firstOrNull { it.packageName == settings.iconPack }
    val missing = settings.iconPack != null && selected == null
    SettingsChoiceRow(
        title = "Icon pack",
        options = listOf<IconPack?>(null) + packs,
        selected = selected,
        label = { pack ->
            when {
                pack != null -> pack.label
                missing -> "Pack not installed · using system icons"
                packs.isEmpty() -> "System icons · no packs installed"
                else -> "System icons"
            }
        },
        onSelect = { onSelect(it?.packageName) },
        icon = Icons.Default.Widgets,
        tint = SettingsTint.Blue,
        description = { if (it == null) "Each app's own icon" else null },
    )
}

/** Four real app icons rendered with the current shape, size, pack, and theme on a wallpaper-like card. */
@Composable
private fun IconPreview(state: LauncherUiState, model: LauncherViewModel) {
    val tokens = visualTokens(state.settings.visualStyle)
    val sample = remember(state.apps) { state.apps.take(4) }
    if (sample.isEmpty()) return
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(tokens.accent.copy(alpha = .85f), tokens.accentSecondary.copy(alpha = .75f), Color(0xFF14121F))))
            .padding(vertical = 20.dp, horizontal = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            sample.forEach { app ->
                AppTile(
                    app = app,
                    iconLoader = model.iconLoader,
                    iconSize = state.settings.iconSize.dp,
                    appearance = state.settings.iconAppearance,
                    showLabel = state.settings.showHomeLabels,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Home screen

private fun LazyListScope.homePage(settings: LauncherSettings, model: LauncherViewModel, onAddWidget: () -> Unit) {
    val locked = settings.layoutLocked
    if (locked) {
        item(key = "locked-note") {
            SettingsNote("The layout is locked. Unlock it below to change the grid, pages, or widgets.", Modifier.padding(horizontal = 16.dp))
        }
    }
    group("grid", "Layout") {
        SettingsStepperRow("Columns", settings.columns, 4..8, model::updateColumns, icon = Icons.Default.ViewColumn, tint = SettingsTint.Blue, enabled = !locked)
        SettingsDivider()
        SettingsStepperRow("Rows", settings.rows, 4..8, model::updateRows, icon = Icons.Default.GridView, tint = SettingsTint.Blue, enabled = !locked)
        SettingsDivider()
        SettingsStepperRow(
            "Pages",
            settings.pageCount,
            1..9,
            model::updatePageCount,
            summary = "Pages that hold items can't be removed",
            icon = Icons.Default.Apps,
            tint = SettingsTint.Blue,
            enabled = !locked,
        )
    }
    group("home-look", "Display") {
        SettingsSwitchRow("App labels", settings.showHomeLabels, model::updateShowHomeLabels, "Names under apps and folders", Icons.Default.Title, SettingsTint.Teal)
        SettingsDivider()
        SettingsSwitchRow("Search bar", settings.homeSearchBar, model::updateHomeSearchBar, "Search pill above the dock", Icons.Default.Search, SettingsTint.Teal)
    }
    group("editing", "Editing") {
        SettingsSwitchRow(
            "Lock layout",
            settings.layoutLocked,
            model::updateLayoutLocked,
            "Prevents moving, adding, or removing Home items",
            Icons.Default.Lock,
            SettingsTint.Orange,
        )
        SettingsDivider()
        SettingsNavRow("Add widget", if (locked) "Unlock the layout first" else "Choose from Android's widget list", Icons.Default.Widgets, SettingsTint.Green, onAddWidget)
        SettingsDivider()
        SettingsNavRow("Wallpaper", "Open Android's wallpaper picker", Icons.Default.Wallpaper, SettingsTint.Pink, model.systemActions::openWallpaperPicker)
    }
}

// ---------------------------------------------------------------------------------------------
// App library

private fun LazyListScope.libraryPage(state: LauncherUiState, model: LauncherViewModel, openHidden: () -> Unit) {
    val settings = state.settings
    group("library-search", "Search") {
        SettingsSwitchRow(
            "Open keyboard automatically",
            settings.drawerAutoKeyboard,
            model::updateDrawerAutoKeyboard,
            "Focus search each time the library opens",
            Icons.Default.Keyboard,
            SettingsTint.Teal,
        )
    }
    group("library-suggestions", "Suggestions") {
        SettingsSwitchRow(
            "Suggested apps",
            settings.drawerSuggestions,
            model::updateDrawerSuggestions,
            "Apps you open often and recently. Stays on this device.",
            Icons.Default.AutoAwesome,
            SettingsTint.Purple,
        )
        SettingsDivider()
        SettingsRow(
            title = "Clear suggestion history",
            summary = "Start ranking from scratch",
            icon = Icons.Default.History,
            tint = SettingsTint.Slate,
            onClick = model::clearSuggestions,
        )
    }
    group("library-hidden", "Privacy") {
        val count = settings.hiddenApps.size
        SettingsNavRow(
            "Hidden apps",
            when (count) {
                0 -> "None hidden. Long-press an app and choose Hide."
                1 -> "1 app hidden"
                else -> "$count apps hidden"
            },
            Icons.Default.VisibilityOff,
            SettingsTint.Slate,
            openHidden,
        )
    }
}

private fun LazyListScope.hiddenAppsPage(state: LauncherUiState, model: LauncherViewModel) {
    val hidden = state.allApps.filter { it.id in state.settings.hiddenApps }
    val missing = state.settings.hiddenApps.size - hidden.size
    if (hidden.isEmpty() && missing == 0) {
        item(key = "empty") {
            SettingsNote("No apps are hidden. Long-press an app in the library and choose Hide to keep it out of the list.", Modifier.padding(horizontal = 16.dp))
        }
        return
    }
    group("hidden-list") {
        hidden.forEachIndexed { index, app ->
            if (index > 0) SettingsDivider()
            SettingsRow(
                title = app.label,
                summary = app.profileLabel?.let { "$it profile" },
                trailing = { TextButton(onClick = { model.unhide(app.id) }) { Text("Show") } },
            ) 
        }
        if (missing > 0) {
            if (hidden.isNotEmpty()) SettingsDivider()
            SettingsNote("$missing hidden ${if (missing == 1) "app is" else "apps are"} not installed right now.")
        }
    }
    item(key = "unhide-all") {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = model::unhideAll) { Text("Show all hidden apps") }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Clock

private fun LazyListScope.clockPage(settings: LauncherSettings, model: LauncherViewModel) {
    if (settings.clockStyle != ClockStyle.HIDDEN) item(key = "clock-preview") { ClockPreview(settings) }
    group("clock-style", "Style") {
        SettingsChoiceRow(
            "Clock",
            ClockStyle.entries,
            settings.clockStyle,
            { it.label() },
            model::updateClockStyle,
            icon = Icons.Default.AccessTime,
            tint = SettingsTint.Orange,
            description = {
                when (it) {
                    ClockStyle.HIDDEN -> "Wallpaper only; uses no timer"
                    ClockStyle.LARGE -> "Big time with the date"
                    ClockStyle.COMPACT -> "Smaller time with the date"
                    ClockStyle.MINIMAL -> "Thin time, no date"
                }
            },
        )
        if (settings.clockStyle != ClockStyle.HIDDEN) {
            SettingsDivider()
            SettingsChoiceRow("Time format", ClockTimeFormat.entries, settings.clockTimeFormat, { it.label() }, model::updateClockTimeFormat, icon = Icons.Default.Timer, tint = SettingsTint.Orange)
            SettingsDivider()
            SettingsChoiceRow("Alignment", ClockAlignment.entries, settings.clockAlignment, { it.label() }, model::updateClockAlignment, icon = Icons.Default.FormatAlignCenter, tint = SettingsTint.Orange)
            SettingsDivider()
            SettingsChoiceRow("Font", ClockFont.entries, settings.clockFont, { it.label() }, model::updateClockFont, icon = Icons.Default.TextFields, tint = SettingsTint.Orange)
            SettingsDivider()
            SettingsSliderRow("Size", settings.clockSize, 28..72, 2, model::updateClockSize, icon = Icons.Default.FormatSize, tint = SettingsTint.Orange, valueLabel = { "$it sp" })
        }
    }
    if (settings.clockStyle != ClockStyle.HIDDEN) {
        group("clock-details", "Details") {
            SettingsSwitchRow(
                "Show date",
                settings.showDate,
                model::updateShowDate,
                if (settings.clockStyle == ClockStyle.MINIMAL) "The minimal style never shows the date" else null,
                Icons.Default.CalendarToday,
                SettingsTint.Orange,
                enabled = settings.clockStyle != ClockStyle.MINIMAL,
            )
            SettingsDivider()
            SettingsSwitchRow("Show seconds", settings.showSeconds, model::updateShowSeconds, "Updates every second and uses a little more battery", Icons.Default.Timer, SettingsTint.Orange)
        }
    }
}

@Composable
private fun ClockPreview(settings: LauncherSettings) {
    val tokens = visualTokens(settings.visualStyle)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(tokens.accent.copy(alpha = .85f), tokens.accentSecondary.copy(alpha = .7f), Color(0xFF14121F))))
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        LauncherClock(
            style = settings.clockStyle,
            alignment = settings.clockAlignment,
            timeFormat = settings.clockTimeFormat,
            font = settings.clockFont,
            size = settings.clockSize,
            showDate = settings.showDate,
            showSeconds = settings.showSeconds,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Gestures

private fun LazyListScope.gesturesPage(state: LauncherUiState, model: LauncherViewModel) {
    val settings = state.settings
    group("gestures", "On empty Home space") {
        GestureRow("Swipe up", Icons.Default.ArrowUpward, settings.swipeUpAction, model::updateSwipeUp)
        SettingsDivider()
        GestureRow("Swipe down", Icons.Default.ArrowDownward, settings.swipeDownAction, model::updateSwipeDown)
        SettingsDivider()
        GestureRow("Double tap", Icons.Default.TouchApp, settings.doubleTapAction, model::updateDoubleTap)
        SettingsDivider()
        GestureRow("Pinch", Icons.Default.ZoomOutMap, settings.pinchAction, model::updatePinch)
    }
    val usesLock = listOf(settings.swipeUpAction, settings.swipeDownAction, settings.doubleTapAction, settings.pinchAction)
        .contains(GestureAction.LOCK_SCREEN)
    if (usesLock) {
        group("lock-status", "Lock screen") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                SettingsNote("Locking the screen from Home needs Android 9 or newer.")
            } else {
                SettingsStatusRow(
                    title = "Rumb screen lock",
                    ok = state.lockServiceConnected,
                    okLabel = "Ready",
                    notOkLabel = "Turn it on in Accessibility settings",
                    icon = Icons.Default.Accessibility,
                    tint = SettingsTint.Pink,
                    actionLabel = "Open",
                    onAction = model::openAccessibilitySettings,
                )
                if (!state.lockServiceConnected) SettingsNote(RESTRICTED_HINT)
            }
        }
    }
    group("feedback", "Feedback") {
        SettingsSwitchRow("Haptic feedback", settings.haptics, model::updateHaptics, "Vibrate on long press, drag, and index scrubbing", Icons.Default.Vibration, SettingsTint.Pink)
    }
    item(key = "gesture-note") {
        SettingsNote(
            "Gestures only start on empty Home space, so apps, widgets, and page swipes always come first. Long-press empty space to customise Home.",
            Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun GestureRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: GestureAction, onSelect: (GestureAction) -> Unit) {
    SettingsChoiceRow(
        title = title,
        options = GestureAction.entries,
        selected = selected,
        label = { it.label() },
        onSelect = onSelect,
        icon = icon,
        tint = SettingsTint.Pink,
        description = {
            when (it) {
                GestureAction.OPEN_SEARCH -> "Open the library with the keyboard ready"
                GestureAction.LOCK_LAYOUT -> "Lock or unlock Home editing"
                GestureAction.LOCK_SCREEN -> "Turn off the screen (Android 9+, needs Accessibility)"
                else -> null
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Notifications & access

private fun LazyListScope.accessPage(state: LauncherUiState, model: LauncherViewModel) {
    val settings = state.settings
    group("dots", "Notification dots") {
        SettingsSwitchRow(
            "Show notification dots",
            settings.notificationDots,
            model::updateNotificationDots,
            "A dot on apps with unread notifications",
            Icons.Default.Notifications,
            SettingsTint.Red,
        )
        if (settings.notificationDots) {
            SettingsDivider()
            SettingsStatusRow(
                title = "Notification access",
                ok = state.notificationAccess,
                okLabel = "Allowed",
                notOkLabel = "Not allowed, so no dots are shown",
                icon = Icons.Default.Shield,
                tint = SettingsTint.Red,
                actionLabel = "Allow",
                onAction = model::openNotificationAccess,
            )
        }
        SettingsNote("Rumb reads only which app posted a notification, never its title or text. Ongoing notifications such as music and downloads don't get a dot.")
    }
    group("lock-access", "Screen lock gesture") {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            SettingsNote("Needs Android 9 or newer.")
        } else {
            SettingsStatusRow(
                title = "Rumb screen lock",
                ok = state.lockServiceConnected,
                okLabel = "On. Choose “Lock screen” for a gesture to use it.",
                notOkLabel = "Off",
                icon = Icons.Default.Accessibility,
                tint = SettingsTint.Pink,
                actionLabel = "Open",
                onAction = model::openAccessibilitySettings,
            )
            SettingsNote("This accessibility service can only lock the screen. It receives no events and can't read what's on screen.")
        }
    }
    item(key = "restricted") { SettingsNote(RESTRICTED_HINT, Modifier.padding(horizontal = 16.dp)) }
}

private const val RESTRICTED_HINT =
    "If Android says the setting is restricted, open App info for Rumb, tap ⋮, choose Allow restricted settings, and try again."

// ---------------------------------------------------------------------------------------------
// Backup & about

private fun LazyListScope.backupPage(onExport: () -> Unit, onImport: () -> Unit) {
    group("backup", "Layout file") {
        SettingsNavRow("Export", "Save settings and Home layout as a JSON file", Icons.Default.Save, SettingsTint.Green, onExport)
        SettingsDivider()
        SettingsNavRow("Import", "Replace the current setup from a backup file", Icons.Default.Restore, SettingsTint.Blue, onImport)
    }
    item(key = "backup-note") {
        SettingsNote(
            "Backups include appearance, gestures, Home layout, folders, hidden apps, and custom names. Widgets and suggestion history stay on this device.",
            Modifier.padding(horizontal = 16.dp),
        )
    }
}

private fun LazyListScope.aboutPage(state: LauncherUiState, model: LauncherViewModel) {
    item(key = "about-hero") {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.height(72.dp).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(SettingsTint.Purple, SettingsTint.Blue)))
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text("Rumb Launcher Next", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    group("about-system", "System") {
        SettingsStatusRow(
            title = "Default Home app",
            ok = state.isDefaultLauncher,
            okLabel = "Rumb is your Home app",
            notOkLabel = "Another launcher is the default",
            icon = Icons.Default.Home,
            tint = SettingsTint.Blue,
            actionLabel = "Change",
            onAction = model.systemActions::openHomeSettings,
        )
    }
    item(key = "privacy") {
        SettingsNote(
            "Offline by design: no internet permission, analytics, ads, or accounts. Optional notification dots and the screen-lock gesture stay off until you allow them in Android Settings.",
            Modifier.padding(horizontal = 16.dp),
        )
    }
    item(key = "about-footer") {
        Text(
            "Made with Kotlin and Jetpack Compose",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Labels

private fun ThemePreference.label() = when (this) {
    ThemePreference.SYSTEM -> "System"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
    ThemePreference.AMOLED -> "AMOLED black"
}

private fun IconShape.label() = when (this) {
    IconShape.SYSTEM -> "System default"
    IconShape.CIRCLE -> "Circle"
    IconShape.ROUNDED -> "Rounded square"
    IconShape.SQUIRCLE -> "Squircle"
}

private fun ClockStyle.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun ClockFont.label() = when (this) {
    ClockFont.SANS -> "Sans serif"
    ClockFont.SERIF -> "Serif"
    ClockFont.MONOSPACE -> "Monospace"
}
private fun ClockAlignment.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun ClockTimeFormat.label() = when (this) {
    ClockTimeFormat.SYSTEM -> "Follow system"
    ClockTimeFormat.TWELVE_HOUR -> "12-hour"
    ClockTimeFormat.TWENTY_FOUR_HOUR -> "24-hour"
}

private fun GestureAction.label() = when (this) {
    GestureAction.NONE -> "Nothing"
    GestureAction.OPEN_DRAWER -> "Open app library"
    GestureAction.OPEN_SEARCH -> "Search"
    GestureAction.OPEN_QUICK_PANEL -> "Quick panel"
    GestureAction.OPEN_SETTINGS -> "Settings"
    GestureAction.LOCK_LAYOUT -> "Lock Home layout"
    GestureAction.LOCK_SCREEN -> "Lock screen"
}
