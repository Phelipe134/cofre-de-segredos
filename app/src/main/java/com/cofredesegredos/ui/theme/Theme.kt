package com.cofredesegredos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Foreground,
    onPrimary = OnForeground,
    secondary = Gold,
    onSecondary = Foreground,
    tertiary = Gold,
    background = Background,
    onBackground = Foreground,
    surface = Surface,
    onSurface = Foreground,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedForeground,
    outline = Border,
    error = Destructive,
    onError = OnForeground,
)

private val DarkColors = darkColorScheme(
    primary = ForegroundDark,
    onPrimary = Foreground,
    secondary = Gold,
    onSecondary = Foreground,
    tertiary = Gold,
    background = BackgroundDark,
    onBackground = ForegroundDark,
    surface = SurfaceDark,
    onSurface = ForegroundDark,
    surfaceVariant = MutedDark,
    onSurfaceVariant = MutedForegroundDark,
    outline = BorderDark,
    error = Destructive,
    onError = ForegroundDark,
)

@Composable
fun CofreDeSegredosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
