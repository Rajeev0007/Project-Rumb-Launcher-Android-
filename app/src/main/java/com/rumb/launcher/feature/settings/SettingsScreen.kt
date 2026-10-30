package com.rumb.launcher.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rumb.launcher.BuildConfig
import com.rumb.launcher.domain.ClockAlignment
import com.rumb.launcher.domain.ClockFont
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.ClockTimeFormat
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.ThemePreference
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.feature.customize.VisualStylePreview
import com.rumb.launcher.ui.components.VisualBackdrop
import com.rumb.launcher.ui.components.VisualSurface
import com.rumb.launcher.ui.components.visualTokens

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onBack: () -> Unit,
    onVisualStyle: (VisualStyle) -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onIconShape: (IconShape) -> Unit,
    onThemedIcons: (Boolean) -> Unit,
    onColumns: (Int) -> Unit,
    onRows: (Int) -> Unit,
    onIconSize: (Int) -> Unit,
    onPages: (Int) -> Unit,
    hiddenAppCount: Int,
    onUnhideAll: () -> Unit,
    onLayoutLock: (Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onClockStyle: (ClockStyle) -> Unit,
    onClockAlignment: (ClockAlignment) -> Unit,
    onClockTimeFormat: (ClockTimeFormat) -> Unit,
    onClockFont: (ClockFont) -> Unit,
    onClockSize: (Int) -> Unit,
    onShowDate: (Boolean) -> Unit,
    onShowSeconds: (Boolean) -> Unit,
    onSwipeUp: (GestureAction) -> Unit,
    onSwipeDown: (GestureAction) -> Unit,
    onDoubleTap: (GestureAction) -> Unit,
    onPinch: (GestureAction) -> Unit,
    onHomeSettings: () -> Unit,
    onWallpaper: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val tokens = visualTokens(settings.visualStyle)
    val supportedGestureActions = GestureAction.entries
    VisualBackdrop(settings.visualStyle, Modifier.fillMaxSize(), opaque = true) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home", tint = tokens.foreground) }
                Column(Modifier.padding(start = 6.dp)) {
                    Text("Settings", color = tokens.foreground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Shape your Rumb experience", color = tokens.mutedForeground, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingsCard("Visual style", "Four original materials with live preview", settings.visualStyle) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(VisualStyle.entries.size) { index ->
                            val style = VisualStyle.entries[index]
                            VisualStylePreview(style, style == settings.visualStyle, onVisualStyle)
                        }
                    }
                }

                SettingsCard("Appearance", "Color, icon geometry, and scale", settings.visualStyle) {
                    Label("Theme", tokens.foreground)
                    Choices(ThemePreference.entries, settings.theme, { it.label() }, onTheme)
                    Toggle("Material You colors", "Uses your system palette on Android 12 and newer", settings.dynamicColor, onDynamicColor, Build.VERSION.SDK_INT >= 31, tokens)
                    Label("Icon shape", tokens.foreground)
                    Choices(IconShape.entries, settings.iconShape, { it.label() }, onIconShape)
                    Toggle(
                        "Themed icons",
                        "Uses monochrome artwork when Android and the app provide it",
                        settings.themedIcons,
                        onThemedIcons,
                        Build.VERSION.SDK_INT >= 33,
                        tokens,
                    )
                    Label("Icon size", tokens.foreground)
                    Choices(listOf(44, 52, 60, 68), settings.iconSize, { "$it dp" }, onIconSize)
                }

                SettingsCard("Home layout", "Pages, grid, widgets, and editing protection", settings.visualStyle) {
                    Label("Columns", tokens.foreground)
                    Choices((4..8).toList(), settings.columns, Int::toString, onColumns)
                    Label("Rows", tokens.foreground)
                    Choices((4..8).toList(), settings.rows, Int::toString, onRows)
                    Label("Pages", tokens.foreground)
                    Choices((1..9).toList(), settings.pageCount, Int::toString, onPages)
                    Toggle("Lock layout", "Prevents moving, combining, adding, removing, and editing home items", settings.layoutLocked, onLayoutLock, true, tokens)
                    if (hiddenAppCount > 0) OutlinedButton(onClick = onUnhideAll, Modifier.fillMaxWidth()) {
                        Text("Restore $hiddenAppCount hidden ${if (hiddenAppCount == 1) "app" else "apps"}")
                    }
                    OutlinedButton(onClick = onAddWidget, enabled = !settings.layoutLocked, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AddBox, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (settings.layoutLocked) "Unlock layout to add widgets" else "Add widget")
                    }
                    Text(
                        "Android handles widget selection and provider setup. Widget bindings stay on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedForeground,
                    )
                }

                SettingsCard("Clock", "Time, date, alignment, and typography", settings.visualStyle) {
                    Label("Style", tokens.foreground)
                    Choices(ClockStyle.entries, settings.clockStyle, { it.label() }, onClockStyle)
                    if (settings.clockStyle != ClockStyle.HIDDEN) {
                        Label("Alignment", tokens.foreground)
                        Choices(ClockAlignment.entries, settings.clockAlignment, { it.label() }, onClockAlignment)
                        Label("Time format", tokens.foreground)
                        Choices(ClockTimeFormat.entries, settings.clockTimeFormat, { it.label() }, onClockTimeFormat)
                        Label("Font", tokens.foreground)
                        Choices(ClockFont.entries, settings.clockFont, { it.label() }, onClockFont)
                        Label("Size", tokens.foreground)
                        Choices(listOf(30, 40, 50, 58, 66, 72), settings.clockSize, { "$it sp" }, onClockSize)
                        Toggle("Show date", null, settings.showDate, onShowDate, true, tokens)
                        Toggle("Show seconds", "Updates each second and uses slightly more power", settings.showSeconds, onShowSeconds, true, tokens)
                    } else {
                        Text(
                            "Home shows only wallpaper, your items, and the compact dock. Other clock choices remain saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedForeground,
                        )
                    }
                }

                SettingsCard("Gestures", "Shortcuts that yield to apps, widgets, pages, and drag", settings.visualStyle) {
                    Label("Swipe up", tokens.foreground)
                    Choices(supportedGestureActions, settings.swipeUpAction, { it.label() }, onSwipeUp)
                    Label("Swipe down", tokens.foreground)
                    Choices(supportedGestureActions, settings.swipeDownAction, { it.label() }, onSwipeDown)
                    Label("Double tap", tokens.foreground)
                    Choices(supportedGestureActions, settings.doubleTapAction, { it.label() }, onDoubleTap)
                    Label("Pinch", tokens.foreground)
                    Choices(supportedGestureActions, settings.pinchAction, { it.label() }, onPinch)
                    Text(
                        "Pinch requires two pointers. Long-press only activates customization on unconsumed empty Home space. Rumb never replaces Android's secure lock screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedForeground,
                    )
                }

                SettingsCard("Backup", "Portable, versioned, and validated", settings.visualStyle) {
                    Text("Exports settings and layout as JSON. Device-local widget IDs and bindings are excluded.", style = MaterialTheme.typography.bodySmall, color = tokens.mutedForeground)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onExport, Modifier.weight(1f)) {
                            Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Export")
                        }
                        OutlinedButton(onClick = onImport, Modifier.weight(1f)) {
                            Icon(Icons.Default.Restore, null); Spacer(Modifier.width(6.dp)); Text("Import")
                        }
                    }
                }

                SettingsCard("System", "Android-managed launcher and wallpaper controls", settings.visualStyle) {
                    Button(onClick = onHomeSettings, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Home, null); Spacer(Modifier.width(8.dp)); Text("Choose default home app")
                    }
                    OutlinedButton(onClick = onWallpaper, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Palette, null); Spacer(Modifier.width(8.dp)); Text("Change wallpaper")
                    }
                }

                Text(
                    "Rumb Launcher ${BuildConfig.VERSION_NAME} · Hardened standard edition · Offline by design",
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    textAlign = TextAlign.Center,
                    color = tokens.mutedForeground,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, style: VisualStyle, content: @Composable () -> Unit) {
    val tokens = visualTokens(style)
    VisualSurface(style, Modifier.fillMaxWidth(), strong = false, elevation = 7.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = tokens.foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = tokens.mutedForeground, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun Label(value: String, color: Color) {
    Text(value, Modifier.padding(top = 6.dp), color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun Toggle(
    title: String,
    description: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean,
    tokens: com.rumb.launcher.ui.components.VisualTokens,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) tokens.foreground else tokens.mutedForeground.copy(.55f), style = MaterialTheme.typography.bodyLarge)
            description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = tokens.mutedForeground) }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun <T> Choices(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options.size) { index ->
            val option = options[index]
            if (option == selected) FilledTonalButton(onClick = { onSelect(option) }) { Text(label(option)) }
            else OutlinedButton(onClick = { onSelect(option) }) { Text(label(option)) }
        }
    }
}

private fun ThemePreference.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun IconShape.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun ClockStyle.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun ClockAlignment.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun ClockFont.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun ClockTimeFormat.label() = when (this) {
    ClockTimeFormat.SYSTEM -> "System"
    ClockTimeFormat.TWELVE_HOUR -> "12-hour"
    ClockTimeFormat.TWENTY_FOUR_HOUR -> "24-hour"
}
private fun GestureAction.label() = when (this) {
    GestureAction.NONE -> "None"
    GestureAction.OPEN_DRAWER -> "All apps"
    GestureAction.OPEN_QUICK_PANEL -> "Quick panel"
    GestureAction.OPEN_SETTINGS -> "Settings"
    GestureAction.LOCK_LAYOUT -> "Toggle layout lock"
}
