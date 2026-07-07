package dev.monday.ui.theme

import androidx.compose.ui.graphics.Color

// Primary palette — Deep navy base with electric blue accents
val MondayNavy = Color(0xFF0A0E1A)
val MondayNavyLight = Color(0xFF121829)
val MondayNavySurface = Color(0xFF1A2035)
val MondayNavyCard = Color(0xFF1E2540)
val MondayNavyElevated = Color(0xFF252D4A)

// Accent — Electric blue
val MondayBlue = Color(0xFF3B82F6)
val MondayBlueBright = Color(0xFF60A5FA)
val MondayBlueLight = Color(0xFF93C5FD)
val MondayBlueDim = Color(0xFF1D4ED8)
val MondayBlueGlow = Color(0x333B82F6)

// Secondary accent — Purple
val MondayPurple = Color(0xFF8B5CF6)
val MondayPurpleLight = Color(0xFFA78BFA)

// Status colors
val MondayGreen = Color(0xFF10B981)
val MondayGreenLight = Color(0xFF34D399)
val MondayRed = Color(0xFFEF4444)
val MondayRedLight = Color(0xFFF87171)
val MondayAmber = Color(0xFFF59E0B)
val MondayAmberLight = Color(0xFFFBBF24)

// Text
val MondayTextPrimary = Color(0xFFF1F5F9)
val MondayTextSecondary = Color(0xFF94A3B8)
val MondayTextTertiary = Color(0xFF64748B)
val MondayTextMuted = Color(0xFF475569)

// Gradient
val GradientBlue = listOf(MondayBlue, MondayPurple)
val GradientCard = listOf(Color(0xFF1E2540), Color(0xFF162036))
val GradientDark = listOf(MondayNavy, Color(0xFF0F1628))

// Priority score colors
fun priorityColor(score: Float): Color = when {
    score >= 0.9f -> MondayRed
    score >= 0.7f -> MondayAmber
    score >= 0.5f -> MondayBlue
    score >= 0.2f -> MondayTextTertiary
    else -> MondayTextMuted
}
