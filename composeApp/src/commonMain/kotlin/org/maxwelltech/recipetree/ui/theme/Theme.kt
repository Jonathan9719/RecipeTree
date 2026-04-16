package org.maxwelltech.recipetree.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Sage,
    onPrimary = SurfaceCard,
    primaryContainer = SageLight,
    onPrimaryContainer = Forest,
    secondary = HoneyAccent,
    onSecondary = Charcoal,
    secondaryContainer = HoneyDark,
    onSecondaryContainer = SurfaceCard,
    background = Linen,
    onBackground = Charcoal,
    surface = SurfaceCard,
    onSurface = Charcoal,
    surfaceVariant = LinenDark,
    onSurfaceVariant = CharcoalLight,
    outline = SageLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = SageLight,
    onPrimary = Forest,
    primaryContainer = SageDark,
    onPrimaryContainer = SageLight,
    secondary = HoneyAccent,
    onSecondary = Charcoal,
    secondaryContainer = HoneyDark,
    onSecondaryContainer = SurfaceCard,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnBackgroundDark,
    outline = SageDark,
)

@Composable
fun RecipeTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RecipeTreeTypography(),
        content = content
    )
}