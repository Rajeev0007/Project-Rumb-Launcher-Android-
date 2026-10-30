package com.rumb.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.VisualStyle

@Immutable
data class VisualTokens(
    val accent: Color,
    val accentSecondary: Color,
    val foreground: Color,
    val mutedForeground: Color,
    val scrimTop: Color,
    val scrimBottom: Color,
    val surface: Color,
    val strongSurface: Color,
    val border: Color,
    val highlight: Color,
    val radius: Dp,
    val dockRadius: Dp,
    val elevation: Dp,
)

fun visualTokens(style: VisualStyle): VisualTokens = when (style) {
    VisualStyle.AURA_GLASS -> VisualTokens(
        accent = Color(0xFF9B8CFF),
        accentSecondary = Color(0xFF64D8FF),
        foreground = Color.White,
        mutedForeground = Color(0xD9FFFFFF),
        scrimTop = Color(0x2E261657),
        scrimBottom = Color(0xA30A1026),
        surface = Color(0x702A2452),
        strongSurface = Color(0xD6221C42),
        border = Color(0x59FFFFFF),
        highlight = Color(0x70FFFFFF),
        radius = 28.dp,
        dockRadius = 34.dp,
        elevation = 18.dp,
    )
    VisualStyle.SOFT_PANEL -> VisualTokens(
        accent = Color(0xFFFFB66D),
        accentSecondary = Color(0xFFFF7F92),
        foreground = Color(0xFF241B1A),
        mutedForeground = Color(0xC72F2523),
        scrimTop = Color(0x24FFF5EB),
        scrimBottom = Color(0x7A251A1C),
        surface = Color(0xE8FFF8F2),
        strongSurface = Color(0xFFF8EEE8),
        border = Color(0x70FFFFFF),
        highlight = Color.White,
        radius = 24.dp,
        dockRadius = 26.dp,
        elevation = 10.dp,
    )
    VisualStyle.CRYSTAL_DOCK -> VisualTokens(
        accent = Color(0xFF67E8F9),
        accentSecondary = Color(0xFF818CF8),
        foreground = Color.White,
        mutedForeground = Color(0xD6E9F8FF),
        scrimTop = Color(0x283EC9D4),
        scrimBottom = Color(0xB0061625),
        surface = Color(0x66223E52),
        strongSurface = Color(0xDE102A3A),
        border = Color(0x668BE9FF),
        highlight = Color(0x8FFFFFFF),
        radius = 20.dp,
        dockRadius = 22.dp,
        elevation = 22.dp,
    )
    VisualStyle.PURE_MINIMAL -> VisualTokens(
        accent = Color(0xFFF4F4F5),
        accentSecondary = Color(0xFFB9BBC3),
        foreground = Color.White,
        mutedForeground = Color(0xCFFFFFFF),
        scrimTop = Color(0x18000000),
        scrimBottom = Color(0x99000000),
        surface = Color(0xB31A1A1C),
        strongSurface = Color(0xF018181A),
        border = Color(0x30FFFFFF),
        highlight = Color(0x3DFFFFFF),
        radius = 16.dp,
        dockRadius = 18.dp,
        elevation = 6.dp,
    )
}

@Composable
fun VisualBackdrop(
    style: VisualStyle,
    modifier: Modifier = Modifier,
    opaque: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = visualTokens(style)
    val base = if (opaque) tokens.strongSurface else Color.Transparent
    Box(modifier.background(base)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(tokens.scrimTop, tokens.accent.copy(alpha = if (opaque) .16f else .09f), tokens.scrimBottom),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = if (opaque) .08f else .12f), Color.Transparent, Color.Black.copy(alpha = .30f)),
                ),
            ),
        )
        content()
    }
}

@Composable
fun VisualSurface(
    style: VisualStyle,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    shape: Shape = RoundedCornerShape(visualTokens(style).radius),
    elevation: Dp = visualTokens(style).elevation,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = visualTokens(style)
    val color = if (strong) tokens.strongSurface else tokens.surface
    Surface(
        modifier = modifier.shadow(elevation, shape, clip = false),
        shape = shape,
        color = color,
        border = BorderStroke(1.dp, tokens.border),
    ) {
        Box(
            Modifier.background(
                Brush.verticalGradient(listOf(tokens.highlight.copy(alpha = .13f), Color.Transparent)),
            ),
            content = content,
        )
    }
}

fun Modifier.glassBorder(style: VisualStyle, shape: Shape): Modifier =
    border(1.dp, visualTokens(style).border, shape)

fun VisualStyle.displayName(): String = when (this) {
    VisualStyle.AURA_GLASS -> "Aura glass"
    VisualStyle.SOFT_PANEL -> "Soft panel"
    VisualStyle.CRYSTAL_DOCK -> "Crystal dock"
    VisualStyle.PURE_MINIMAL -> "Pure minimal"
}

fun VisualStyle.description(): String = when (this) {
    VisualStyle.AURA_GLASS -> "Violet light, layered glass"
    VisualStyle.SOFT_PANEL -> "Warm, calm, softly elevated"
    VisualStyle.CRYSTAL_DOCK -> "Cool crystal edges and depth"
    VisualStyle.PURE_MINIMAL -> "Quiet contrast, reduced chrome"
}

@Composable
fun VisualDockSurface(
    style: VisualStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = visualTokens(style)
    VisualSurface(style, modifier, shape = RoundedCornerShape(tokens.dockRadius), content = content)
}

@Composable
fun VisualDrawerSurface(
    style: VisualStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = VisualSurface(
    style = style,
    modifier = modifier,
    strong = true,
    shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    elevation = 24.dp,
    content = content,
)

@Composable
fun VisualChip(
    style: VisualStyle,
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = visualTokens(style)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) tokens.accent.copy(alpha = .22f) else tokens.surface,
        border = BorderStroke(1.dp, if (selected) tokens.accent.copy(alpha = .72f) else tokens.border),
    ) { Box(content = content) }
}
