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
    val isSystemApp: Boolean = false,
) {
    /** Matches notification badges, which Android reports per package and profile. */
    val badgeKey: String get() = notificationBadgeKey(id.profileSerial, packageName)
}

fun notificationBadgeKey(profileSerial: Long, packageName: String): String = "$profileSerial|$packageName"

data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val profileSerial: Long,
)

enum class ThemePreference { SYSTEM, LIGHT, DARK, AMOLED }
enum class VisualStyle { AURA_GLASS, SOFT_PANEL, CRYSTAL_DOCK, PURE_MINIMAL }
enum class IconShape { SYSTEM, CIRCLE, ROUNDED, SQUIRCLE }
enum class ClockStyle { HIDDEN, LARGE, COMPACT, MINIMAL }
enum class ClockAlignment { LEFT, CENTER, RIGHT }
enum class ClockTimeFormat { SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR }
enum class ClockFont { SANS, SERIF, MONOSPACE }
enum class GestureAction { NONE, OPEN_DRAWER, OPEN_SEARCH, OPEN_QUICK_PANEL, OPEN_SETTINGS, LOCK_LAYOUT, LOCK_SCREEN }
enum class HomeItemType { APP, FOLDER, WIDGET }

data class HomeItem(
    val id: String,
    val type: HomeItemType,
    val page: Int,
    val cell: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val title: String = "",
    val appIds: List<AppId> = emptyList(),
    val widgetId: Int? = null,
    val widgetProvider: String? = null,
)

data class LauncherSettings(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val visualStyle: VisualStyle = VisualStyle.AURA_GLASS,
    val dynamicColor: Boolean = true,
    val iconShape: IconShape = IconShape.SYSTEM,
    val themedIcons: Boolean = false,
    val columns: Int = 5,
    val rows: Int = 5,
    val iconSize: Int = 54,
    val pageCount: Int = 1,
    val layoutLocked: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.LARGE,
    val clockAlignment: ClockAlignment = ClockAlignment.LEFT,
    val clockTimeFormat: ClockTimeFormat = ClockTimeFormat.SYSTEM,
    val clockFont: ClockFont = ClockFont.SANS,
    val clockSize: Int = 58,
    val showDate: Boolean = true,
    val showSeconds: Boolean = false,
    val swipeUpAction: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDownAction: GestureAction = GestureAction.NONE,
    val doubleTapAction: GestureAction = GestureAction.LOCK_LAYOUT,
    val pinchAction: GestureAction = GestureAction.NONE,
    val hiddenApps: Set<AppId> = emptySet(),
    val homeItems: List<HomeItem> = emptyList(),
    val dockApps: List<AppId> = emptyList(),
    val iconPack: String? = null,
    val showHomeLabels: Boolean = true,
    val homeSearchBar: Boolean = false,
    val drawerSuggestions: Boolean = true,
    val drawerAutoKeyboard: Boolean = false,
    val haptics: Boolean = true,
    val customLabels: Map<AppId, String> = emptyMap(),
    val notificationDots: Boolean = false,
) {
    companion object { const val CURRENT_SCHEMA = 7 }
}

/** Everything that changes how an app icon bitmap is produced. Used as a cache and recomposition key. */
data class IconAppearance(
    val shape: IconShape = IconShape.SYSTEM,
    val themed: Boolean = false,
    val iconPack: String? = null,
)

val LauncherSettings.iconAppearance: IconAppearance
    get() = IconAppearance(iconShape, themedIcons, iconPack)

data class CatalogState(
    val apps: List<InstalledApp> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
)
