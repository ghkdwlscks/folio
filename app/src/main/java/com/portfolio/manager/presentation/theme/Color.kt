package com.portfolio.manager.presentation.theme

import androidx.compose.ui.graphics.Color

// Premium Finance Color Palette

// Primary - Rich Indigo (premium, trustworthy)
val Primary80 = Color(0xFFC7D2FE)  // Indigo 200
val Primary60 = Color(0xFF818CF8)  // Indigo 400
val Primary40 = Color(0xFF6366F1)  // Indigo 500
val Primary20 = Color(0xFF4338CA)  // Indigo 700

// Secondary - Vibrant Cyan (growth, energy)
val Secondary80 = Color(0xFF99F6E4)  // Teal 200
val Secondary60 = Color(0xFF2DD4BF)  // Teal 400
val Secondary40 = Color(0xFF14B8A6)  // Teal 500
val Secondary20 = Color(0xFF0F766E)  // Teal 700

// Accent - Warm Amber (highlights, attention)
val Accent80 = Color(0xFFFDE68A)   // Amber 200
val Accent40 = Color(0xFFF59E0B)   // Amber 500

// Legacy aliases for compatibility
val Purple80 = Primary80
val PurpleGrey80 = Secondary80
val Pink80 = Accent80

val Purple40 = Primary40
val PurpleGrey40 = Secondary40
val Pink40 = Accent40

// Semantic colors for stocks (vibrant, clear)
val GainGreen = Color(0xFF22C55E)      // Green 500 - vibrant success
val GainGreenLight = Color(0xFFDCFCE7) // Green 100
val GainGreenDark = Color(0xFF166534)  // Green 800

val LossRed = Color(0xFFEF4444)        // Red 500 - clear warning
val LossRedLight = Color(0xFFFEE2E2)   // Red 100
val LossRedDark = Color(0xFF991B1B)    // Red 800

// Neutral color for zero change (no gain, no loss)
val NeutralGray = Color(0xFF9CA3AF)        // Gray 400
val NeutralGrayLight = Color(0xFFF3F4F6)   // Gray 100

// Pastel variants for dark backgrounds (brighter for visibility)
val GainGreenPastel = Color(0xFF86EFAC)  // Green 300
val LossRedPastel = Color(0xFFFCA5A5)    // Red 300

// Surface colors (warm neutrals)
val SurfaceLight = Color(0xFFFAFAFA)   // Neutral 50 - warm white
val SurfaceDark = Color(0xFF18181B)    // Zinc 900 - rich dark

// Pie chart colors (vibrant, distinguishable)
val ChartColors = listOf(
    Color(0xFF6366F1), // Indigo (primary)
    Color(0xFF14B8A6), // Teal (secondary)
    Color(0xFF22C55E), // Green
    Color(0xFF3B82F6), // Blue
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Violet
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFF84CC16), // Lime
    Color(0xFFE11D48), // Rose
    Color(0xFFA855F7), // Purple
    Color(0xFF0EA5E9), // Sky
    Color(0xFFEAB308), // Yellow
    Color(0xFF10B981), // Emerald
    Color(0xFF7C3AED), // Violet Dark
    Color(0xFF0891B2), // Cyan Dark
    Color(0xFF2563EB), // Blue Dark
    Color(0xFFDB2777), // Fuchsia
    Color(0xFF059669)  // Green Dark
)
