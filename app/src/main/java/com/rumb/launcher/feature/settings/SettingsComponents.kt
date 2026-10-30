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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Category colours for row icons. Muted enough for light and dark surfaces. */
object SettingsTint {
    val Purple = Color(0xFF7C6CF2)
    val Blue = Color(0xFF3E8BFF)
    val Teal = Color(0xFF13A89E)
    val Orange = Color(0xFFF08A24)
    val Pink = Color(0xFFE5508C)
    val Red = Color(0xFFE5484D)
    val Green = Color(0xFF2FA84F)
    val Slate = Color(0xFF6B7385)
}

@Composable
fun SettingsIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = .16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** A titled, rounded card. Rows inside are separated by inset dividers. */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f),
    )
}

/** The base row: optional leading icon, title and summary, optional trailing content. */
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
    val alpha = if (enabled) 1f else .45f
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            SettingsIcon(icon, tint.copy(alpha = tint.alpha * alpha))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
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
fun SettingsNavRow(
    title: String,
    summary: String?,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    SettingsRow(title = title, summary = summary, icon = icon, tint = tint, onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Shows the current choice as the summary and opens a radio list to change it. */
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
) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        title = title,
        summary = label(selected),
        icon = icon,
        tint = tint,
        enabled = enabled,
        onClick = { open = true },
    )
    if (open) {
        ChoiceDialog(
            title = title,
            options = options,
            selected = selected,
            label = label,
            description = description,
            onDismiss = { open = false },
            onSelect = {
                open = false
                if (it != selected) onSelect(it)
            },
        )
    }
}

@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    description: (T) -> String?,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label(option), style = MaterialTheme.typography.bodyLarge)
                            description(option)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A compact − value + control for small integer ranges such as grid size. */
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onValueChange(value - 1) },
                enabled = enabled && value > range.first,
                modifier = Modifier.size(36.dp).semantics { contentDescription = "Decrease $title" },
            ) { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp)) }
            Text(
                valueLabel(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 36.dp).padding(horizontal = 6.dp),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            FilledTonalIconButton(
                onClick = { onValueChange(value + 1) },
                enabled = enabled && value < range.last,
                modifier = Modifier.size(36.dp).semantics { contentDescription = "Increase $title" },
            ) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

/**
 * A slider that previews locally while dragging and commits once on release, so expensive
 * changes (icon re-rendering, clock relayout) happen once instead of on every frame.
 */
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
    val shown = (dragValue / step).roundToInt() * step
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        SettingsRow(title = title, icon = icon, tint = tint) {
            Text(
                valueLabel(shown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                val committed = shown.coerceIn(range.first, range.last)
                if (committed != value) onValueCommitted(committed)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            modifier = Modifier.padding(start = if (icon != null) 72.dp else 16.dp, end = 16.dp),
        )
    }
}

/** Explanatory text inside a group. */
@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** A status line with a coloured dot, e.g. "Notification access · Allowed". */
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
    SettingsRow(
        title = title,
        summary = if (ok) okLabel else notOkLabel,
        icon = icon,
        tint = tint,
        onClick = onAction,
    ) {
        if (ok) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(SettingsTint.Green))
        } else if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun SettingsSectionSpacer() = Spacer(Modifier.size(4.dp))

@Composable
fun SettingsBanner(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            androidx.compose.material3.Button(onClick = onAction, modifier = Modifier.padding(top = 6.dp)) { Text(action) }
        }
    }
}
