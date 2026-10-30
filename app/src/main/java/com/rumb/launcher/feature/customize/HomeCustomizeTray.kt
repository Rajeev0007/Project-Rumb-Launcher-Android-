package com.rumb.launcher.feature.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.ui.components.VisualChip
import com.rumb.launcher.ui.components.VisualSurface
import com.rumb.launcher.ui.components.description
import com.rumb.launcher.ui.components.displayName
import com.rumb.launcher.ui.components.visualTokens

@Composable
fun HomeCustomizeTray(
    selectedStyle: VisualStyle,
    layoutLocked: Boolean,
    onStyle: (VisualStyle) -> Unit,
    onWallpaper: () -> Unit,
    onWidgets: () -> Unit,
    onLayoutAndClock: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = visualTokens(selectedStyle)
    VisualSurface(
        style = selectedStyle,
        modifier = modifier.fillMaxWidth(),
        strong = true,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        elevation = 24.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape).background(tokens.mutedForeground.copy(alpha = .35f)),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Customize Home", color = tokens.foreground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (layoutLocked) "Layout locked · appearance is still available" else "Changes preview instantly",
                        color = tokens.mutedForeground,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (layoutLocked) Icon(Icons.Default.Lock, "Layout is locked", tint = tokens.accent, modifier = Modifier.size(20.dp))
            }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(VisualStyle.entries, key = { it.name }) { style ->
                    VisualStylePreview(style, style == selectedStyle, onStyle)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TrayAction(Icons.Default.Wallpaper, "Wallpaper", tokens.foreground, onWallpaper)
                TrayAction(Icons.Default.AddBox, "Widgets", if (layoutLocked) tokens.mutedForeground.copy(.55f) else tokens.foreground, onWidgets)
                TrayAction(Icons.Default.GridView, "Layout & clock", tokens.foreground, onLayoutAndClock)
                TrayAction(Icons.Default.Check, "Done", tokens.accent, onDone)
            }
        }
    }
}

@Composable
fun VisualStylePreview(style: VisualStyle, selected: Boolean, onSelect: (VisualStyle) -> Unit, modifier: Modifier = Modifier) {
    val tokens = visualTokens(style)
    VisualChip(
        style = style,
        selected = selected,
        modifier = modifier.width(132.dp).semantics {
            this.selected = selected
            contentDescription = "${style.displayName()} visual style${if (selected) ", selected" else ""}"
        }.clickable(role = Role.RadioButton) { onSelect(style) },
    ) {
        Column(Modifier.padding(7.dp)) {
        Box(
            Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(tokens.accent.copy(.88f), tokens.accentSecondary.copy(.80f), tokens.strongSurface))),
        ) {
            Row(Modifier.align(Alignment.BottomCenter).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(4) { Box(Modifier.size(11.dp).background(Color.White.copy(.78f), RoundedCornerShape(4.dp))) }
            }
            if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(17.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(style.displayName(), color = tokens.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
        Text(style.description(), color = tokens.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TrayAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.size(width = 82.dp, height = 64.dp).clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
