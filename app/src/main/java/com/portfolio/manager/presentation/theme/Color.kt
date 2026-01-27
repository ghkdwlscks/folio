package com.portfolio.manager.presentation.theme

import androidx.compose.ui.graphics.Color

// Modern Finance Color Palette

// Primary - Teal (trust, stability)
val Primary80 = Color(0xFF80CBC4)  // Light teal
val Primary60 = Color(0xFF4DB6AC)  // Medium teal
val Primary40 = Color(0xFF009688)  // Teal
val Primary20 = Color(0xFF00796B)  // Dark teal

// Secondary - Deep Blue (professionalism)
val Secondary80 = Color(0xFF90CAF9)  // Light blue
val Secondary60 = Color(0xFF42A5F5)  // Medium blue
val Secondary40 = Color(0xFF1976D2)  // Blue
val Secondary20 = Color(0xFF0D47A1)  // Dark blue

// Accent - Amber (attention, highlights)
val Accent80 = Color(0xFFFFE082)   // Light amber
val Accent40 = Color(0xFFFFA000)   // Amber

// Legacy aliases for compatibility
val Purple80 = Primary80
val PurpleGrey80 = Secondary80
val Pink80 = Accent80

val Purple40 = Primary40
val PurpleGrey40 = Secondary40
val Pink40 = Accent40

// Semantic colors for stocks (modern, softer tones)
val GainGreen = Color(0xFF10B981)      // Emerald - modern green
val GainGreenLight = Color(0xFFD1FAE5) // Light emerald
val GainGreenDark = Color(0xFF065F46)  // Dark emerald

val LossRed = Color(0xFFEF4444)        // Softer red
val LossRedLight = Color(0xFFFEE2E2)   // Light red
val LossRedDark = Color(0xFF991B1B)    // Dark red

// Pastel variants for dark backgrounds
val GainGreenPastel = Color(0xFF6EE7B7)  // Soft emerald
val LossRedPastel = Color(0xFFFCA5A5)    // Soft red

// Surface colors (clean, modern)
val SurfaceLight = Color(0xFFF8FAFC)   // Slate 50 - slightly cooler white
val SurfaceDark = Color(0xFF0F172A)    // Slate 900 - rich dark blue

// Pie chart colors (20 distinct colors - cohesive with finance palette)
val ChartColors = listOf(
    Color(0xFF14B8A6), // Teal (primary)
    Color(0xFF3B82F6), // Blue (secondary)
    Color(0xFF10B981), // Emerald
    Color(0xFF06B6D4), // Cyan
    Color(0xFF6366F1), // Indigo
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFFF97316), // Orange
    Color(0xFF84CC16), // Lime
    Color(0xFF0891B2), // Cyan Dark
    Color(0xFFA855F7), // Purple
    Color(0xFF059669), // Green Dark
    Color(0xFFDB2777), // Fuchsia
    Color(0xFF7C3AED), // Violet Dark
    Color(0xFF0D9488), // Teal Dark
    Color(0xFFCA8A04), // Yellow
    Color(0xFF2563EB), // Blue Dark
    Color(0xFFE11D48), // Rose
    Color(0xFF4F46E5)  // Indigo Dark
)
