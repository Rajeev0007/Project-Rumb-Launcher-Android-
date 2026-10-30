package com.rumb.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.rumb.launcher.data.LauncherTheme

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF5B43CB),
    secondary = androidx.compose.ui.graphics.Color(0xFF635B70),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFC9BFFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFCBC2DB),
    tertiary = androidx.compose.ui.graphics.Color(0xFFEFB8C8),
)

@Composable
fun RumbLauncherTheme(
    preference: LauncherTheme,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preference) {
        LauncherTheme.SYSTEM -> isSystemInDarkTheme()
        LauncherTheme.LIGHT -> false
        LauncherTheme.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
