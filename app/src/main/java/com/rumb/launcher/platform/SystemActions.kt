package com.rumb.launcher.platform

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.rumb.launcher.domain.InstalledApp

class SystemActions(context: Context) {
    private val appContext = context.applicationContext

    fun openAppInfo(app: InstalledApp) = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${app.packageName}")),
    )

    fun openInternetSettings() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        start(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    } else {
        start(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openBluetoothSettings() = start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openDisplaySettings() = start(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    fun openSoundSettings() = start(Intent(Settings.ACTION_SOUND_SETTINGS))
    fun openBatterySettings() = start(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun openWallpaperPicker() = start(Intent(Intent.ACTION_SET_WALLPAPER))
    fun openHomeSettings() = start(Intent(Settings.ACTION_HOME_SETTINGS))

    /** Hands the query to the user's search or browser app. Rumb itself never goes online. */
    fun webSearch(query: String) {
        val text = query.trim().take(200)
        if (text.isEmpty()) return
        start(
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(text)}"))
                .addCategory(Intent.CATEGORY_BROWSABLE),
        )
    }

    fun storeSearch(query: String) {
        val text = query.trim().take(200)
        if (text.isEmpty()) return
        start(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(text)}&c=apps")))
    }

    /** Asks Android's package installer to uninstall; Android shows its own confirmation. */
    fun uninstall(packageName: String) = start(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))

    fun openNotificationAccess(component: ComponentName) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        start(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString()),
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        )
    } else {
        start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAccessibilitySettings() = start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun toast(message: String) = Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()

    private fun start(intent: Intent, fallback: Intent? = null) {
        val target = when {
            intent.resolveActivity(appContext.packageManager) != null -> intent
            fallback?.resolveActivity(appContext.packageManager) != null -> fallback
            else -> {
                toast("This action is not available on this device")
                return
            }
        }
        try {
            appContext.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            toast("This action is not available on this device")
        } catch (_: SecurityException) {
            toast("Android blocked this action")
        } catch (_: RuntimeException) {
            toast("This settings screen could not be opened")
        }
    }
}
