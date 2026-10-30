package com.rumb.launcher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.rumb.launcher.domain.ClockAlignment
import com.rumb.launcher.domain.ClockFont
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.ClockTimeFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun LauncherClock(
    style: ClockStyle,
    alignment: ClockAlignment,
    timeFormat: ClockTimeFormat,
    font: ClockFont,
    size: Int,
    showDate: Boolean,
    showSeconds: Boolean,
    modifier: Modifier = Modifier,
) {
    if (style == ClockStyle.HIDDEN) return
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                now = ZonedDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    LaunchedEffect(showSeconds) {
        while (true) {
            val currentMillis = System.currentTimeMillis()
            now = ZonedDateTime.now()
            val interval = if (showSeconds) 1_000L else 60_000L
            delay(interval - currentMillis % interval)
        }
    }
    val use24Hour = when (timeFormat) {
        ClockTimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        ClockTimeFormat.TWELVE_HOUR -> false
        ClockTimeFormat.TWENTY_FOUR_HOUR -> true
    }
    val skeleton = when {
        use24Hour && showSeconds -> "Hms"
        use24Hour -> "Hm"
        showSeconds -> "hms"
        else -> "hm"
    }
    val timePattern = remember(locale, skeleton) { DateFormat.getBestDateTimePattern(locale, skeleton) }
    val timeFormatter = remember(locale, timePattern) { DateTimeFormatter.ofPattern(timePattern, locale) }
    val datePattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM") }
    val dateFormatter = remember(locale, datePattern) { DateTimeFormatter.ofPattern(datePattern, locale) }
    val horizontalAlignment = when (alignment) {
        ClockAlignment.LEFT -> Alignment.Start
        ClockAlignment.CENTER -> Alignment.CenterHorizontally
        ClockAlignment.RIGHT -> Alignment.End
    }
    val textAlign = when (alignment) {
        ClockAlignment.LEFT -> TextAlign.Start
        ClockAlignment.CENTER -> TextAlign.Center
        ClockAlignment.RIGHT -> TextAlign.End
    }
    val fontFamily = when (font) {
        ClockFont.SANS -> FontFamily.SansSerif
        ClockFont.SERIF -> FontFamily.Serif
        ClockFont.MONOSPACE -> FontFamily.Monospace
    }
    val fontWeight = when (style) {
        ClockStyle.HIDDEN -> FontWeight.Thin
        ClockStyle.LARGE -> FontWeight.Light
        ClockStyle.COMPACT -> FontWeight.Normal
        ClockStyle.MINIMAL -> FontWeight.Thin
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = horizontalAlignment) {
        Text(
            text = now.format(timeFormatter),
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            fontSize = size.sp,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = Shadow(Color.Black.copy(alpha = .58f), Offset(0f, 2f), 8f),
            ),
            maxLines = 1,
        )
        if (showDate && style != ClockStyle.MINIMAL) Text(
            text = now.format(dateFormatter),
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(.86f),
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(Color.Black.copy(alpha = .56f), Offset(0f, 1f), 5f),
            ),
            fontFamily = fontFamily,
            textAlign = textAlign,
            maxLines = 1,
        )
    }
}
