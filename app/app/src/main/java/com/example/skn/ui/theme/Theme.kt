package com.example.skn.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val colorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    primaryContainer   = SecondaryColor,
    onPrimaryContainer = OnSecondaryColor,
    secondary = SecondaryColor,
    onSecondary = OnSecondaryColor,
    secondaryContainer   = TertiaryColor,
    onSecondaryContainer = OnTertiaryColor,
    tertiary = TertiaryColor,
    onTertiary = OnTertiaryColor,
    background = BackgroundColor,
    onBackground = OnBackgroundColor,
    surface = SurfaceColor,
    onSurface = OnSurfaceColor,
)

@Composable
fun SKNTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}