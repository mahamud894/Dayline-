package com.dayline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DaylineColors = darkColorScheme(
    primary = Color(0xFFB7FF4A),
    secondary = Color(0xFFBD82FF),
    tertiary = Color(0xFF59F5D8),
    background = Color(0xFF0B0D14),
    surface = Color(0xFF151923),
    onPrimary = Color(0xFF11141C),
    onSecondary = Color(0xFF15101E),
    onBackground = Color(0xFFF4F5FF),
    onSurface = Color(0xFFF4F5FF)
)

@Composable
fun DaylineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DaylineColors,
        content = content
    )
}
