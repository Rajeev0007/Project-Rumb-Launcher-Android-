package com.rumb.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rumb.launcher.ui.design.ButtonGroup
import com.rumb.launcher.ui.design.ExpressiveSwitch
import com.rumb.launcher.ui.design.FilledIcon
import com.rumb.launcher.ui.design.OptionSheet
import com.rumb.launcher.ui.design.RumbPalette
import com.rumb.launcher.ui.design.ThickSlider
import kotlin.math.roundToInt

/** Category colours for filled row icons. */
object SettingsTint {
    val Purple = RumbPalette.Violet
    val Blue = RumbPalette.Blue
    val Teal = RumbPalette.Teal
    val Orange = RumbPalette.Orange
    val Pink = RumbPalette.Pink
    val Red = RumbPalette.Red
    val Green = RumbPalette.Green
    val Slate = RumbPalette.Slate
}

/** A rounded card. An optional title sits inside the card, One UI style. */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            content()
        }
    }
}

@Composable
fun SettingsDivider(inset: Boolean = true) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (inset) 74.dp else 18.dp, end = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
    )
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    tint: Color = SettingsTint.Slate,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else .42f
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 66.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            FilledIcon(icon, tint.copy(alpha = alpha))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsNavRow(title: String, summary: String?, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    SettingsRow(title = title, summary = summary, icon = icon, tint = tint, onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    icon: ImageVector? = null,
    tint: Color = SettingsTint.Slate,
    enabled: Boolean = true,
) {
    SettingsRow(
        title = title,
        summary = summary,
        icon = icon,
        tint = tint,
        enabled = enabled,
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        ExpressiveSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Shows the current choice and opens a sheet of large options. Use for longer lists. */
@Composable
fun <T> SettingsChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    icon: ImageVector? = null,
    tint: Color = SettingsTint.Slate,
    enabled: Boolean = true,
    description: (T) -> String? = { null },
    optionIcon: (T) -> ImageVector? = { null },
) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(title = title, summary = label(selected), icon = icon, tint = tint, enabled = enabled, onClick = { open = true }) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        OptionSheet(
            title = title,
            options = options,
            selected = selected,
            label = label,
            description = description,
            icon = optionIcon,
            onDismiss = { open = false },
            onSelect = {
                open = false
                if (it != selected) onSelect(it)
            },
        )
    }
}

/** A titled connected button group for 2–4 options, shown inline. */
@Composable
fun <T> SettingsSegmentedRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
    summary: String? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        if (summary != null) {
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        ButtonGroup(options, selected, label, onSelect, Modifier.padding(top = 10.dp), enabled = enabled)
    }
}

/** − value + inside a tonal pill. */
@Composable
fun SettingsStepperRow(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    summary: String? = null,
    icon: ImageVector? = null,
    tint: Color = SettingsTint.Slate,
    enabled: Boolean = true,
    valueLabel: (Int) -> String = Int::toString,
) {
    SettingsRow(title = title, summary = summary, icon = icon, tint = tint, enabled = enabled) {
        Row(
            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onValueChange(value - 1) },
                enabled = enabled && value > range.first,
                modifier = Modifier.size(40.dp).semantics { contentDescription = "Decrease $title" },
            ) { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp)) }
            Text(
                valueLabel(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 28.dp),
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                enabled = enabled && value < range.last,
                modifier = Modifier.size(40.dp).semantics { contentDescription = "Increase $title" },
            ) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

/** Thick slider that previews while dragging and commits once on release. */
@Composable
fun SettingsSliderRow(
    title: String,
    value: Int,
    range: IntRange,
    step: Int,
    onValueCommitted: (Int) -> Unit,
    icon: ImageVector? = null,
    tint: Color = SettingsTint.Slate,
    valueLabel: (Int) -> String = Int::toString,
) {
    var dragValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val shown = ((dragValue - range.first) / step).roundToInt() * step + range.first
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        SettingsRow(title = title, icon = icon, tint = tint) {
            Text(valueLabel(shown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        ThickSlider(
            value = dragValue,
            onValueChange = { dragValue = it },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            onValueChangeFinished = {
                val committed = shown.coerceIn(range.first, range.last)
                if (committed != value) onValueCommitted(committed)
            },
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
fun SettingsStatusRow(
    title: String,
    ok: Boolean,
    okLabel: String,
    notOkLabel: String,
    icon: ImageVector,
    tint: Color,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    SettingsRow(title = title, summary = if (ok) okLabel else notOkLabel, icon = icon, tint = tint, onClick = onAction) {
        if (ok) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(SettingsTint.Green))
        } else if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun SettingsBanner(title: String, body: String, action: String, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            androidx.compose.material3.Button(onClick = onAction, modifier = Modifier.padding(top = 6.dp)) { Text(action) }
        }
    }
}
