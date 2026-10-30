package com.rumb.launcher.platform.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.platform.apps.LauncherAppsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppIconLoader(
    context: Context,
    private val catalog: LauncherAppsCatalog,
) {
    private val packageManager = context.applicationContext.packageManager
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    suspend fun load(
        appId: AppId,
        sizePx: Int,
        densityDpi: Int,
        themed: Boolean,
        themeColor: Int,
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        val key = "${appId.stableKey}:$sizePx:$densityDpi:$themed:$themeColor"
        synchronized(cache) { cache.get(key) }?.asImageBitmap() ?: run {
            val info = catalog.activityInfo(appId) ?: return@withContext null
            val base = info.getIcon(densityDpi)
            val drawable = themedDrawable(base, themeColor) ?: base
            val user = catalog.userFor(appId.profileSerial)
            val badged = if (user != null) packageManager.getUserBadgedIcon(drawable, user) else drawable
            render(badged, sizePx).also { synchronized(cache) { cache.put(key, it) } }.asImageBitmap()
        }
    }

    fun invalidate() = synchronized(cache) { cache.evictAll() }

    private fun themedDrawable(source: Drawable, color: Int): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || source !is AdaptiveIconDrawable) return null
        val monochrome = source.monochrome?.constantState?.newDrawable()?.mutate() ?: return null
        monochrome.setTint(color)
        return LayerDrawable(arrayOf(ColorDrawable(Color.TRANSPARENT), monochrome))
    }

    private fun render(drawable: Drawable, size: Int): Bitmap {
        val safeSize = size.coerceIn(48, 512)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(0, 0, safeSize, safeSize)
        drawable.draw(Canvas(bitmap))
        drawable.bounds = oldBounds
        return bitmap
    }

    companion object { private const val MAX_CACHE_KB = 16 * 1024 }
}
