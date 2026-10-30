package com.rumb.launcher.feature.quickpanel

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.ui.design.ActionTile
import com.rumb.launcher.ui.design.RumbPalette
import com.rumb.launcher.ui.design.SystemBarsFor
import com.rumb.launcher.ui.design.pressScale
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun QuickPanelScreen(
    style: VisualStyle,
    onBack: () -> Unit,
    onInternet: () -> Unit,
    onBluetooth: () -> Unit,
    onDisplay: () -> Unit,
    onSound: () -> Unit,
    onBattery: () -> Unit,
    onWallpaper: () -> Unit,
    onHomeSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SystemBarsFor(colors.surface)
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1_000L * (60 - now.second).coerceAtLeast(1))
        }
    }
    val locale = Locale.getDefault()
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }

    Column(
        Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(top = 28.dp, start = 10.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(now.format(timeFormatter), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Medium, color = colors.onSurface)
                    Text(now.format(dateFormatter), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ExpandLess, contentDescription = "Close quick panel", tint = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillTile("Internet", "Wi‑Fi and mobile data", Icons.Default.Wifi, RumbPalette.Blue, onInternet, Modifier.weight(1f))
                PillTile("Bluetooth", "Devices", Icons.Default.Bluetooth, RumbPalette.Indigo, onBluetooth, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Display", Icons.Default.DisplaySettings, RumbPalette.Amber, onDisplay, Modifier.weight(1f), "Brightness, dark mode")
                ActionTile("Sound", Icons.Default.VolumeUp, RumbPalette.Pink, onSound, Modifier.weight(1f), "Volume, vibration")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Battery", Icons.Default.BatterySaver, RumbPalette.Green, onBattery, Modifier.weight(1f), "Saver and usage")
                ActionTile("Wallpaper", Icons.Default.Wallpaper, RumbPalette.Violet, onWallpaper, Modifier.weight(1f), "Change background")
            }
            ActionTile("Default Home app", Icons.Default.Home, RumbPalette.Teal, onHomeSettings, Modifier.fillMaxWidth(), "Choose which launcher opens on Home")
        }
        Text(
            "These shortcuts open Android's own controls. Rumb never changes system settings itself.",
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(vertical = 20.dp),
        )
    }
}

/** One UI style wide tile: a filled circle icon and two lines of text in a tall pill. */
@Composable
private fun PillTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction, .95f)
    Row(
        modifier.height(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(42.dp))
            .background(tint)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
