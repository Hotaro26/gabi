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
                darkColorScheme(primary = Color(0xFF81C784), secondary = Color(0xFFA5D6A7))
            } else {
                lightColorScheme(primary = Color(0xFF2E7D32), secondary = Color(0xFF4CAF50))
            }
        }
        AppTheme.Midnight -> {
            if (darkTheme) {
                darkColorScheme(primary = Color(0xFF90CAF9), secondary = Color(0xFF64B5F6))
            } else {
                lightColorScheme(primary = Color(0xFF1565C0), secondary = Color(0xFF1E88E5))
            }
        }
        AppTheme.Rose -> {
            if (darkTheme) {
                darkColorScheme(primary = Color(0xFFF48FB1), secondary = Color(0xFFF06292))
            } else {
                lightColorScheme(primary = Color(0xFFC2185B), secondary = Color(0xFFE91E63))
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
