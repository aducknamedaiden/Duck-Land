package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemePreference

private val GrapeLightColorScheme = lightColorScheme(
    primary = GrapePrimaryLight,
    onPrimary = GrapeOnPrimaryLight,
    primaryContainer = GrapePrimaryContainerLight,
    onPrimaryContainer = GrapeOnPrimaryContainerLight,
    secondary = GrapeSecondaryLight,
    onSecondary = GrapeOnSecondaryLight,
    secondaryContainer = GrapeSecondaryContainerLight,
    onSecondaryContainer = GrapeOnSecondaryContainerLight,
    tertiary = GrapeTertiaryLight,
    onTertiary = GrapeOnTertiaryLight,
    tertiaryContainer = GrapeTertiaryContainerLight,
    onTertiaryContainer = GrapeOnTertiaryContainerLight,
    background = GrapeBackgroundLight,
    onBackground = GrapeOnBackgroundLight,
    surface = GrapeSurfaceLight,
    onSurface = GrapeOnSurfaceLight,
    surfaceVariant = GrapeSurfaceVariantLight,
    onSurfaceVariant = GrapeOnSurfaceVariantLight,
    outline = GrapeOutlineLight
)

private val GrapeDarkColorScheme = darkColorScheme(
    primary = GrapePrimaryDark,
    onPrimary = GrapeOnPrimaryDark,
    primaryContainer = GrapePrimaryContainerDark,
    onPrimaryContainer = GrapeOnPrimaryContainerDark,
    secondary = GrapeSecondaryDark,
    onSecondary = GrapeOnSecondaryDark,
    secondaryContainer = GrapeSecondaryContainerDark,
    onSecondaryContainer = GrapeOnSecondaryContainerDark,
    tertiary = GrapeTertiaryDark,
    onTertiary = GrapeOnTertiaryDark,
    tertiaryContainer = GrapeTertiaryContainerDark,
    onTertiaryContainer = GrapeOnTertiaryContainerDark,
    background = GrapeBackgroundDark,
    onBackground = GrapeOnBackgroundDark,
    surface = GrapeSurfaceDark,
    onSurface = GrapeOnSurfaceDark,
    surfaceVariant = GrapeSurfaceVariantDark,
    onSurfaceVariant = GrapeOnSurfaceVariantDark,
    outline = GrapeOutlineDark
)

val DuckLandShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun DuckLandTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (useDark) GrapeDarkColorScheme else GrapeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DuckLandTypography,
        shapes = DuckLandShapes,
        content = content
    )
}
