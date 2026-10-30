package com.rumb.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.rumb.launcher.domain.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF5A45C7), onPrimary = Color.White,
    secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260),
    surface = Color(0xFFFFF8FF), surfaceVariant = Color(0xFFE7E0EC),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9BFFF), onPrimary = Color(0xFF2B167E),
    secondary = Color(0xFFCBC2DB), tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218), surface = Color(0xFF141218),
)
private val AmoledColors = darkColorScheme(
    primary = Color(0xFFC9BFFF), onPrimary = Color(0xFF24106E),
    background = Color.Black, surface = Color.Black,
    surfaceVariant = Color(0xFF17151D), onBackground = Color(0xFFF2EDF7), onSurface = Color(0xFFF2EDF7),
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
        preference == ThemePreference.AMOLED -> AmoledColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
