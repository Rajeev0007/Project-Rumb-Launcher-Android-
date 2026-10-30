package com.rumb.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B48C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1B0B5E),
    secondary = Color(0xFF5E5A71),
    secondaryContainer = Color(0xFFE4DFF6),
    onSecondaryContainer = Color(0xFF1B1730),
    tertiary = Color(0xFF7A5263),
    background = Color(0xFFF5F5F9),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF5F5F9),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E3EC),
    onSurfaceVariant = Color(0xFF5D5E69),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFE),
    surfaceContainer = Color(0xFFF0F0F5),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE9E9F0),
    outline = Color(0xFF8C8C98),
    outlineVariant = Color(0xFFDADAE3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BEFF),
    onPrimary = Color(0xFF2A1886),
    primaryContainer = Color(0xFF41309E),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFC8C3DC),
    secondaryContainer = Color(0xFF3A364C),
    onSecondaryContainer = Color(0xFFE4DFF6),
    tertiary = Color(0xFFEBB7CA),
    background = Color(0xFF0F0F13),
    onBackground = Color(0xFFEDEDF3),
    surface = Color(0xFF0F0F13),
    onSurface = Color(0xFFEDEDF3),
    surfaceVariant = Color(0xFF2A2A33),
    onSurfaceVariant = Color(0xFFA9A9B6),
    surfaceContainerLowest = Color(0xFF0A0A0D),
    surfaceContainerLow = Color(0xFF16161B),
    surfaceContainer = Color(0xFF1A1A20),
    surfaceContainerHigh = Color(0xFF1E1E25),
    surfaceContainerHighest = Color(0xFF292930),
    outline = Color(0xFF75757F),
    outlineVariant = Color(0xFF34343D),
)

private val AmoledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF101013),
    surfaceContainerHigh = Color(0xFF141417),
    surfaceContainerHighest = Color(0xFF1E1E22),
)

private val LauncherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun RumbLauncherTheme(
    preference: ThemePreference,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = preference == ThemePreference.DARK || preference == ThemePreference.AMOLED ||
        (preference == ThemePreference.SYSTEM && isSystemInDarkTheme())
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (preference == ThemePreference.AMOLED) {
                dynamic.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black)
            } else dynamic
        }
        preference == ThemePreference.AMOLED -> AmoledColors
        dark -> DarkColors
        else -> LightColors
    }
    // Home sits on the wallpaper, so the window theme keeps light system-bar icons
    // (windowLightStatusBar=false). Opaque overlays such as Settings switch them while shown.
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }
    MaterialTheme(colorScheme = colors, shapes = LauncherShapes, content = content)
}
