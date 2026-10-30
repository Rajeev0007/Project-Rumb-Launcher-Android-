@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rumb.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.platform.icons.AppIconLoader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: InstalledApp,
    iconLoader: AppIconLoader,
    iconSize: Dp,
    iconShape: IconShape,
    themed: Boolean,
    homeStyle: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    labelColor: Color? = null,
) {
    val density = LocalDensity.current
    val densityDpi = LocalConfiguration.current.densityDpi
    val sizePx = with(density) { iconSize.roundToPx() }
    val tint = MaterialTheme.colorScheme.primary.toArgb()
    val bitmap by produceState<ImageBitmap?>(null, app.id, sizePx, densityDpi, themed, tint) {
        value = iconLoader.load(app.id, sizePx, densityDpi, themed, tint)
    }
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            val shape = iconClip(iconShape)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "Open ${app.label}${app.profileLabel?.let { ", $it profile" }.orEmpty()}",
                    modifier = Modifier.size(iconSize).clip(shape),
                )
            } else {
                Box(Modifier.size(iconSize).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = labelColor ?: if (homeStyle) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            style = if (homeStyle) MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(Color.Black.copy(alpha = .88f), Offset(0f, 1.5f), 4f),
            ) else MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        if (app.isWorkProfile) Text(
            "Work",
            fontSize = 9.sp,
            color = labelColor?.copy(alpha = .72f) ?: if (homeStyle) Color.White.copy(.72f) else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun FolderTile(folder: HomeItem, iconSize: Dp, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(iconSize).clip(RoundedCornerShape(18.dp)).background(
                Brush.linearGradient(listOf(Color.White.copy(.34f), Color.White.copy(.12f), Color.Black.copy(.18f))),
            ),
            Alignment.Center,
        ) {
            Icon(Icons.Default.Folder, contentDescription = "Open ${folder.title}", tint = Color.White, modifier = Modifier.size(iconSize * .62f))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            folder.title.ifBlank { "Folder" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(Color.Black.copy(alpha = .88f), Offset(0f, 1.5f), 4f),
            ),
        )
    }
}

private fun iconClip(shape: IconShape): Shape = when (shape) {
    IconShape.SYSTEM -> RectangleShape
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED -> RoundedCornerShape(24.dp)
    IconShape.SQUIRCLE -> RoundedCornerShape(34)
}

