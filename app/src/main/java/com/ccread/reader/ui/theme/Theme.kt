package com.ccread.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A3B2E),
    onPrimary = Color(0xFFFFF8EE),
    primaryContainer = Color(0xFFF0DCD4),
    onPrimaryContainer = Color(0xFF4A1C15),
    secondary = Color(0xFF6B5A44),
    onSecondary = Color(0xFFFFF8EE),
    secondaryContainer = Color(0xFFE6DCCB),
    onSecondaryContainer = Color(0xFF2F261A),
    tertiary = Color(0xFF4F5D4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE5D8),
    onTertiaryContainer = Color(0xFF1E2A1C),
    background = Color(0xFFF4EEE2),
    onBackground = Color(0xFF2B231A),
    surface = Color(0xFFFBF6EC),
    onSurface = Color(0xFF2B231A),
    surfaceVariant = Color(0xFFE9DFCE),
    onSurfaceVariant = Color(0xFF6E6252),
    outline = Color(0xFFA99A82),
    outlineVariant = Color(0xFFD8CCB8),
    error = Color(0xFFA33A2B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF2DAD3),
    onErrorContainer = Color(0xFF3F120C),
    inverseSurface = Color(0xFF332A20),
    inverseOnSurface = Color(0xFFF7F0E4),
    scrim = Color(0xFF1A120B),
    surfaceContainerLowest = Color(0xFFFFFBF3),
    surfaceContainerLow = Color(0xFFF8F2E7),
    surfaceContainer = Color(0xFFF3EBDD),
    surfaceContainerHigh = Color(0xFFEDE3D2),
    surfaceContainerHighest = Color(0xFFE6DAC6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0A08A),
    onPrimary = Color(0xFF4A1C15),
    primaryContainer = Color(0xFF5E2B20),
    onPrimaryContainer = Color(0xFFF5D8CE),
    secondary = Color(0xFFCDBB9F),
    onSecondary = Color(0xFF33291B),
    secondaryContainer = Color(0xFF3E3324),
    onSecondaryContainer = Color(0xFFEADDC8),
    tertiary = Color(0xFFA9BAA2),
    onTertiary = Color(0xFF1E2A1C),
    tertiaryContainer = Color(0xFF35452F),
    onTertiaryContainer = Color(0xFFDDE8D7),
    background = Color(0xFF17120D),
    onBackground = Color(0xFFEFE6D6),
    surface = Color(0xFF211A13),
    onSurface = Color(0xFFEFE6D6),
    surfaceVariant = Color(0xFF2E251B),
    onSurfaceVariant = Color(0xFFB6A78F),
    outline = Color(0xFF7C6D58),
    outlineVariant = Color(0xFF3A3024),
    error = Color(0xFFE6A08D),
    onError = Color(0xFF3F120C),
    errorContainer = Color(0xFF5C261C),
    onErrorContainer = Color(0xFFF8DAD2),
    inverseSurface = Color(0xFFEFE6D6),
    inverseOnSurface = Color(0xFF2B231A),
    scrim = Color(0xFF1A120B),
    surfaceContainerLowest = Color(0xFF120E0A),
    surfaceContainerLow = Color(0xFF1B1510),
    surfaceContainer = Color(0xFF211A13),
    surfaceContainerHigh = Color(0xFF271F17),
    surfaceContainerHighest = Color(0xFF2E251B),
)

private val JingduTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 40.sp,
        letterSpacing = 3.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp,
    ),
)

private val JingduShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun JingduTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = JingduTypography,
        shapes = JingduShapes,
        content = content,
    )
}
