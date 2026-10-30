@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.rumb.launcher.feature.quickpanel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.ui.components.visualTokens
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
    val tokens = visualTokens(style)
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(30_000)
        }
    }
    val locale = Locale.getDefault()
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home", tint = Color.White)
                }
                Text(
                    "Quick panel",
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(.68f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                now.format(timeFormatter),
                color = Color.White,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
            )
            Text(
                now.format(dateFormatter),
                color = Color.White.copy(.62f),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth().widthIn(max = 720.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LargePanelCard(
                    icon = Icons.Default.Wifi,
                    title = "Internet",
                    subtitle = "Android connectivity panel",
                    accent = tokens.accent,
                    modifier = Modifier.weight(1f),
                    onClick = onInternet,
                )
                LargePanelCard(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth",
                    subtitle = "Open device settings",
                    accent = tokens.accentSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onBluetooth,
                )
            }
            Spacer(Modifier.height(24.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CirclePanelAction(Icons.Default.DisplaySettings, "Display", tokens.accent, onDisplay)
                CirclePanelAction(Icons.Default.VolumeUp, "Sound", tokens.accentSecondary, onSound)
                CirclePanelAction(Icons.Default.BatterySaver, "Battery", tokens.accent, onBattery)
                CirclePanelAction(Icons.Default.Wallpaper, "Wallpaper", tokens.accentSecondary, onWallpaper)
                CirclePanelAction(Icons.Default.Home, "Home settings", tokens.accent, onHomeSettings)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Controls open Android-managed panels. Rumb does not read or change protected system state.",
                color = Color.White.copy(.44f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun LargePanelCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.aspectRatio(1.18f).clickable(role = Role.Button, onClick = onClick),
        color = Color(0xFF18181B),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(.1f)),
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(52.dp).background(accent.copy(.2f), CircleShape), Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(27.dp))
            }
            Column {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(.5f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CirclePanelAction(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.size(width = 94.dp, height = 106.dp).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            color = Color(0xFF1B1B1E),
            border = BorderStroke(1.dp, Color.White.copy(.1f)),
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            label,
            color = Color.White.copy(.78f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}
