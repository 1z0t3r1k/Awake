package com.amiawake.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.core.view.WindowCompat

val AwakeBlue = Color(0xFF335F74)
val AwakeTeal = Color(0xFF2D6C68)
val NightIndigo = Color(0xFF59617F)
val WarmAmber = Color(0xFF936A26)

private val LightColors = lightColorScheme(
    primary = AwakeBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2EAF6),
    onPrimaryContainer = Color(0xFF0B3447),
    secondary = AwakeTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE9E5),
    onSecondaryContainer = Color(0xFF123B38),
    tertiary = NightIndigo,
    tertiaryContainer = Color(0xFFE1E4FF),
    background = Color(0xFFF8FAFB),
    surface = Color(0xFFF8FAFB),
    surfaceContainer = Color(0xFFEEF2F4),
    surfaceContainerHigh = Color(0xFFE7ECEF),
    onSurface = Color(0xFF172126),
    onSurfaceVariant = Color(0xFF526068),
    outline = Color(0xFF7C898F),
    outlineVariant = Color(0xFFC9D2D6),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAE0),
    onPrimary = Color(0xFF063548),
    primaryContainer = Color(0xFF244F62),
    onPrimaryContainer = Color(0xFFCDEAF8),
    secondary = Color(0xFF9FD1CC),
    onSecondary = Color(0xFF073B38),
    secondaryContainer = Color(0xFF245450),
    onSecondaryContainer = Color(0xFFC9ECE8),
    tertiary = Color(0xFFC2C7EB),
    tertiaryContainer = Color(0xFF414865),
    background = Color(0xFF101719),
    surface = Color(0xFF101719),
    surfaceContainer = Color(0xFF1A2327),
    surfaceContainerHigh = Color(0xFF222D31),
    onSurface = Color(0xFFE4EBEE),
    onSurfaceVariant = Color(0xFFBAC5CA),
    outline = Color(0xFF849197),
    outlineVariant = Color(0xFF3B474C),
    error = Color(0xFFFFB4AB),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun AmIAwakeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}
