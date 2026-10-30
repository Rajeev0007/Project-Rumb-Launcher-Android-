package com.rumb.launcher.ui.design

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Rumb Expressive: the launcher's design language. It borrows the large collapsing titles and soft
 * rounded cards of One UI, the springy button groups, morphing toggles, and bar-thumb sliders of
 * Material 3 Expressive, and the vivid filled icons and quick tiles of OriginOS.
 */
object RumbMotion {
    fun <T> bouncy() = spring<T>(dampingRatio = .58f, stiffness = Spring.StiffnessMediumLow)
    fun <T> smooth() = spring<T>(dampingRatio = .9f, stiffness = Spring.StiffnessMedium)
}

/** Vivid category colours used by filled icons. */
object RumbPalette {
    val Violet = Color(0xFF7C6CF2)
    val Blue = Color(0xFF3E8BFF)
    val Teal = Color(0xFF12A594)
    val Orange = Color(0xFFF58A1F)
    val Pink = Color(0xFFEC4F8C)
    val Red = Color(0xFFE5484D)
    val Green = Color(0xFF2FA84F)
    val Indigo = Color(0xFF5B5BD6)
    val Slate = Color(0xFF6E7687)
    val Amber = Color(0xFFE8A317)
}

/** Shrinks slightly while pressed and springs back on release. */
@Composable
fun pressScale(interaction: InteractionSource, pressedScale: Float = .96f): State<Float> {
    val pressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) pressedScale else 1f, RumbMotion.bouncy(), label = "press scale")
}

/** A filled rounded-square icon with a white glyph, in the OriginOS / One UI settings style. */
@Composable
fun FilledIcon(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * .32f))
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, .14f), color))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * .54f))
    }
}

@Composable
fun ExpressiveSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else null,
    )
}

/**
 * Connected button group. The selected segment becomes fully round and filled; neighbours keep
 * tight inner corners. A pressed segment briefly widens, like Material 3 Expressive groups.
 */
