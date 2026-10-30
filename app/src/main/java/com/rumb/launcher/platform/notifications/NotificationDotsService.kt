package com.rumb.launcher.platform.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationDotStore {
    private val mutableCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = mutableCounts.asStateFlow()

    fun replacePackages(packages: List<String>) {
        mutableCounts.value = packages.groupingBy { it }.eachCount()
    }

    fun clear() { mutableCounts.value = emptyMap() }

    fun hasAccess(context: Context): Boolean {
        val component = ComponentName(context, NotificationDotsService::class.java).flattenToString()
        return Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.split(':')?.any { it == component } == true
    }
}

class NotificationDotsService : NotificationListenerService() {
    override fun onListenerConnected() = updateCounts()

    override fun onListenerDisconnected() {
        NotificationDotStore.clear()
        requestRebind(ComponentName(this, NotificationDotsService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = updateCounts()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = updateCounts()
    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = updateCounts()

    private fun updateCounts() {
        val packages = runCatching {
            val rankingMap = currentRanking
            activeNotifications.orEmpty().mapNotNull { notification ->
                if (!notification.isClearable ||
                    notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
                ) {
                    return@mapNotNull null
                }
                val ranking = Ranking()
                if (!rankingMap.getRanking(notification.key, ranking) || !ranking.canShowBadge()) {
                    return@mapNotNull null
                }
                notification.packageName
            }
        }.getOrDefault(emptyList())
        NotificationDotStore.replacePackages(packages)
    }

    override fun onDestroy() {
        NotificationDotStore.clear()
        super.onDestroy()
    }
}
