package com.ccread.reader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B2F2F),
    background = Color(0xFFF5ECD8),
    surface = Color(0xFFF5ECD8),
    onBackground = Color(0xFF3B2F2F),
    onSurface = Color(0xFF3B2F2F),
)

@Composable
fun JingduTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
