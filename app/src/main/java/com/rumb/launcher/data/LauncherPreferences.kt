package com.rumb.launcher.data

import android.content.Context

enum class LauncherTheme { SYSTEM, LIGHT, DARK }

class LauncherPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("launcher_preferences", Context.MODE_PRIVATE)

    var favoriteKeys: Set<String>
        get() = preferences.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()
        set(value) {
            preferences.edit().putStringSet(KEY_FAVORITES, value).apply()
        }

    var columns: Int
        get() = preferences.getInt(KEY_COLUMNS, DEFAULT_COLUMNS).coerceIn(4, 6)
        set(value) {
            preferences.edit().putInt(KEY_COLUMNS, value.coerceIn(4, 6)).apply()
        }

    var iconSize: Int
        get() = preferences.getInt(KEY_ICON_SIZE, DEFAULT_ICON_SIZE).coerceIn(44, 68)
        set(value) {
            preferences.edit().putInt(KEY_ICON_SIZE, value.coerceIn(44, 68)).apply()
        }

    var theme: LauncherTheme
        get() = runCatching {
            LauncherTheme.valueOf(preferences.getString(KEY_THEME, LauncherTheme.SYSTEM.name).orEmpty())
        }.getOrDefault(LauncherTheme.SYSTEM)
        set(value) {
            preferences.edit().putString(KEY_THEME, value.name).apply()
        }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_THEME = "theme"
        private const val DEFAULT_COLUMNS = 5
        private const val DEFAULT_ICON_SIZE = 52
    }
}
