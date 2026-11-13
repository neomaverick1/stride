package com.neomaverick.stride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object StrideColors {
    val Background = Color(0xFF0A1020)
    val Surface = Color(0xFF131C30)
    val SurfaceHigh = Color(0xFF1B2640)
    val Lime = Color(0xFFC6F432)
    val Teal = Color(0xFF3DD6C4)
    val Coral = Color(0xFFFF6E6E)
    val Amber = Color(0xFFFFB347)
    val Violet = Color(0xFFB388FF)
    val Muted = Color(0xFF8C97B0)
}

private val scheme = darkColorScheme(
    primary = StrideColors.Lime, onPrimary = Color(0xFF0A1020),
    secondary = StrideColors.Teal, onSecondary = Color(0xFF0A1020),
    background = StrideColors.Background, onBackground = Color.White,
    surface = StrideColors.Surface, onSurface = Color.White,
    surfaceVariant = StrideColors.SurfaceHigh, onSurfaceVariant = StrideColors.Muted,
    surfaceContainer = StrideColors.Surface, surfaceContainerHigh = StrideColors.SurfaceHigh,
    error = StrideColors.Coral,
)

private val type = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

@Composable
fun StrideTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, typography = type, content = content)
