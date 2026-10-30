package com.rumb.launcher.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.rumb.launcher.domain.InstalledApp

class SystemActions(context: Context) {
    private val appContext = context.applicationContext

    fun openAppInfo(app: InstalledApp) = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${app.packageName}")),
    )

    fun requestUninstall(app: InstalledApp) = start(
        Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:${app.packageName}")),
    )

    fun openWallpaperPicker() = start(Intent(Intent.ACTION_SET_WALLPAPER))
    fun openHomeSettings() = start(Intent(Settings.ACTION_HOME_SETTINGS))
    fun openNotificationAccess() = start(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))

    fun toast(message: String) = Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()

    private fun start(intent: Intent) {
        try {
            appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            toast("This action is not available on this device")
        } catch (_: SecurityException) {
            toast("Android blocked this action")
        }
    }
}
