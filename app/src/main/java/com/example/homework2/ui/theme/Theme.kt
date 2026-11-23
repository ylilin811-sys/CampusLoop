package com.example.homework2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryTeal,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = SurfaceLight,
    onBackground = TextMainDark,
    onSurface = TextMainDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onBackground = TextMainLight,
    onSurface = TextMainLight
)

@Composable
fun CampusMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
