/*
 * Copyright 2026, Rajeev0007 (Rumb Launcher)
 *
 * Licensed under the GNU General Public License, version 3 or later.
 */

package app.lawnchair.rumb

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.AlarmClock
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.android.launcher3.R
import java.util.Calendar

private const val PREFS = "rumb_widgets"
private const val KEY_TORCH = "torch_on"

private fun Context.activityIntent(requestCode: Int, intent: Intent): PendingIntent = PendingIntent.getActivity(
    this,
    requestCode,
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private fun Context.broadcastIntent(requestCode: Int, intent: Intent): PendingIntent = PendingIntent.getBroadcast(
    this,
    requestCode,
    intent,
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/** The first intent this phone can open, or the last one as a fallback. */
private fun Context.firstResolvable(vararg intents: Intent): Intent =
    intents.firstOrNull { it.resolveActivity(packageManager) != null } ?: intents.last()

private fun Context.widgetIds(provider: Class<out AppWidgetProvider>): IntArray =
    AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, provider))

// ---------------------------------------------------------------------------------------
// Clocks: the layouts use TextClock, so they tick without any updates from Rumb.
// ---------------------------------------------------------------------------------------

abstract class RumbClockWidgetBase(private val layout: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, layout)
        val clockApp = context.firstResolvable(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(Settings.ACTION_DATE_SETTINGS),
        )
        views.setOnClickPendingIntent(R.id.rumb_widget_root, context.activityIntent(layout, clockApp))
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}

/** Minimal clock: a large weekday, then the time and date. */
class RumbDayClockWidget : RumbClockWidgetBase(R.layout.rumb_widget_day_clock)

/** iPhone-style glass card with a large time and the date. */
class RumbGlassClockWidget : RumbClockWidgetBase(R.layout.rumb_widget_glass_clock)

// ---------------------------------------------------------------------------------------
// Control center: iOS-style tiles. The torch toggles in place; the others open the
// matching Android panel or settings screen.
// ---------------------------------------------------------------------------------------

class RumbControlCenterWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        watchTorch(context)
        appWidgetManager.updateAppWidget(appWidgetIds, build(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_TORCH) {
            watchTorch(context)
            toggleTorch(context)
            refresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_TOGGLE_TORCH = "app.lawnchair.rumb.TOGGLE_TORCH"

        @Volatile private var torchWatched = false

        fun refresh(context: Context) {
            val ids = context.widgetIds(RumbControlCenterWidget::class.java)
            if (ids.isNotEmpty()) AppWidgetManager.getInstance(context).updateAppWidget(ids, build(context))
        }

        private fun flashCameraId(cameraManager: CameraManager): String? = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        /**
         * Keeps the tile in sync when the torch is switched from Quick Settings or another app.
         * Android reports the current state as soon as the callback is registered.
         */
        private fun watchTorch(context: Context) {
            if (torchWatched) return
            val app = context.applicationContext
            try {
                val cameraManager = app.getSystemService(CameraManager::class.java) ?: return
                val flashId = flashCameraId(cameraManager) ?: return
                cameraManager.registerTorchCallback(
                    object : CameraManager.TorchCallback() {
                        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                            if (cameraId != flashId) return
                            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            if (prefs.getBoolean(KEY_TORCH, false) != enabled) {
                                prefs.edit().putBoolean(KEY_TORCH, enabled).apply()
                                refresh(app)
                            }
                        }
                    },
                    Handler(Looper.getMainLooper()),
                )
                torchWatched = true
            } catch (_: Exception) {
                // No camera service or no flash: the torch tile simply does nothing.
            }
        }

        private fun toggleTorch(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val target = !prefs.getBoolean(KEY_TORCH, false)
            try {
                val cameraManager = context.getSystemService(CameraManager::class.java)
                val flashId = if (cameraManager == null) null else flashCameraId(cameraManager)
                if (cameraManager == null || flashId == null) {
                    Toast.makeText(context, "This phone has no flashlight", Toast.LENGTH_SHORT).show()
                    return
                }
                cameraManager.setTorchMode(flashId, target)
                prefs.edit().putBoolean(KEY_TORCH, target).apply()
            } catch (_: Exception) {
                Toast.makeText(context, "The flashlight is being used by another app", Toast.LENGTH_SHORT).show()
            }
        }

        private fun build(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.rumb_widget_control_center)
            val panels = Build.VERSION.SDK_INT >= 29
            val tiles = listOf(
                R.id.rumb_tile_wifi to
                    (if (panels) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS)),
                R.id.rumb_tile_internet to
                    (if (panels) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_WIRELESS_SETTINGS)),
                R.id.rumb_tile_bluetooth to Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                R.id.rumb_tile_volume to
                    (if (panels) Intent(Settings.Panel.ACTION_VOLUME) else Intent(Settings.ACTION_SOUND_SETTINGS)),
                R.id.rumb_tile_display to Intent(Settings.ACTION_DISPLAY_SETTINGS),
                R.id.rumb_tile_battery to context.firstResolvable(
                    Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                ),
                R.id.rumb_tile_hidden to Intent(Intent.ACTION_VIEW, Uri.parse("lawnchair://settings/hidden_settings"))
                    .setPackage(context.packageName),
            )
            tiles.forEachIndexed { index, (id, intent) ->
                views.setOnClickPendingIntent(id, context.activityIntent(100 + index, intent))
            }
            views.setOnClickPendingIntent(
                R.id.rumb_tile_torch,
                context.broadcastIntent(
                    200,
                    Intent(ACTION_TOGGLE_TORCH).setClass(context, RumbControlCenterWidget::class.java),
                ),
            )
            val torchOn = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_TORCH, false)
            views.setInt(
                R.id.rumb_tile_torch_icon,
                "setBackgroundResource",
                if (torchOn) R.drawable.rumb_tile_bg_on else R.drawable.rumb_tile_bg,
            )
            views.setInt(R.id.rumb_tile_torch_icon, "setColorFilter", if (torchOn) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt())
            return views
        }
    }
}

