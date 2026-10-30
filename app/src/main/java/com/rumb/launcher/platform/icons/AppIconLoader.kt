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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.IconAppearance
import com.rumb.launcher.platform.apps.LauncherAppsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppIconLoader(
    context: Context,
    private val catalog: LauncherAppsCatalog,
) {
    private val packageManager = context.applicationContext.packageManager
    private val iconPacks = IconPackManager(context)
    private val packLock = Any()
    @Volatile private var loadedPack: Pair<String, LoadedIconPack?>? = null
    private val cache = object : LruCache<String, ImageBitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.asAndroidBitmap().allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun cacheKey(appId: AppId, sizePx: Int, densityDpi: Int, appearance: IconAppearance, themeColor: Int): String =
        "${appId.stableKey}:$sizePx:$densityDpi:${appearance.themed}:${appearance.iconPack.orEmpty()}:$themeColor"

    /** Synchronous cache lookup so already-loaded icons render on the first frame without flashing. */
    fun peek(key: String): ImageBitmap? = synchronized(cache) { cache.get(key) }

    suspend fun load(
        appId: AppId,
        sizePx: Int,
        densityDpi: Int,
        appearance: IconAppearance,
        themeColor: Int,
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        val key = cacheKey(appId, sizePx, densityDpi, appearance, themeColor)
        peek(key) ?: run {
            val info = catalog.activityInfo(appId) ?: return@withContext null
            val packDrawable = appearance.iconPack?.let { pack(it)?.drawableFor(appId.component, densityDpi) }
            val base = packDrawable ?: info.getIcon(densityDpi)
            val drawable = if (packDrawable == null && appearance.themed) themedDrawable(base, themeColor) ?: base else base
            val user = catalog.userFor(appId.profileSerial)
            val badged = if (user != null) packageManager.getUserBadgedIcon(drawable, user) else drawable
            val bitmap = render(badged, sizePx)
            bitmap.prepareToDraw()
            bitmap.asImageBitmap().also { synchronized(cache) { cache.put(key, it) } }
        }
    }

    fun installedPacks(): List<IconPack> = iconPacks.installedPacks()

    fun invalidate() {
        synchronized(cache) { cache.evictAll() }
        synchronized(packLock) { loadedPack = null }
    }

    /** Releases memory when Android reports pressure. */
    fun trim() = synchronized(cache) { cache.trimToSize(MAX_CACHE_KB / 4) }

    private fun pack(packageName: String): LoadedIconPack? {
        loadedPack?.let { if (it.first == packageName) return it.second }
        synchronized(packLock) {
            loadedPack?.let { if (it.first == packageName) return it.second }
            val loaded = iconPacks.load(packageName)
            loadedPack = packageName to loaded
            return loaded
        }
    }

    private fun themedDrawable(source: Drawable, color: Int): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || source !is AdaptiveIconDrawable) return null
        val monochrome = source.monochrome?.constantState?.newDrawable()?.mutate() ?: return null
        monochrome.setTint(color)
        return LayerDrawable(arrayOf(ColorDrawable(Color.TRANSPARENT), monochrome))
    }

    private fun render(drawable: Drawable, size: Int): Bitmap {
        val safeSize = size.coerceIn(24, 512)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(0, 0, safeSize, safeSize)
        drawable.draw(Canvas(bitmap))
        drawable.bounds = oldBounds
        return bitmap
    }

    companion object { private const val MAX_CACHE_KB = 24 * 1024 }
}
