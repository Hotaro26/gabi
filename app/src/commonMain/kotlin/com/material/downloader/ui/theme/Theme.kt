package com.material.downloader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

val M3ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

enum class AppTheme(val label: String) {
    Default("Lavender"),
    Forest("Forest"),
    Midnight("Midnight"),
    Rose("Rose"),
    Ocean("Ocean"),
    Sunset("Sunset"),
    Amethyst("Amethyst"),
    Cyberpunk("Cyberpunk"),
    Monochrome("Monochrome"),
    Dynamic("Dynamic (M3)")
}

@Composable
fun ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: AppTheme = AppTheme.Dynamic,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (theme) {
        AppTheme.Dynamic -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        AppTheme.Monochrome -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = Color.Black,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color.Black,
                    onPrimary = Color.White,
                    background = Color.White,
                    onBackground = Color.Black,
                    surface = Color.White,
                    onSurface = Color.Black
                )
            }
        }
        AppTheme.Forest -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF81C784), onPrimary = Color(0xFF00390A),
                    primaryContainer = Color(0xFF005313), onPrimaryContainer = Color(0xFF9DF49E),
                    secondary = Color(0xFFA5D6A7), onSecondary = Color(0xFF00390A),
                    secondaryContainer = Color(0xFF005313), onSecondaryContainer = Color(0xFFC0F3C2)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF2E7D32), onPrimary = Color.White,
                    primaryContainer = Color(0xFF9DF49E), onPrimaryContainer = Color(0xFF002204),
                    secondary = Color(0xFF4CAF50), onSecondary = Color.White,
                    secondaryContainer = Color(0xFFC0F3C2), onSecondaryContainer = Color(0xFF002204)
                )
            }
        }
        AppTheme.Midnight -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF90CAF9), onPrimary = Color(0xFF003258),
                    primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
                    secondary = Color(0xFF64B5F6), onSecondary = Color(0xFF003258),
                    secondaryContainer = Color(0xFF00497D), onSecondaryContainer = Color(0xFFD1E4FF)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1565C0), onPrimary = Color.White,
                    primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D36),
                    secondary = Color(0xFF1E88E5), onSecondary = Color.White,
                    secondaryContainer = Color(0xFFD1E4FF), onSecondaryContainer = Color(0xFF001D36)
                )
            }
        }
        AppTheme.Rose -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFF48FB1), onPrimary = Color(0xFF5C1133),
                    primaryContainer = Color(0xFF7D2649), onPrimaryContainer = Color(0xFFFFD9E2),
                    secondary = Color(0xFFF06292), onSecondary = Color(0xFF5C1133),
                    secondaryContainer = Color(0xFF7D2649), onSecondaryContainer = Color(0xFFFFD9E2)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFC2185B), onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF3E001D),
                    secondary = Color(0xFFE91E63), onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFD9E2), onSecondaryContainer = Color(0xFF3E001D)
                )
            }
        }
        AppTheme.Ocean -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF80DEEA), onPrimary = Color(0xFF00363D),
                    primaryContainer = Color(0xFF004F58), onPrimaryContainer = Color(0xFF9FF0FA),
                    secondary = Color(0xFF4DD0E1), onSecondary = Color(0xFF00363D),
                    secondaryContainer = Color(0xFF004F58), onSecondaryContainer = Color(0xFF9FF0FA),
                    tertiary = Color(0xFF81D4FA)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF00838F), onPrimary = Color.White,
                    primaryContainer = Color(0xFF9FF0FA), onPrimaryContainer = Color(0xFF001F24),
                    secondary = Color(0xFF0097A7), onSecondary = Color.White,
                    secondaryContainer = Color(0xFF9FF0FA), onSecondaryContainer = Color(0xFF001F24),
                    tertiary = Color(0xFF0277BD)
                )
            }
        }
        AppTheme.Sunset -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFFB74D), onPrimary = Color(0xFF4C2700),
                    primaryContainer = Color(0xFF6F3C00), onPrimaryContainer = Color(0xFFFFDCC1),
                    secondary = Color(0xFFFF8A65), onSecondary = Color(0xFF4C2700),
                    secondaryContainer = Color(0xFF6F3C00), onSecondaryContainer = Color(0xFFFFDCC1),
                    tertiary = Color(0xFFFFD54F)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFEF6C00), onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFDCC1), onPrimaryContainer = Color(0xFF2E1500),
                    secondary = Color(0xFFD84315), onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFDCC1), onSecondaryContainer = Color(0xFF2E1500),
                    tertiary = Color(0xFFF57F17)
                )
            }
        }
        AppTheme.Amethyst -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFCE93D8), onPrimary = Color(0xFF4A0072),
                    primaryContainer = Color(0xFF6A00A3), onPrimaryContainer = Color(0xFFEADDFF),
                    secondary = Color(0xFFBA68C8), onSecondary = Color(0xFF4A0072),
                    secondaryContainer = Color(0xFF6A00A3), onSecondaryContainer = Color(0xFFEADDFF),
                    tertiary = Color(0xFFE1BEE7)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF6A1B9A), onPrimary = Color.White,
                    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF270046),
                    secondary = Color(0xFF8E24AA), onSecondary = Color.White,
                    secondaryContainer = Color(0xFFEADDFF), onSecondaryContainer = Color(0xFF270046),
                    tertiary = Color(0xFF4A148C)
                )
            }
        }
        AppTheme.Cyberpunk -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF00E5FF), onPrimary = Color(0xFF000000),
                    primaryContainer = Color(0xFF003B42), onPrimaryContainer = Color(0xFF00E5FF),
                    secondary = Color(0xFFFF007F), onSecondary = Color(0xFF000000),
                    secondaryContainer = Color(0xFF5A002C), onSecondaryContainer = Color(0xFFFF007F),
                    background = Color(0xFF121212), surface = Color(0xFF1E1E1E)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF00B8D4), onPrimary = Color.White,
                    primaryContainer = Color(0xFFB3F5FC), onPrimaryContainer = Color(0xFF003B42),
                    secondary = Color(0xFFF50057), onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFB3CA), onSecondaryContainer = Color(0xFF5A002C),
                    background = Color(0xFFF5F5F5), surface = Color.White
                )
            }
        }
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = M3ExpressiveShapes,
        content = content
    )
}
