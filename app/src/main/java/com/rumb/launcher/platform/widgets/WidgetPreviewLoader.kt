package com.rumb.launcher.platform.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads provider-owned preview art with a bounded in-memory cache and icon fallback. */
class WidgetPreviewLoader(
    context: Context,
    private val catalog: WidgetProviderCatalog,
) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    suspend fun load(
        provider: InstalledWidgetProvider,
        widthPx: Int,
        heightPx: Int,
        densityDpi: Int,
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        val safeWidth = widthPx.coerceIn(64, 768)
        val safeHeight = heightPx.coerceIn(64, 512)
        val key = "${provider.stableKey}:$safeWidth:$safeHeight:$densityDpi"
        synchronized(cache) { cache.get(key) }?.asImageBitmap() ?: run {
            val info = catalog.providerInfo(provider) ?: return@withContext null
            val preview = runCatching { info.loadPreviewImage(appContext, densityDpi) }.getOrNull()
            val drawable = preview ?: runCatching { info.loadIcon(appContext, densityDpi) }.getOrNull()
                ?.let { icon -> runCatching { packageManager.getUserBadgedIcon(icon, info.profile) }.getOrDefault(icon) }
                ?: return@withContext null
            render(drawable, safeWidth, safeHeight).also { bitmap ->
                synchronized(cache) { cache.put(key, bitmap) }
            }.asImageBitmap()
        }
    }

    fun invalidate() = synchronized(cache) { cache.evictAll() }

    private fun render(drawable: Drawable, width: Int, height: Int): Bitmap {
        val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: width
        val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: height
        val scale = min(width.toFloat() / intrinsicWidth, height.toFloat() / intrinsicHeight)
        val drawWidth = (intrinsicWidth * scale).toInt().coerceIn(1, width)
        val drawHeight = (intrinsicHeight * scale).toInt().coerceIn(1, height)
        val left = (width - drawWidth) / 2
        val top = (height - drawHeight) / 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        drawable.draw(Canvas(bitmap))
        drawable.bounds = oldBounds
        return bitmap
    }

    companion object { private const val MAX_CACHE_KB = 24 * 1024 }
}
