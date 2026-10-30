package com.rumb.launcher.data

import android.content.Context
import com.rumb.launcher.domain.AppId
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class AppUsage(val count: Int, val lastLaunch: Long) {
    /** Launch count with a three-day half-life, plus a boost for apps opened in the last two hours. */
    fun score(now: Long): Double {
        val ageHours = ((now - lastLaunch).coerceAtLeast(0L)) / 3_600_000.0
        val decayed = count * 0.5.pow(ageHours / 72.0)
        return decayed + if (ageHours < 2.0) 2.0 else 0.0
    }
}

/**
 * Counts launches made through Rumb to rank app suggestions. Data stays in private,
 * device-local preferences. It is never exported and needs no usage-access permission.
 */
class UsageTracker(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(load())
    val state: StateFlow<Map<String, AppUsage>> = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Any()
    private var writeJob: Job? = null
    @Volatile private var dirty = false

    fun recordLaunch(appId: AppId, now: Long = System.currentTimeMillis()) {
        mutableState.update { current ->
            val previous = current[appId.stableKey]
            val merged = current + (appId.stableKey to AppUsage((previous?.count ?: 0) + 1, now))
            if (merged.size <= MAX_ENTRIES) merged
            else merged.entries.sortedByDescending { it.value.lastLaunch }.take(MAX_ENTRIES).associate { it.key to it.value }
        }
        scheduleWrite()
    }

    fun clear() {
        mutableState.value = emptyMap()
        scheduleWrite()
    }

    fun flush() {
        writeJob?.cancel()
        write()
    }

    private fun scheduleWrite() {
        dirty = true
        writeJob?.cancel()
        writeJob = scope.launch {
            delay(WRITE_DELAY_MS)
            write()
        }
    }

    private fun write() = synchronized(writeLock) {
        if (!dirty) return@synchronized
        dirty = false
        val json = JSONObject()
        mutableState.value.forEach { (key, usage) -> json.put(key, JSONArray().put(usage.count).put(usage.lastLaunch)) }
        preferences.edit().putString(KEY_USAGE, json.toString()).apply()
    }

    private fun load(): Map<String, AppUsage> {
        val raw = preferences.getString(KEY_USAGE, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val entry = json.optJSONArray(key) ?: return@forEach
                    val count = entry.optInt(0, 0)
                    val last = entry.optLong(1, 0L)
                    if (count > 0 && last > 0L) put(key, AppUsage(count.coerceAtMost(1_000_000), last))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private companion object {
        const val PREFERENCES_NAME = "launcher_usage"
        const val KEY_USAGE = "usage_v1"
        const val MAX_ENTRIES = 300
        const val WRITE_DELAY_MS = 1_000L
    }
}
