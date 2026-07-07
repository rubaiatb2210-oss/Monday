package dev.monday.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MondayDarkColorScheme = darkColorScheme(
    primary = MondayBlue,
    onPrimary = MondayTextPrimary,
    primaryContainer = MondayBlueDim,
    onPrimaryContainer = MondayBlueLight,
    secondary = MondayPurple,
    onSecondary = MondayTextPrimary,
    secondaryContainer = MondayNavyElevated,
    onSecondaryContainer = MondayPurpleLight,
    tertiary = MondayGreen,
    onTertiary = MondayTextPrimary,
    background = MondayNavy,
    onBackground = MondayTextPrimary,
    surface = MondayNavyLight,
    onSurface = MondayTextPrimary,
    surfaceVariant = MondayNavySurface,
    onSurfaceVariant = MondayTextSecondary,
    surfaceContainerLowest = MondayNavy,
    surfaceContainerLow = MondayNavyLight,
    surfaceContainer = MondayNavySurface,
    surfaceContainerHigh = MondayNavyCard,
    surfaceContainerHighest = MondayNavyElevated,
    error = MondayRed,
    onError = MondayTextPrimary,
    outline = MondayTextMuted,
    outlineVariant = MondayNavyElevated
)

@Composable
fun MondayTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = MondayDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MondayNavy.toArgb()
            window.navigationBarColor = MondayNavy.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MondayTypography,
        content = content
    )
}
