@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rumb.launcher.ui.components

import android.app.ActivityOptions
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.IconAppearance
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.platform.apps.LaunchSource
import com.rumb.launcher.platform.icons.AppIconLoader
import kotlin.math.roundToInt

/** Remembers where a tile sits on screen so a launch can animate out of it. */
@Stable
class LaunchSourceTracker internal constructor(private val view: View) {
    internal var coordinates: LayoutCoordinates? = null

    fun source(): LaunchSource? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        val bounds = layout.boundsInWindow()
        if (bounds.width <= 0f || bounds.height <= 0f) return null
        val rect = android.graphics.Rect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )
        val options = runCatching {
            ActivityOptions.makeScaleUpAnimation(view, rect.left, rect.top, rect.width(), rect.height()).toBundle()
        }.getOrNull()
        return LaunchSource(rect, options)
    }
}

@Composable
fun rememberLaunchSourceTracker(): LaunchSourceTracker {
    val view = LocalView.current
    return remember(view) { LaunchSourceTracker(view) }
}

fun Modifier.trackLaunchSource(tracker: LaunchSourceTracker): Modifier = onPlaced { tracker.coordinates = it }

/**
 * An app icon that renders a cached bitmap on its first frame and loads missing icons off the
 * main thread. A neutral placeholder is shown only for icons that were never loaded.
 */
@Composable
fun AppIconImage(
    appId: AppId,
    iconLoader: AppIconLoader,
    size: Dp,
    appearance: IconAppearance,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val densityDpi = LocalConfiguration.current.densityDpi
    val sizePx = with(density) { size.roundToPx() }
    val tint = MaterialTheme.colorScheme.primary.toArgb()
    val key = remember(appId, sizePx, densityDpi, appearance, tint) {
        iconLoader.cacheKey(appId, sizePx, densityDpi, appearance, tint)
    }
    val initial = remember(key) { iconLoader.peek(key) }
    val bitmap by produceState<ImageBitmap?>(initialValue = initial, key1 = key) {
        val cached = iconLoader.peek(key)
        if (cached != null) {
            value = cached
        } else {
            value = iconLoader.load(appId, sizePx, densityDpi, appearance, tint)
        }
    }
    val shape = iconClip(appearance.shape)
    val clipModifier = if (appearance.shape == IconShape.SYSTEM) Modifier else Modifier.clip(shape)
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier.size(size).then(clipModifier),
        )
    } else {
        Box(
            modifier.size(size).clip(if (appearance.shape == IconShape.SYSTEM) CircleShape else shape)
                .background(Color.White.copy(alpha = .14f)),
        )
    }
}

/**
 * Icon and label. When [onClick] is null the tile is display-only and the caller owns gestures
 * (Home uses this for its combined tap / long-press menu / drag handling).
 */
@Composable
fun AppTile(
    app: InstalledApp,
    iconLoader: AppIconLoader,
    iconSize: Dp,
    appearance: IconAppearance,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.White,
    labelShadow: Boolean = true,
    showLabel: Boolean = true,
    onClick: ((LaunchSource?) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val tracker = rememberLaunchSourceTracker()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .88f else 1f,
        animationSpec = spring(dampingRatio = .6f, stiffness = Spring.StiffnessMediumLow),
        label = "app press scale",
    )
    val clickModifier = if (onClick != null) {
        Modifier.clip(RoundedCornerShape(18.dp)).combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClickLabel = "Open ${app.label}",
            onLongClickLabel = if (onLongClick != null) "More options" else null,
            onLongClick = onLongClick,
            onClick = { onClick(tracker.source()) },
        )
    } else Modifier
    Column(
        modifier = modifier.then(clickModifier).padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIconImage(
            appId = app.id,
            iconLoader = iconLoader,
            size = iconSize,
            appearance = appearance,
            contentDescription = "${app.label}${app.profileLabel?.let { ", $it profile" }.orEmpty()}",
            modifier = Modifier.trackLaunchSource(tracker).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            TileLabel(app.label, labelColor, labelShadow, maxLines = 2)
            if (app.isWorkProfile) Text("Work", fontSize = 9.sp, color = labelColor.copy(alpha = .72f))
        }
    }
}

@Composable
fun FolderTile(
    folder: HomeItem,
    apps: List<InstalledApp>,
    iconLoader: AppIconLoader,
    iconSize: Dp,
    appearance: IconAppearance,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val shape = RoundedCornerShape(iconSize * .3f)
    val inset = iconSize * .12f
    val gap = iconSize * .06f
    val mini = (iconSize - inset * 2 - gap) / 2
    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(iconSize).clip(shape).background(Color.White.copy(alpha = .22f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = .30f)), shape)
                .padding(inset),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                apps.take(4).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { app ->
                            AppIconImage(app.id, iconLoader, mini, appearance.copy(shape = IconShape.SYSTEM), contentDescription = null)
                        }
                    }
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            TileLabel(folder.title.ifBlank { "Folder" }, Color.White, shadow = true, maxLines = 1)
        }
    }
}

@Composable
private fun TileLabel(text: String, color: Color, shadow: Boolean, maxLines: Int) {
    Text(
        text = text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        color = color,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        style = if (shadow) MaterialTheme.typography.labelSmall.copy(
            shadow = Shadow(Color.Black.copy(alpha = .88f), Offset(0f, 1.5f), 4f),
        ) else MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun iconClip(shape: IconShape): Shape = when (shape) {
    IconShape.SYSTEM -> RectangleShape
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED -> RoundedCornerShape(24)
    IconShape.SQUIRCLE -> RoundedCornerShape(34)
}
