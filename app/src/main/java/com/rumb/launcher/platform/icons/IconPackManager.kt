package com.rumb.launcher.platform.icons

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Xml
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.xmlpull.v1.XmlPullParser

data class IconPack(val packageName: String, val label: String)

/**
 * Discovers and reads third-party icon packs that use the widely supported ADW/Nova
 * `appfilter.xml` format. Packs are read through public PackageManager resources only.
 */
class IconPackManager(context: Context) {
    private val packageManager = context.applicationContext.packageManager

    fun installedPacks(): List<IconPack> {
        val packs = LinkedHashMap<String, IconPack>()
        PACK_ACTIONS.forEach { action ->
            runCatching { queryActivities(Intent(action)) }.getOrDefault(emptyList()).forEach { info ->
                val packageName = info.activityInfo?.packageName ?: return@forEach
                if (packageName !in packs) {
                    val label = runCatching { info.loadLabel(packageManager).toString() }.getOrNull()
                    packs[packageName] = IconPack(packageName, label?.ifBlank { packageName } ?: packageName)
                }
            }
        }
        return packs.values.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun load(packageName: String): LoadedIconPack? = runCatching {
        val resources = packageManager.getResourcesForApplication(packageName)
        val mapping = parseAppFilter(resources, packageName)
        if (mapping.isEmpty()) null else LoadedIconPack(packageName, resources, mapping)
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun queryActivities(intent: Intent): List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        packageManager.queryIntentActivities(intent, 0)
    }

    @SuppressLint("DiscouragedApi")
    private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
        val result = HashMap<String, String>()
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        val parser: XmlPullParser = if (xmlId != 0) {
            resources.getXml(xmlId)
        } else {
            val text = runCatching {
                resources.assets.open("appfilter.xml").bufferedReader().use { it.readText() }
            }.getOrNull() ?: return result
            if (text.length > MAX_APPFILTER_CHARS) return result
            Xml.newPullParser().apply { setInput(StringReader(text)) }
        }
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && result.size < MAX_ENTRIES) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && !drawable.isNullOrBlank()) {
                        normalizeComponent(component)?.let { result.putIfAbsent(it, drawable) }
                    }
                }
                event = parser.next()
            }
        } finally {
            (parser as? XmlResourceParser)?.close()
        }
        return result
    }

    private fun normalizeComponent(raw: String): String? {
        val inner = raw.substringAfter("ComponentInfo{", "").substringBefore('}')
        if (inner.isBlank() || !inner.contains('/')) return null
        return ComponentName.unflattenFromString(inner)?.flattenToString()
    }

    private companion object {
        val PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
        )
        const val MAX_ENTRIES = 20_000
        const val MAX_APPFILTER_CHARS = 8 * 1024 * 1024
    }
}

class LoadedIconPack internal constructor(
    val packageName: String,
    private val resources: Resources,
    private val mapping: Map<String, String>,
) {
    private val drawableIds = ConcurrentHashMap<String, Int>()

    @SuppressLint("DiscouragedApi")
    fun drawableFor(component: String, densityDpi: Int): Drawable? {
        val name = mapping[component] ?: return null
        val id = drawableIds.getOrPut(name) { resources.getIdentifier(name, "drawable", packageName) }
        if (id == 0) return null
        return runCatching { resources.getDrawableForDensity(id, densityDpi, null) }.getOrNull()
    }
}
