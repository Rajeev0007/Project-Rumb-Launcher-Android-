package com.rumb.launcher.data

import android.content.Context
import android.os.Process
import android.os.UserManager
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class LauncherStateRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val currentProfileSerial = userManager
        ?.getSerialNumberForUser(Process.myUserHandle())?.takeIf { it >= 0 } ?: 0L
    private val mutableState = MutableStateFlow(load())
    val state: StateFlow<LauncherSettings> = mutableState.asStateFlow()

    @Synchronized
    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val normalized = normalize(transform(mutableState.value))
        persist(normalized)
        mutableState.value = normalized
    }

    fun exportJson(): String = encode(
        mutableState.value.copy(
            homeItems = mutableState.value.homeItems.filterNot { it.type == HomeItemType.WIDGET },
        ),
        includeDeviceBindings = false,
        portableIds = true,
    ).toString(2)

    @Throws(IllegalArgumentException::class)
    fun importJson(raw: String) {
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_IMPORT_BYTES) {
            throw IllegalArgumentException("Backup is larger than 1 MB")
        }
        val parsed = try {
            decode(JSONObject(raw), allowWidgets = false)
        } catch (error: JSONException) {
            throw IllegalArgumentException("Backup is not valid Rumb Launcher JSON", error)
        }
        update { parsed }
    }

    private fun load(): LauncherSettings {
        val json = preferences.getString(KEY_STATE, null)
        if (json != null) {
            runCatching { decode(JSONObject(json), allowWidgets = false) }.getOrNull()?.let { return normalize(it) }
        }
        return migrateLegacy()
    }

    private fun migrateLegacy(): LauncherSettings {
        val favorites = preferences.getStringSet(KEY_LEGACY_FAVORITES, emptySet()).orEmpty()
        val legacyColumns = preferences.getInt(KEY_LEGACY_COLUMNS, 5).coerceIn(4, 6)
        val legacyCapacity = legacyColumns * 5
        val items = favorites.sorted().mapIndexedNotNull { index, component ->
            component.takeIf { it.contains('/') }?.let {
                HomeItem(
                    id = "app-${currentProfileSerial}-${component.hashCode()}",
                    type = HomeItemType.APP,
                    page = index / legacyCapacity,
                    cell = index % legacyCapacity,
                    appIds = listOf(AppId(component, currentProfileSerial)),
                )
            }
        }
        val legacyTheme = runCatching {
            ThemePreference.valueOf(preferences.getString(KEY_LEGACY_THEME, "SYSTEM").orEmpty())
        }.getOrDefault(ThemePreference.SYSTEM)
        val migrated = normalize(
            LauncherSettings(
                theme = legacyTheme,
                columns = legacyColumns,
                iconSize = preferences.getInt(KEY_LEGACY_ICON_SIZE, 52),
                pageCount = (items.maxOfOrNull { it.page } ?: 0) + 1,
                homeItems = items,
            ),
        )
        persist(migrated)
        return migrated
    }

    private fun persist(value: LauncherSettings) {
        preferences.edit().putString(
            KEY_STATE,
            encode(value, includeDeviceBindings = true, portableIds = false).toString(),
        ).apply()
    }

    private fun normalize(value: LauncherSettings): LauncherSettings {
        val columns = value.columns.coerceIn(4, 8)
        val rows = value.rows.coerceIn(4, 8)
        val pages = value.pageCount.coerceIn(1, 9)
        val capacity = columns * rows
        val occupied = mutableSetOf<Pair<Int, Int>>()
        val items = value.homeItems
            .asSequence()
            .filter { it.page in 0 until pages && it.cell in 0 until capacity }
            .filter { it.type == HomeItemType.WIDGET || it.appIds.isNotEmpty() }
            .filter { occupied.add(it.page to it.cell) }
            .map { item ->
                item.copy(
                    title = item.title.take(48),
                    appIds = item.appIds.distinctBy(AppId::stableKey).take(24),
                )
            }
            .toList()
        return value.copy(
            schemaVersion = LauncherSettings.CURRENT_SCHEMA,
            columns = columns,
            rows = rows,
            iconSize = value.iconSize.coerceIn(42, 72),
            pageCount = pages,
            hiddenApps = value.hiddenApps.take(1000).toSet(),
            homeItems = items,
            dockApps = value.dockApps.distinctBy(AppId::stableKey).take(5),
        )
    }

    private fun encode(
        value: LauncherSettings,
        includeDeviceBindings: Boolean,
        portableIds: Boolean,
    ): JSONObject = JSONObject().apply {
        put("format", FORMAT)
        put("version", LauncherSettings.CURRENT_SCHEMA)
        put("theme", value.theme.name)
        put("dynamicColor", value.dynamicColor)
        put("iconShape", value.iconShape.name)
        put("themedIcons", value.themedIcons)
        put("columns", value.columns)
        put("rows", value.rows)
        put("iconSize", value.iconSize)
        put("pageCount", value.pageCount)
        put("layoutLocked", value.layoutLocked)
        put("clockStyle", value.clockStyle.name)
        put("showDate", value.showDate)
        put("showSeconds", value.showSeconds)
        put("notificationDots", value.notificationDots)
        put("swipeUpAction", value.swipeUpAction.name)
        put("swipeDownAction", value.swipeDownAction.name)
        put("doubleTapAction", value.doubleTapAction.name)
        put("hiddenApps", JSONArray(value.hiddenApps.map { encodeId(it, portableIds) }))
        put("dockApps", JSONArray(value.dockApps.map { encodeId(it, portableIds) }))
        put("homeItems", JSONArray().apply {
            value.homeItems.forEach { item ->
                if (includeDeviceBindings || item.type != HomeItemType.WIDGET) put(JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type.name)
                    put("page", item.page)
                    put("cell", item.cell)
                    put("title", item.title)
                    put("apps", JSONArray(item.appIds.map { encodeId(it, portableIds) }))
                    if (includeDeviceBindings) item.widgetId?.let { put("widgetId", it) }
                    item.widgetProvider?.let { put("widgetProvider", it) }
                })
            }
        })
    }

    private fun decode(json: JSONObject, allowWidgets: Boolean): LauncherSettings {
        if (json.optString("format") != FORMAT) throw IllegalArgumentException("Not a Rumb Launcher backup")
        val version = json.optInt("version", -1)
        if (version !in 1..LauncherSettings.CURRENT_SCHEMA) throw IllegalArgumentException("Unsupported backup version")
        fun enum(name: String, fallback: String): String = json.optString(name, fallback)
        fun ids(name: String): Set<AppId> = json.optJSONArray(name).toStrings().mapNotNull(::parseStoredId).toSet()
        val homeItems = json.optJSONArray("homeItems").objects().mapNotNull { item ->
            val type = runCatching { HomeItemType.valueOf(item.optString("type")) }.getOrNull() ?: return@mapNotNull null
            if (type == HomeItemType.WIDGET && !allowWidgets) return@mapNotNull null
            HomeItem(
                id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null,
                type = type,
                page = item.optInt("page", 0),
                cell = item.optInt("cell", 0),
                title = item.optString("title"),
                appIds = item.optJSONArray("apps").toStrings().mapNotNull(::parseStoredId),
                widgetId = item.optInt("widgetId", -1).takeIf { it >= 0 && allowWidgets },
                widgetProvider = item.optString("widgetProvider").takeIf(String::isNotBlank),
            )
        }
        return normalize(
            LauncherSettings(
                theme = enumValue(enum("theme", "SYSTEM"), ThemePreference.SYSTEM),
                dynamicColor = json.optBoolean("dynamicColor", true),
                iconShape = enumValue(enum("iconShape", "SYSTEM"), IconShape.SYSTEM),
                themedIcons = json.optBoolean("themedIcons", false),
                columns = json.optInt("columns", 5),
                rows = json.optInt("rows", 5),
                iconSize = json.optInt("iconSize", 54),
                pageCount = json.optInt("pageCount", 1),
                layoutLocked = json.optBoolean("layoutLocked", false),
                clockStyle = enumValue(enum("clockStyle", "LARGE"), ClockStyle.LARGE),
                showDate = json.optBoolean("showDate", true),
                showSeconds = json.optBoolean("showSeconds", false),
                notificationDots = json.optBoolean("notificationDots", false),
                swipeUpAction = enumValue(enum("swipeUpAction", "OPEN_DRAWER"), GestureAction.OPEN_DRAWER),
                swipeDownAction = enumValue(enum("swipeDownAction", "NONE"), GestureAction.NONE),
                doubleTapAction = enumValue(enum("doubleTapAction", "LOCK_LAYOUT"), GestureAction.LOCK_LAYOUT),
                hiddenApps = ids("hiddenApps"),
                dockApps = json.optJSONArray("dockApps").toStrings().mapNotNull(::parseStoredId),
                homeItems = homeItems,
            ),
        )
    }

    private fun encodeId(id: AppId, portable: Boolean): String {
        if (!portable) return id.stableKey
        if (id.profileSerial == currentProfileSerial) return "personal|${id.component}"
        val profileIndex = secondaryProfileSerials().indexOf(id.profileSerial)
        return if (profileIndex >= 0) "profile$profileIndex|${id.component}" else "unavailable|${id.component}"
    }

    private fun parseStoredId(value: String): AppId? {
        AppId.parse(value)?.let { return it }
        val separator = value.indexOf('|')
        if (separator <= 0) return null
        val role = value.substring(0, separator)
        val component = value.substring(separator + 1).takeIf { it.contains('/') } ?: return null
        val serial = when {
            role == "personal" -> currentProfileSerial
            role.startsWith("profile") -> role.removePrefix("profile").toIntOrNull()
                ?.let { secondaryProfileSerials().getOrNull(it) }
            else -> null
        } ?: return null
        return AppId(component, serial)
    }

    private fun secondaryProfileSerials(): List<Long> = userManager?.userProfiles.orEmpty()
        .mapNotNull { userManager?.getSerialNumberForUser(it)?.takeIf { serial -> serial >= 0 } }
        .filter { it != currentProfileSerial }
        .sorted()

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private fun JSONArray?.toStrings(): List<String> = if (this == null) emptyList() else buildList {
        for (index in 0 until length()) optString(index).takeIf(String::isNotBlank)?.let(::add)
    }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(::add)
    }

    companion object {
        private const val PREFERENCES_NAME = "launcher_preferences"
        private const val KEY_STATE = "state_v2_json"
        private const val KEY_LEGACY_FAVORITES = "favorites"
        private const val KEY_LEGACY_COLUMNS = "columns"
        private const val KEY_LEGACY_ICON_SIZE = "icon_size"
        private const val KEY_LEGACY_THEME = "theme"
        private const val FORMAT = "com.rumb.launcher.layout"
        private const val MAX_IMPORT_BYTES = 1_048_576
    }
}
