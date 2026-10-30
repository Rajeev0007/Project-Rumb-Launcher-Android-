package com.rumb.launcher.platform.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.rumb.launcher.domain.notificationBadgeKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide set of badge keys (profile serial + package) that currently have a dot-worthy notification. */
object NotificationBadgeStore {
    private val mutableBadges = MutableStateFlow<Set<String>>(emptySet())
    val badges: StateFlow<Set<String>> = mutableBadges.asStateFlow()

    internal fun publish(keys: Set<String>) {
        if (mutableBadges.value != keys) mutableBadges.value = keys
    }
}

/**
 * Supplies notification dots. Only the package, profile, flags, and the system "show badge" ranking
 * are read. Titles, text, extras, and actions are never accessed or stored. The component is disabled
 * in the manifest and enabled only when the user turns on notification dots.
 */
class RumbNotificationListener : NotificationListenerService() {
    private val userManager by lazy { getSystemService(UserManager::class.java) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationBadgeStore.publish(emptySet())
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRankingUpdate(rankingMap: NotificationListenerService.RankingMap?) = refresh()

    private fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val rankingMap = runCatching { currentRanking }.getOrNull()
        val ranking = NotificationListenerService.Ranking()
        val keys = HashSet<String>()
        for (sbn in active) {
            val flags = sbn.notification?.flags ?: continue
            if (flags and Notification.FLAG_GROUP_SUMMARY != 0) continue
            if (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0) continue
            if (rankingMap != null && rankingMap.getRanking(sbn.key, ranking) && !ranking.canShowBadge()) continue
            val serial = userManager?.getSerialNumberForUser(sbn.user)?.takeIf { it >= 0 } ?: continue
            keys += notificationBadgeKey(serial, sbn.packageName)
        }
        NotificationBadgeStore.publish(keys)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, RumbNotificationListener::class.java)

        fun hasAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Enables the listener only while dots are on, so Android never binds it otherwise. */
        fun setEnabled(context: Context, enabled: Boolean) {
            val packageManager = context.packageManager
            val component = component(context)
            val state = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            if (packageManager.getComponentEnabledSetting(component) == state) return
            runCatching { packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) }
            if (enabled) runCatching { NotificationListenerService.requestRebind(component) } else NotificationBadgeStore.publish(emptySet())
        }
    }
}
