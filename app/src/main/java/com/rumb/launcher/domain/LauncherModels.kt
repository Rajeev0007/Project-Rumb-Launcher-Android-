package com.rumb.launcher.domain

data class AppId(val component: String, val profileSerial: Long) {
    val stableKey: String = "$profileSerial|$component"

    companion object {
        fun parse(value: String): AppId? {
            val separator = value.indexOf('|')
            if (separator <= 0) return null
            val serial = value.substring(0, separator).toLongOrNull() ?: return null
            val component = value.substring(separator + 1)
            return component.takeIf { it.contains('/') }?.let { AppId(it, serial) }
        }
    }
}

data class InstalledApp(
    val id: AppId,
    val label: String,
    val packageName: String,
    val activityName: String,
    val profileLabel: String? = null,
    val isWorkProfile: Boolean = false,
)

data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val profileSerial: Long,
)

enum class ThemePreference { SYSTEM, LIGHT, DARK, AMOLED }
enum class IconShape { SYSTEM, CIRCLE, ROUNDED, SQUIRCLE }
enum class ClockStyle { LARGE, COMPACT, MINIMAL }
enum class GestureAction { NONE, OPEN_DRAWER, OPEN_SETTINGS, OPEN_NOTIFICATIONS, LOCK_LAYOUT }
enum class HomeItemType { APP, FOLDER, WIDGET }

data class HomeItem(
    val id: String,
    val type: HomeItemType,
    val page: Int,
    val cell: Int,
    val title: String = "",
    val appIds: List<AppId> = emptyList(),
    val widgetId: Int? = null,
    val widgetProvider: String? = null,
)

data class LauncherSettings(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val dynamicColor: Boolean = true,
    val iconShape: IconShape = IconShape.SYSTEM,
    val themedIcons: Boolean = false,
    val columns: Int = 5,
    val rows: Int = 5,
    val iconSize: Int = 54,
    val pageCount: Int = 1,
    val layoutLocked: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.LARGE,
    val showDate: Boolean = true,
    val showSeconds: Boolean = false,
    val notificationDots: Boolean = false,
    val swipeUpAction: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDownAction: GestureAction = GestureAction.NONE,
    val doubleTapAction: GestureAction = GestureAction.LOCK_LAYOUT,
    val hiddenApps: Set<AppId> = emptySet(),
    val homeItems: List<HomeItem> = emptyList(),
    val dockApps: List<AppId> = emptyList(),
) {
    companion object { const val CURRENT_SCHEMA = 2 }
}

data class CatalogState(
    val apps: List<InstalledApp> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
)
