package com.rumb.launcher.data

import android.content.Context
import android.os.Process
import android.os.UserManager
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.ClockAlignment
import com.rumb.launcher.domain.ClockFont
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.ClockTimeFormat
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.ThemePreference
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.domain.normalizeHomePlacements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingLock = Any()
    private val writeLock = Any()
    private var pending: LauncherSettings? = null
    private var persistJob: Job? = null

    /**
     * Applies [transform] and publishes the result immediately. JSON encoding and the disk write
     * happen off the main thread, coalesced so rapid changes (drags, sliders) write once.
     */
    @Synchronized
    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val current = mutableState.value
        val transformed = transform(current)
        if (transformed === current) return
        val normalized = normalize(transformed)
        mutableState.value = normalized
        schedulePersist(normalized)
    }

    /** Writes any pending state now. Call when the launcher stops so nothing is lost. */
    fun flush() {
        persistJob?.cancel()
        writePending()
    }

    private fun schedulePersist(value: LauncherSettings) {
        synchronized(pendingLock) { pending = value }
        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DELAY_MS)
            writePending()
        }
    }

    private fun writePending() = synchronized(writeLock) {
        val value = synchronized(pendingLock) { pending.also { pending = null } } ?: return@synchronized
        persist(value)
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
        val json = try {
            JSONObject(raw)
        } catch (error: JSONException) {
            throw IllegalArgumentException("Backup is not valid Rumb Launcher JSON", error)
        }
        validatePortableLayout(json)
        val parsed = try {
            decode(json, allowWidgets = false)
        } catch (error: JSONException) {
            throw IllegalArgumentException("Backup is not valid Rumb Launcher JSON", error)
        }
        update { parsed }
        flush()
    }

    private fun load(): LauncherSettings {
        val json = preferences.getString(KEY_STATE, null)
        if (json != null) {
            runCatching { decode(JSONObject(json), allowWidgets = true) }.getOrNull()?.let { return normalize(it) }
        }
        val hasLegacyState = listOf(
            KEY_LEGACY_FAVORITES,
            KEY_LEGACY_COLUMNS,
            KEY_LEGACY_ICON_SIZE,
            KEY_LEGACY_THEME,
        ).any(preferences::contains)
        return if (hasLegacyState) migrateLegacy() else freshV3Defaults()
    }

    private fun freshV3Defaults(): LauncherSettings = normalize(
        LauncherSettings(
            clockStyle = ClockStyle.HIDDEN,
            swipeDownAction = GestureAction.OPEN_QUICK_PANEL,
            homeSearchBar = true,
        ),
    ).also(::persist)

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
        val sanitized = value.homeItems.mapNotNull { item ->
            val apps = item.appIds.distinctBy(AppId::stableKey).take(24)
            when (item.type) {
                HomeItemType.APP -> apps.firstOrNull()?.let {
                    item.copy(title = "", appIds = listOf(it), spanX = 1, spanY = 1, widgetId = null, widgetProvider = null)
                }
                HomeItemType.FOLDER -> when {
                    apps.isEmpty() -> null
                    apps.size == 1 -> item.copy(
                        type = HomeItemType.APP,
                        title = "",
                        appIds = apps,
                        spanX = 1,
                        spanY = 1,
                        widgetId = null,
                        widgetProvider = null,
                    )
                    else -> item.copy(
                        title = item.title.trim().take(48).ifBlank { "Folder" },
                        appIds = apps,
                        spanX = 1,
                        spanY = 1,
                        widgetId = null,
                        widgetProvider = null,
                    )
                }
                HomeItemType.WIDGET -> if (item.widgetId != null && item.widgetId >= 0 && !item.widgetProvider.isNullOrBlank()) {
                    item.copy(title = item.title.take(48), appIds = emptyList())
                } else null
            }
        }.distinctBy(HomeItem::id)
        val items = normalizeHomePlacements(
            source = sanitized,
            columns = columns,
            rows = rows,
            maxPages = 9,
        )
        val normalizedPages = maxOf(pages, (items.maxOfOrNull(HomeItem::page) ?: 0) + 1)
        return value.copy(
            schemaVersion = LauncherSettings.CURRENT_SCHEMA,
            columns = columns,
            rows = rows,
            iconSize = value.iconSize.coerceIn(42, 72),
            pageCount = normalizedPages,
            clockSize = value.clockSize.coerceIn(28, 72),
            hiddenApps = value.hiddenApps.take(1000).toSet(),
            homeItems = items,
            dockApps = value.dockApps.distinctBy(AppId::stableKey).take(5),
            iconPack = value.iconPack?.trim()?.takeIf { PACKAGE_NAME.matches(it) },
            customLabels = value.customLabels.entries.asSequence()
                .mapNotNull { (id, label) -> label.trim().take(MAX_LABEL_LENGTH).takeIf(String::isNotEmpty)?.let { id to it } }
                .take(1000)
                .toMap(),
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
        put("visualStyle", value.visualStyle.name)
        put("dynamicColor", value.dynamicColor)
        put("iconShape", value.iconShape.name)
        put("themedIcons", value.themedIcons)
        put("columns", value.columns)
        put("rows", value.rows)
        put("iconSize", value.iconSize)
        put("pageCount", value.pageCount)
        put("layoutLocked", value.layoutLocked)
        put("clockStyle", value.clockStyle.name)
        put("clockAlignment", value.clockAlignment.name)
        put("clockTimeFormat", value.clockTimeFormat.name)
        put("clockFont", value.clockFont.name)
        put("clockSize", value.clockSize)
        put("showDate", value.showDate)
        put("showSeconds", value.showSeconds)
        put("swipeUpAction", value.swipeUpAction.name)
        put("swipeDownAction", value.swipeDownAction.name)
        put("doubleTapAction", value.doubleTapAction.name)
        put("pinchAction", value.pinchAction.name)
        put("hiddenApps", JSONArray(value.hiddenApps.map { encodeId(it, portableIds) }))
        put("dockApps", JSONArray(value.dockApps.map { encodeId(it, portableIds) }))
        value.iconPack?.let { put("iconPack", it) }
        put("showHomeLabels", value.showHomeLabels)
        put("homeSearchBar", value.homeSearchBar)
        put("drawerSuggestions", value.drawerSuggestions)
        put("drawerAutoKeyboard", value.drawerAutoKeyboard)
        put("haptics", value.haptics)
        put("notificationDots", value.notificationDots)
        put("customLabels", JSONArray().apply {
            value.customLabels.forEach { (id, label) ->
                put(JSONObject().put("app", encodeId(id, portableIds)).put("label", label))
            }
        })
        put("homeItems", JSONArray().apply {
            value.homeItems.forEach { item ->
                if (includeDeviceBindings || item.type != HomeItemType.WIDGET) put(JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type.name)
                    put("page", item.page)
                    put("cell", item.cell)
                    put("spanX", item.spanX)
                    put("spanY", item.spanY)
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
                spanX = item.optInt("spanX", 1),
                spanY = item.optInt("spanY", 1),
                title = item.optString("title"),
                appIds = item.optJSONArray("apps").toStrings().mapNotNull(::parseStoredId),
                widgetId = item.optInt("widgetId", -1).takeIf { it >= 0 && allowWidgets },
                widgetProvider = item.optString("widgetProvider").takeIf(String::isNotBlank),
            )
        }
        return normalize(
            LauncherSettings(
                theme = enumValue(enum("theme", "SYSTEM"), ThemePreference.SYSTEM),
                visualStyle = enumValue(enum("visualStyle", "AURA_GLASS"), VisualStyle.AURA_GLASS),
                dynamicColor = json.optBoolean("dynamicColor", true),
                iconShape = enumValue(enum("iconShape", "SYSTEM"), IconShape.SYSTEM),
                themedIcons = json.optBoolean("themedIcons", false),
                columns = json.optInt("columns", 5),
                rows = json.optInt("rows", 5),
                iconSize = json.optInt("iconSize", 54),
                pageCount = json.optInt("pageCount", 1),
                layoutLocked = json.optBoolean("layoutLocked", false),
                clockStyle = enumValue(enum("clockStyle", "LARGE"), ClockStyle.LARGE),
                clockAlignment = enumValue(enum("clockAlignment", "LEFT"), ClockAlignment.LEFT),
                clockTimeFormat = enumValue(enum("clockTimeFormat", "SYSTEM"), ClockTimeFormat.SYSTEM),
                clockFont = enumValue(enum("clockFont", "SANS"), ClockFont.SANS),
                clockSize = json.optInt("clockSize", defaultClockSize(json)),
                showDate = json.optBoolean("showDate", true),
                showSeconds = json.optBoolean("showSeconds", false),
                swipeUpAction = enumValue(enum("swipeUpAction", "OPEN_DRAWER"), GestureAction.OPEN_DRAWER),
                swipeDownAction = enumValue(enum("swipeDownAction", "NONE"), GestureAction.NONE),
                doubleTapAction = enumValue(enum("doubleTapAction", "LOCK_LAYOUT"), GestureAction.LOCK_LAYOUT),
                pinchAction = enumValue(enum("pinchAction", "NONE"), GestureAction.NONE),
                hiddenApps = ids("hiddenApps"),
                dockApps = json.optJSONArray("dockApps").toStrings().mapNotNull(::parseStoredId),
                homeItems = homeItems,
                iconPack = json.optString("iconPack").takeIf(String::isNotBlank),
                showHomeLabels = json.optBoolean("showHomeLabels", true),
                // Existing layouts keep their look; fresh installs opt in to the search bar.
                homeSearchBar = json.optBoolean("homeSearchBar", false),
                drawerSuggestions = json.optBoolean("drawerSuggestions", true),
                drawerAutoKeyboard = json.optBoolean("drawerAutoKeyboard", false),
                haptics = json.optBoolean("haptics", true),
                notificationDots = json.optBoolean("notificationDots", false),
                customLabels = json.optJSONArray("customLabels").objects().mapNotNull { entry ->
                    val id = parseStoredId(entry.optString("app")) ?: return@mapNotNull null
                    entry.optString("label").trim().take(MAX_LABEL_LENGTH).takeIf(String::isNotEmpty)?.let { id to it }
                }.toMap(),
            ),
        )
    }

    private fun validatePortableLayout(json: JSONObject) {
        if (json.optString("format") != FORMAT) throw IllegalArgumentException("Not a Rumb Launcher backup")
        val columns = json.optInt("columns", 5)
        val rows = json.optInt("rows", 5)
        val pages = json.optInt("pageCount", 1)
        if (columns !in 4..8 || rows !in 4..8 || pages !in 1..9) {
            throw IllegalArgumentException("Backup contains an invalid home grid")
        }
        val ids = mutableSetOf<String>()
        val occupied = mutableSetOf<Pair<Int, Int>>()
        json.optJSONArray("homeItems").objects().forEach { item ->
            val type = runCatching { HomeItemType.valueOf(item.optString("type")) }.getOrNull()
                ?: throw IllegalArgumentException("Backup contains an unknown home item")
            if (type == HomeItemType.WIDGET) return@forEach
            val id = item.optString("id").takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("Backup contains a home item without an ID")
            if (!ids.add(id)) throw IllegalArgumentException("Backup contains duplicate home item IDs")
            val page = item.optInt("page", -1)
            val cell = item.optInt("cell", -1)
            if (page !in 0 until pages || cell !in 0 until columns * rows || !occupied.add(page to cell)) {
                throw IllegalArgumentException("Backup contains overlapping or out-of-bounds home items")
            }
            val apps = item.optJSONArray("apps")
            val minimumApps = if (type == HomeItemType.FOLDER) 2 else 1
            if (apps == null || apps.length() < minimumApps) {
                throw IllegalArgumentException("Backup contains an invalid app or folder")
            }
        }
    }

    private fun defaultClockSize(json: JSONObject): Int = when (json.optString("clockStyle", "LARGE")) {
        "COMPACT" -> 40
        "MINIMAL" -> 30
        else -> 58
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
        private const val MAX_LABEL_LENGTH = 48
        private const val PERSIST_DELAY_MS = 250L
        private val PACKAGE_NAME = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
    }
}
