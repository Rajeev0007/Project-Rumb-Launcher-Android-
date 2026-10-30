/*
 * Copyright 2026, Rajeev0007 (Rumb Launcher)
 *
 * Licensed under the GNU General Public License, version 3 or later.
 */

package app.lawnchair.ui.preferences.destinations

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.components.layout.PreferenceLayoutLazyColumn
import app.lawnchair.ui.preferences.components.layout.preferenceGroupItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A screen of the system Settings app that Rumb can open directly. */
private data class HiddenScreen(
    val label: String,
    val detail: String,
    val intent: Intent,
)

/** Well-known screens that phones ship but rarely link to. Only the ones a phone can open are shown. */
private class Shortcut(
    val label: String,
    val detail: String,
    val action: String? = null,
    val component: ComponentName? = null,
)

private const val SETTINGS = "com.android.settings"

private val SHORTCUTS = listOf(
    Shortcut("Developer options", "USB debugging, animation speed, background limits", action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    Shortcut("Notification log", "Every notification you received, even dismissed ones", component = ComponentName(SETTINGS, "$SETTINGS.Settings\$NotificationStationActivity")),
    Shortcut("Notification history", "Notifications from the last 24 hours", action = "android.settings.NOTIFICATION_HISTORY"),
    Shortcut("Running services", "What is running in the background right now", component = ComponentName(SETTINGS, "$SETTINGS.Settings\$DevRunningServicesActivity")),
    Shortcut("Battery optimisation", "Choose which apps may run unrestricted", action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    Shortcut("Battery saver", "Saver schedule and options", action = Settings.ACTION_BATTERY_SAVER_SETTINGS),
    Shortcut("Usage access", "Apps that can see your app usage", action = Settings.ACTION_USAGE_ACCESS_SETTINGS),
    Shortcut("Notification access", "Apps that can read your notifications", action = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
    Shortcut("Do Not Disturb access", "Apps that can change Do Not Disturb", action = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
    Shortcut("Do Not Disturb", "Schedules, people and apps that can interrupt", action = "android.settings.ZEN_MODE_SETTINGS"),
    Shortcut("Display over other apps", "Apps allowed to draw on top", action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
    Shortcut("Modify system settings", "Apps allowed to change system settings", action = Settings.ACTION_MANAGE_WRITE_SETTINGS),
    Shortcut("Install unknown apps", "Apps allowed to install other apps", action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
    Shortcut("All files access", "Apps that can manage all files", action = "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"),
    Shortcut("Picture-in-picture", "Apps allowed to float in a small window", action = "android.settings.PICTURE_IN_PICTURE_SETTINGS"),
    Shortcut("Default apps", "Home app, browser, phone and SMS", action = Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
    Shortcut("Home app", "Pick the default launcher", action = Settings.ACTION_HOME_SETTINGS),
    Shortcut("All apps", "Every installed app, including system apps", action = Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
    Shortcut("Data usage", "Mobile and Wi-Fi data per app", action = Settings.ACTION_DATA_USAGE_SETTINGS),
    Shortcut("Network & internet", "Mobile network, hotspot, private DNS", action = Settings.ACTION_WIRELESS_SETTINGS),
    Shortcut("VPN", "Always-on VPN and VPN apps", action = Settings.ACTION_VPN_SETTINGS),
    Shortcut("Night Light", "Warm screen schedule", action = "android.settings.NIGHT_DISPLAY_SETTINGS"),
    Shortcut("Accessibility", "Accessibility services and display tweaks", action = Settings.ACTION_ACCESSIBILITY_SETTINGS),
    Shortcut("About phone", "Build number, model and Android version", action = Settings.ACTION_DEVICE_INFO_SETTINGS),
    Shortcut("System UI Tuner", "Status bar icons and other System UI tweaks", component = ComponentName("com.android.systemui", "com.android.systemui.tuner.TunerActivity")),
)

private fun Shortcut.toIntent(): Intent {
    val intent = if (component != null) Intent(Intent.ACTION_MAIN).setComponent(component) else Intent(action)
    return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** True when the activity exists, is enabled, is exported, and needs no permission Rumb lacks. */
private fun Context.canOpen(info: ActivityInfo): Boolean = info.exported && info.enabled &&
    (info.permission == null || checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED)

@Suppress("DEPRECATION")
private fun PackageManager.resolve(intent: Intent): ActivityInfo? = if (Build.VERSION.SDK_INT >= 33) {
    resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L))?.activityInfo
} else {
    resolveActivity(intent, 0)?.activityInfo
}

@Suppress("DEPRECATION")
private fun PackageManager.activitiesOf(packageName: String): PackageInfo? = try {
    if (Build.VERSION.SDK_INT >= 33) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
    } else {
        getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    }
} catch (_: PackageManager.NameNotFoundException) {
    null
}

private fun loadShortcuts(context: Context): List<HiddenScreen> {
    val pm = context.packageManager
    return SHORTCUTS.mapNotNull { shortcut ->
        val intent = shortcut.toIntent()
        val info = pm.resolve(intent) ?: return@mapNotNull null
        if (!context.canOpen(info)) return@mapNotNull null
        HiddenScreen(shortcut.label, shortcut.detail, intent)
    }
}

/** Every screen the phone's Settings app lets other apps open, including ones with no menu entry. */
private fun loadAllScreens(context: Context): List<HiddenScreen> {
    val pm = context.packageManager
    val settingsPackage = pm.resolve(Intent(Settings.ACTION_SETTINGS))?.packageName ?: SETTINGS
    val activities = pm.activitiesOf(settingsPackage)?.activities ?: return emptyList()
    val seen = HashSet<String>()
    return activities
        .asSequence()
        .filter { context.canOpen(it) }
        .mapNotNull { info ->
            val className = info.name.substringAfterLast('.').substringAfterLast('$')
            val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault("").ifBlank { className }
            if (!seen.add("$label|$className")) return@mapNotNull null
            HiddenScreen(
                label = label,
                detail = className,
                intent = Intent(Intent.ACTION_MAIN)
                    .setComponent(ComponentName(info.packageName, info.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, HiddenScreen::label).thenBy(HiddenScreen::detail))
        .toList()
}

private fun Context.open(screen: HiddenScreen) {
    try {
        startActivity(screen.intent)
    } catch (_: Exception) {
        // The phone maker blocked this screen, or it needs extra data to open.
        Toast.makeText(this, "Your phone does not allow opening ${screen.label} from here", Toast.LENGTH_SHORT).show()
    }
}

/** Pins a shortcut to this screen on the Home screen, with the Settings app's icon. */
private fun Context.pinToHome(screen: HiddenScreen) {
    val shortcutManager = getSystemService(ShortcutManager::class.java)
    if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) {
        Toast.makeText(this, "Adding shortcuts is not supported here", Toast.LENGTH_SHORT).show()
        return
    }
    val target = screen.intent.component?.flattenToShortString() ?: screen.intent.action ?: screen.label
    val info = ShortcutInfo.Builder(this, "rumb_hidden_" + Integer.toHexString(target.hashCode()))
        .setShortLabel(screen.label.take(24))
        .setLongLabel(screen.label.take(48))
        .setIcon(settingsIcon(screen))
        .setIntent(Intent(screen.intent))
        .build()
    try {
        shortcutManager.requestPinShortcut(info, null)
    } catch (_: Exception) {
        Toast.makeText(this, "Could not add ${screen.label} to Home", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.settingsIcon(screen: HiddenScreen): Icon {
    val pkg = screen.intent.component?.packageName
        ?: packageManager.resolve(screen.intent)?.packageName
        ?: SETTINGS
    return try {
        val drawable = packageManager.getApplicationIcon(pkg)
        val size = (48 * resources.displayMetrics.density).toInt().coerceAtLeast(96)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        Icon.createWithBitmap(bitmap)
    } catch (_: Exception) {
        Icon.createWithResource(this, com.android.launcher3.R.drawable.rumb_ic_hidden_settings)
    }
}

@Composable
private fun HiddenScreenRow(item: HiddenScreen) {
    val context = LocalContext.current
    PreferenceTemplate(
        title = { Text(text = item.label) },
        description = { Text(text = item.detail) },
        onClick = { context.open(item) },
        endWidget = {
            IconButton(onClick = { context.pinToHome(item) }) {
                MaterialIcon(imageVector = Icons.Rounded.Add, contentDescription = "Add ${item.label} to Home")
            }
        },
    )
}

private data class HiddenSettingsData(
    val shortcuts: List<HiddenScreen> = emptyList(),
    val all: List<HiddenScreen> = emptyList(),
    val loading: Boolean = true,
)

@Composable
fun RumbHiddenSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val data by produceState(HiddenSettingsData(), context) {
        value = withContext(Dispatchers.IO) {
            HiddenSettingsData(loadShortcuts(context), loadAllScreens(context), loading = false)
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(data, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            data.all
        } else {
            data.all.filter { it.label.contains(q, ignoreCase = true) || it.detail.contains(q, ignoreCase = true) }
        }
    }

    PreferenceLayoutLazyColumn(
        label = "Hidden settings",
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        item {
            Text(
                text = "Screens from Android's Settings that are hard to find or have no menu entry. " +
                    "Tap to open, or tap + to add one to your Home screen. " +
                    "Rumb only lists what your phone lets it open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }
        if (data.shortcuts.isNotEmpty()) {
            preferenceGroupItems(
                items = data.shortcuts,
                isFirstChild = true,
                heading = { "Shortcuts" },
                key = { _, item -> "s:" + item.label },
            ) { _, item ->
                HiddenScreenRow(item)
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search all ${data.all.size} Settings screens") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
        }
        when {
            data.loading -> item {
                Text(
                    text = "Loading…",
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                )
            }
            filtered.isEmpty() -> item {
                Text(
                    text = if (query.isBlank()) "Your phone's Settings app shares no extra screens." else "No screen matches \"$query\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                )
            }
            else -> preferenceGroupItems(
                items = filtered,
                isFirstChild = false,
                heading = { "All Settings screens" },
                key = { _, item -> "a:" + item.intent.component?.className + "|" + item.label },
            ) { _, item ->
                HiddenScreenRow(item)
            }
        }
    }
}
