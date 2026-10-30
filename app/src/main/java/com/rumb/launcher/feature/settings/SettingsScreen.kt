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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    notificationAccess: Boolean,
    onBack: () -> Unit,
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
    onNotificationDots: (Boolean) -> Unit,
    onNotificationAccess: () -> Unit,
    onSwipeUp: (GestureAction) -> Unit,
    onSwipeDown: (GestureAction) -> Unit,
    onDoubleTap: (GestureAction) -> Unit,
    onPinch: (GestureAction) -> Unit,
    onHomeSettings: () -> Unit,
    onWallpaper: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val supportedGestureActions = GestureAction.entries.filter { it != GestureAction.OPEN_NOTIFICATIONS }
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home") }
            Text("Rumb Launcher settings", style = MaterialTheme.typography.titleLarge)
        }
    }) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
        ) {
            Heading("Appearance")
            Label("Theme")
            Choices(ThemePreference.entries, settings.theme, { it.label() }, onTheme)
            Toggle("Material You colors", "Uses your system palette on Android 12 and newer", settings.dynamicColor, onDynamicColor, Build.VERSION.SDK_INT >= 31)
            Label("Icon shape")
            Choices(IconShape.entries, settings.iconShape, { it.label() }, onIconShape)
            Toggle(
                "Themed icons",
                "Uses app monochrome artwork where Android and the app provide it",
                settings.themedIcons,
                onThemedIcons,
                Build.VERSION.SDK_INT >= 33,
            )
            Label("Icon size")
            Choices(listOf(44, 52, 60, 68), settings.iconSize, { "$it dp" }, onIconSize)

            Heading("Home layout")
            Label("Columns")
            Choices((4..8).toList(), settings.columns, Int::toString, onColumns)
            Label("Rows")
            Choices((4..8).toList(), settings.rows, Int::toString, onRows)
            Label("Pages")
            Choices((1..9).toList(), settings.pageCount, Int::toString, onPages)
            Toggle("Lock layout", "Prevents moving, combining, adding, removing, or editing home items", settings.layoutLocked, onLayoutLock)
            if (hiddenAppCount > 0) OutlinedButton(onClick = onUnhideAll, Modifier.fillMaxWidth()) {
                Text("Restore $hiddenAppCount hidden ${if (hiddenAppCount == 1) "app" else "apps"}")
            }
            OutlinedButton(onClick = onAddWidget, enabled = !settings.layoutLocked, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddBox, null); Spacer(Modifier.width(8.dp)); Text("Add widget")
            }
            Text(
                "Android's system picker handles widget selection and any provider configuration. Widget bindings remain local to this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Heading("Clock")
            Label("Style")
            Choices(ClockStyle.entries, settings.clockStyle, { it.label() }, onClockStyle)
            Label("Alignment")
            Choices(ClockAlignment.entries, settings.clockAlignment, { it.label() }, onClockAlignment)
            Label("Time format")
            Choices(ClockTimeFormat.entries, settings.clockTimeFormat, { it.label() }, onClockTimeFormat)
            Label("Font")
            Choices(ClockFont.entries, settings.clockFont, { it.label() }, onClockFont)
            Label("Size")
            Choices(listOf(30, 40, 50, 58, 66, 72), settings.clockSize, { "$it sp" }, onClockSize)
            Toggle("Show date", null, settings.showDate, onShowDate)
            Toggle("Show seconds", "Updates every second and uses slightly more power", settings.showSeconds, onShowSeconds)

            Heading("Gestures")
            Label("Swipe up")
            Choices(supportedGestureActions, settings.swipeUpAction, { it.label() }, onSwipeUp)
            Label("Swipe down")
            Choices(supportedGestureActions, settings.swipeDownAction, { it.label() }, onSwipeDown)
            Label("Double tap")
            Choices(supportedGestureActions, settings.doubleTapAction, { it.label() }, onDoubleTap)
            Label("Pinch")
            Choices(supportedGestureActions, settings.pinchAction, { it.label() }, onPinch)
            Text(
                "Pinch requires two pointers to change span and is observed without consuming app, widget, drag, or page events. Android provides no public API for third-party launchers to expand notifications. Rumb does not replace the secure system lock screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )

            Heading("Notification dots")
            Toggle("Show notification dots", "Only per-app counts are kept in memory. Notification content is never persisted.", settings.notificationDots, onNotificationDots)
            OutlinedButton(onClick = onNotificationAccess, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(8.dp)); Text(if (notificationAccess) "Notification access granted" else "Grant notification access")
            }

            Heading("Backup")
            Text("Exports layout and settings as versioned JSON. Device-specific widget IDs and bindings are excluded.", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onExport, Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Export") }
                OutlinedButton(onClick = onImport, Modifier.weight(1f)) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(6.dp)); Text("Import") }
            }

            Heading("System")
            Button(onClick = onHomeSettings, Modifier.fillMaxWidth()) { Icon(Icons.Default.Home, null); Spacer(Modifier.width(8.dp)); Text("Choose default home app") }
            OutlinedButton(onClick = onWallpaper, Modifier.fillMaxWidth().padding(top = 10.dp)) { Icon(Icons.Default.Palette, null); Spacer(Modifier.width(8.dp)); Text("Change wallpaper") }
            Text(
                "Rumb Launcher ${BuildConfig.VERSION_NAME} · Offline by design",
                Modifier.fillMaxWidth().padding(vertical = 30.dp), textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable private fun Heading(value: String) { HorizontalDivider(Modifier.padding(top = 24.dp)); Text(value, Modifier.padding(top = 18.dp, bottom = 10.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
@Composable private fun Label(value: String) { Text(value, Modifier.padding(top = 12.dp, bottom = 7.dp), style = MaterialTheme.typography.labelLarge) }

@Composable
private fun Toggle(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
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
    GestureAction.OPEN_SETTINGS -> "Settings"
    GestureAction.OPEN_NOTIFICATIONS -> "Notifications"
    GestureAction.LOCK_LAYOUT -> "Toggle layout lock"
}
