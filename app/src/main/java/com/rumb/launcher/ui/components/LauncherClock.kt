package com.rumb.launcher.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rumb.launcher.domain.ClockStyle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun LauncherClock(style: ClockStyle, showDate: Boolean, showSeconds: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(showSeconds) {
        while (true) {
            now = LocalDateTime.now()
            delay(if (showSeconds) 1_000 else 30_000)
        }
    }
    val pattern = when {
        DateFormat.is24HourFormat(context) && showSeconds -> "HH:mm:ss"
        DateFormat.is24HourFormat(context) -> "HH:mm"
        showSeconds -> "h:mm:ss a"
        else -> "h:mm a"
    }
    val timeFormatter = remember(pattern, Locale.getDefault()) { DateTimeFormatter.ofPattern(pattern, Locale.getDefault()) }
    val dateFormatter = remember(Locale.getDefault()) { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val size = when (style) { ClockStyle.LARGE -> 58.sp; ClockStyle.COMPACT -> 40.sp; ClockStyle.MINIMAL -> 30.sp }
    Column(modifier) {
        Text(now.format(timeFormatter), color = Color.White, fontSize = size, fontWeight = FontWeight.Light)
        if (showDate && style != ClockStyle.MINIMAL) Text(
            now.format(dateFormatter), color = Color.White.copy(.86f), style = MaterialTheme.typography.titleMedium,
        )
    }
}
