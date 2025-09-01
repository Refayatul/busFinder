package com.rex.busfinder.ui.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape // Import for RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes // Import for Material 3 Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp // Import for dp
import androidx.core.view.WindowCompat

// --- DARK THEME COLOR SCHEME ---
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    scrim = DarkScrim
)

// --- LIGHT THEME COLOR SCHEME ---
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    scrim = LightScrim
)

// --- BLUE THEME COLOR SCHEME ---
private val BlueColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary,
    tertiaryContainer = BlueTertiaryContainer,
    onTertiaryContainer = BlueOnTertiaryContainer,
    background = BlueBackground,
    onBackground = BlueOnBackground,
    surface = BlueSurface,
    onSurface = BlueOnSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurfaceVariant = BlueOnSurfaceVariant,
    error = BlueError,
    onError = BlueOnError,
    errorContainer = BlueErrorContainer,
    onErrorContainer = BlueOnErrorContainer,
    outline = BlueOutline,
    scrim = BlueScrim
)

// --- Custom Shapes (defined directly in Theme.kt) ---
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // For smaller elements like chips
    small = RoundedCornerShape(8.dp),      // For buttons, text fields
    medium = RoundedCornerShape(12.dp),    // For cards
    large = RoundedCornerShape(16.dp),     // For larger cards or containers
    extraLarge = RoundedCornerShape(24.dp) // For sheets, dialogs
)


enum class Theme {
    LIGHT, DARK, BLUE
}

val LocalTheme = staticCompositionLocalOf { mutableStateOf(Theme.LIGHT) }

@Composable
fun BUSFinderTheme(
    themeState: MutableState<Theme> = LocalTheme.current,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color takes precedence on Android 12 (API 31) and above
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback to custom themes if dynamicColor is false or not supported
        themeState.value == Theme.DARK -> DarkColorScheme
        themeState.value == Theme.BLUE -> BlueColorScheme
        else -> LightColorScheme // Default to Light theme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Make the status bar transparent for edge-to-edge display
            window.statusBarColor = Color.TRANSPARENT
            // Ensure content draws behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val insetsController = WindowCompat.getInsetsController(window, view)
            // Set status bar icons to be light or dark based on the current theme's brightness
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // This still assumes Typography.kt exists and defines Typography
        shapes = Shapes,       // Now refers to the Shapes object defined in this file
        content = content
    )
}