@Composable
fun <T> ButtonGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().height(48.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val outer = 24.dp
            val inner = 8.dp
            val start by animateDpAsState(if (isSelected || index == 0) outer else inner, RumbMotion.bouncy(), label = "start corner")
            val end by animateDpAsState(if (isSelected || index == options.lastIndex) outer else inner, RumbMotion.bouncy(), label = "end corner")
            val weight by animateFloatAsState(if (pressed) 1.18f else 1f, RumbMotion.bouncy(), label = "segment width")
            val container by animateColorAsState(if (isSelected) colors.primary else colors.surfaceContainerHighest, label = "segment color")
            val content by animateColorAsState(if (isSelected) colors.onPrimary else colors.onSurface, label = "segment content")
            Box(
                Modifier.weight(weight).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
                    .background(container.copy(alpha = if (enabled) 1f else .5f))
                    .selectable(
                        selected = isSelected,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { if (!isSelected) onSelect(option) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * A thick-track slider with a vertical bar thumb and a gap either side of it. The value previews
 * while dragging; [onValueChangeFinished] fires once on release or tap.
 */
@Composable
fun ThickSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val active = if (enabled) colors.primary else colors.onSurface.copy(alpha = .38f)
    val inactive = colors.surfaceContainerHighest
    var widthPx by remember { mutableIntStateOf(1) }
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(.0001f)
    fun valueAt(x: Float): Float {
        val raw = valueRange.start + (x / widthPx).coerceIn(0f, 1f) * span
        if (steps <= 0) return raw
        val stepSize = span / (steps + 1)
        return (valueRange.start + ((raw - valueRange.start) / stepSize).roundToInt() * stepSize)
            .coerceIn(valueRange.start, valueRange.endInclusive)
    }
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(44.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    currentOnChange(valueAt(offset.x))
                    currentOnFinished()
                }
            }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { currentOnFinished() },
                    onDragCancel = { currentOnFinished() },
                ) { change, _ ->
                    change.consume()
                    currentOnChange(valueAt(change.position.x))
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                if (enabled) setProgress { target ->
                    currentOnChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    currentOnFinished()
                    true
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackHeight = 18.dp.toPx()
            val gap = 6.dp.toPx()
            val thumbWidth = 5.dp.toPx()
            val thumbHeight = 38.dp.toPx()
            val centerY = size.height / 2f
            val x = (fraction * size.width).coerceIn(thumbWidth / 2f, size.width - thumbWidth / 2f)
            val radius = CornerRadius(trackHeight / 2f)
            if (x - gap > 0f) {
                drawRoundRect(active, Offset(0f, centerY - trackHeight / 2f), Size(x - gap, trackHeight), radius)
            }
            if (x + gap < size.width) {
                drawRoundRect(inactive, Offset(x + gap, centerY - trackHeight / 2f), Size(size.width - x - gap, trackHeight), radius)
            }
            drawRoundRect(active, Offset(x - thumbWidth / 2f, centerY - thumbHeight / 2f), Size(thumbWidth, thumbHeight), CornerRadius(thumbWidth / 2f))
        }
    }
}

/** An OriginOS-style quick tile. Turning it on fills it and tightens its corners with a spring. */
@Composable
fun ToggleTile(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    status: String = if (checked) "On" else "Off",
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction, .95f)
    val corner by animateDpAsState(if (checked) 20.dp else 28.dp, RumbMotion.bouncy(), label = "tile corner")
    val container by animateColorAsState(if (checked) colors.primary else colors.surfaceContainerHigh, label = "tile color")
    val content by animateColorAsState(if (checked) colors.onPrimary else colors.onSurface, label = "tile content")
    Row(
        modifier.height(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(container)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(content.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = content, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, color = content.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

/** A large tappable tile that opens something (Quick panel, customise tray). */
@Composable
fun ActionTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction, .95f)
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledIcon(icon, tint, size = 42.dp)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * One UI style page: a tall title area that fades away as you scroll, and a compact top bar whose
 * title and background fade in. Scroll reads happen in draw/layer lambdas, so scrolling never recomposes.
 */
@Composable
fun LargeTitleScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val collapseDistancePx = with(density) { 120.dp.toPx() }
    val collapse = remember(listState, collapseDistancePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }
    Box(modifier.fillMaxSize().background(colors.surface)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "large-title") {
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(top = 56.dp)
                        .heightIn(min = if (onBack == null) 150.dp else 112.dp)
                        .padding(start = 28.dp, end = 28.dp, bottom = 18.dp)
                        .graphicsLayer { alpha = (1f - collapse.value * 1.3f).coerceIn(0f, 1f) },
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            content()
            item(key = "bottom-space") { Spacer(Modifier.navigationBarsPadding().height(28.dp)) }
        }
        Row(
            Modifier.fillMaxWidth()
                .drawBehind { drawRect(colors.surfaceContainer.copy(alpha = collapse.value)) }
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp).graphicsLayer { alpha = ((collapse.value - .55f) / .45f).coerceIn(0f, 1f) },
            )
        }
    }
}

/** A bottom sheet of large selectable options with an optional icon and description for each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    description: (T) -> String? = { null },
    icon: (T) -> ImageVector? = { null },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    val isSelected = option == selected
                    val container by animateColorAsState(if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh, label = "option color")
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(20.dp)).background(container)
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(option) }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icon(option)?.let {
                            Icon(it, contentDescription = null, tint = if (isSelected) colors.onPrimaryContainer else colors.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(label(option), style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) colors.onPrimaryContainer else colors.onSurface)
                            description(option)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = if (isSelected) colors.onPrimaryContainer.copy(alpha = .8f) else colors.onSurfaceVariant)
                            }
                        }
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = colors.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

/** Visual picker: a row of preview cards, the selected one outlined and tinted. */
@Composable
fun <T> PreviewPicker(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable (T, Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            val interaction = remember { MutableInteractionSource() }
            val scale by pressScale(interaction, .94f)
            val container by animateColorAsState(if (isSelected) colors.primaryContainer else colors.surfaceContainerHighest, label = "picker color")
            val border by animateColorAsState(if (isSelected) colors.primary else Color.Transparent, label = "picker border")
            Column(
                Modifier.weight(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(container)
                    .drawBehind {
                        if (border.alpha > 0f) drawRoundRect(border, cornerRadius = CornerRadius(20.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                    .selectable(selected = isSelected, interactionSource = interaction, indication = LocalIndication.current, role = Role.RadioButton) { onSelect(option) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                preview(option, isSelected)
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Sets system bar icon brightness to match an opaque surface while this is composed, and restores
 * light icons (for the wallpaper behind Home) when it leaves.
 */
@Composable
fun SystemBarsFor(surface: Color) {
    val view = LocalView.current
    val lightSurface = surface.luminance() > .5f
    DisposableEffect(view, lightSurface) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = lightSurface
        controller?.isAppearanceLightNavigationBars = lightSurface
        onDispose {
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
        }
    }
}
