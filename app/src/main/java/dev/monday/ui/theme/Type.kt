package dev.monday.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using system default font families for reliability.
// When Google Fonts is configured via res/font, swap to custom FontFamilies.
val HeadingFamily = FontFamily.Default
val BodyFamily = FontFamily.Default

val MondayTypography = Typography(
    // Headlines
    displayLarge = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = (-0.5).sp,
        color = MondayTextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.25).sp,
        color = MondayTextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = MondayTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = MondayTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MondayTextPrimary
    ),
    // Titles
    titleLarge = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = MondayTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = MondayTextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MondayTextSecondary
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = MondayTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MondayTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MondayTextTertiary
    ),
    // Labels
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MondayTextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = MondayTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        color = MondayTextTertiary
    )
)