// ---------------------------------------------------------------------------------------
// Screen time: today's total phone use and the top three apps, from Android's usage stats.
// Uses the usage access that Rumb already asks for to suggest apps.
// ---------------------------------------------------------------------------------------

class RumbScreenTimeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                appWidgetManager.updateAppWidget(appWidgetIds, build(context.applicationContext, appWidgetIds))
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun build(context: Context, ids: IntArray): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.rumb_widget_screen_time)
        val refresh = context.broadcastIntent(
            300,
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setClass(context, RumbScreenTimeWidget::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
        )
        views.setOnClickPendingIntent(R.id.rumb_st_refresh, refresh)

        val rows = listOf(
            Triple(R.id.rumb_st_row1, R.id.rumb_st_name1, R.id.rumb_st_time1),
            Triple(R.id.rumb_st_row2, R.id.rumb_st_name2, R.id.rumb_st_time2),
            Triple(R.id.rumb_st_row3, R.id.rumb_st_name3, R.id.rumb_st_time3),
        )
        if (!context.hasUsageAccess()) {
            views.setTextViewText(R.id.rumb_st_total, "—")
            views.setTextViewText(R.id.rumb_st_hint, "Tap to allow usage access for Rumb, then tap ↻")
            views.setViewVisibility(R.id.rumb_st_hint, View.VISIBLE)
            rows.forEach { (row, _, _) -> views.setViewVisibility(row, View.GONE) }
            views.setOnClickPendingIntent(
                R.id.rumb_widget_root,
                context.activityIntent(301, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
            )
            return views
        }

        val usage = try {
            usageToday(context)
        } catch (_: Exception) {
            emptyList()
        }
        views.setTextViewText(R.id.rumb_st_total, formatDuration(usage.sumOf { it.second }))
        views.setViewVisibility(R.id.rumb_st_hint, if (usage.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.rumb_st_hint, "No app use yet today")
        rows.forEachIndexed { index, (row, name, time) ->
            val entry = usage.getOrNull(index)
            views.setViewVisibility(row, if (entry == null) View.GONE else View.VISIBLE)
            if (entry != null) {
                views.setTextViewText(name, context.appLabel(entry.first))
                views.setTextViewText(time, formatDuration(entry.second))
            }
        }
        views.setOnClickPendingIntent(R.id.rumb_widget_root, refresh)
        return views
    }

    private fun Context.hasUsageAccess(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** Foreground time per app since midnight, longest first, without launchers and System UI. */
    private fun usageToday(context: Context): List<Pair<String, Long>> {
        val usageStats = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(start, now) ?: return emptyList()
        val openSince = HashMap<String, Long>()
        val totals = HashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val key = pkg + "/" + event.className
            when (event.eventType) {
                // Same values as MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND on Android 9 and below.
                UsageEvents.Event.ACTIVITY_RESUMED -> openSince[key] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED ->
                    openSince.remove(key)?.let { totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - it) }
            }
        }
        openSince.forEach { (key, since) ->
            val pkg = key.substringBefore('/')
            totals[pkg] = (totals[pkg] ?: 0L) + (now - since)
        }
        val ignored = HashSet<String>().apply {
            add(context.packageName)
            add("com.android.systemui")
            context.packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .forEach { add(it.activityInfo.packageName) }
        }
        return totals.filter { (pkg, time) -> pkg !in ignored && time >= 60_000L }
            .toList()
            .sortedByDescending { it.second }
    }

    private fun Context.appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }

    private fun formatDuration(millis: Long): String {
        val minutes = millis / 60_000L
        return when {
            minutes < 1 -> "0m"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
