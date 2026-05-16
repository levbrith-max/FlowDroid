package com.flowdroid.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Flow Launcher–inspired palette ──────────────────────────────────────────

val FlowBlue     = Color(0xFF5C9CF5)   // accent
val FlowBlueDim  = Color(0xFF3A78D4)
val FlowSurface  = Color(0xFF1E2130)   // card background
val FlowBg       = Color(0xFF141724)   // main background
val FlowBgLight  = Color(0xFFF3F4F8)
val FlowSurfaceL = Color(0xFFFFFFFF)
val FlowDivider  = Color(0xFF2C2F45)
val FlowText     = Color(0xFFE8EAF6)
val FlowTextSub  = Color(0xFF9094B0)
val FlowTextDark = Color(0xFF1A1C2A)
val FlowRed      = Color(0xFFEF5350)
val FlowGreen    = Color(0xFF66BB6A)
val FlowAmber    = Color(0xFFFFA726)

private val DarkColorScheme = darkColorScheme(
    primary          = FlowBlue,
    onPrimary        = Color.White,
    primaryContainer = FlowBlueDim,
    secondary        = Color(0xFF7C83C8),
    background       = FlowBg,
    surface          = FlowSurface,
    onBackground     = FlowText,
    onSurface        = FlowText,
    surfaceVariant   = Color(0xFF252840),
    outline          = FlowDivider,
    error            = FlowRed,
)

private val LightColorScheme = lightColorScheme(
    primary          = FlowBlueDim,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD0E4FF),
    secondary        = Color(0xFF5259A8),
    background       = FlowBgLight,
    surface          = FlowSurfaceL,
    onBackground     = FlowTextDark,
    onSurface        = FlowTextDark,
    surfaceVariant   = Color(0xFFECEEF8),
    outline          = Color(0xFFCFD2E5),
    error            = FlowRed,
)

@Composable
fun FlowDroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = FlowTypography,
        content     = content,
    )
}

val FlowTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp),
)
