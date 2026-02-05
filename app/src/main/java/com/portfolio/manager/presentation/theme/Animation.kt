package com.portfolio.manager.presentation.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Centralized animation specifications for consistent motion throughout the app.
 */
object AppAnimations {

    // Duration constants (in milliseconds)
    object Duration {
        const val INSTANT = 100
        const val FAST = 200
        const val NORMAL = 300
        const val SLOW = 500
        const val COUNTER = 600
        const val EMPHASIS = 800
    }

    // Spring configurations for interactive elements
    object Springs {
        /** Press feedback - bouncy and responsive */
        val Press: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )

        /** Toggle/switch animations - smooth with slight bounce */
        val Toggle: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )

        /** Expansion animations - gentle and natural */
        val Expansion: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        /** Snappy response for quick interactions */
        val Snappy: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
    }

    // Tween configurations for value animations
    object Tweens {
        /** Fast transitions for subtle changes */
        val Fast: AnimationSpec<Float> = tween(
            durationMillis = Duration.FAST,
            easing = FastOutSlowInEasing
        )

        /** Standard transitions */
        val Normal: AnimationSpec<Float> = tween(
            durationMillis = Duration.NORMAL,
            easing = FastOutSlowInEasing
        )

        /** Counter/value animations */
        val Counter: AnimationSpec<Float> = tween(
            durationMillis = Duration.COUNTER,
            easing = FastOutSlowInEasing
        )

        /** Emphasized animations for important changes */
        val Emphasis: AnimationSpec<Float> = tween(
            durationMillis = Duration.EMPHASIS,
            easing = FastOutSlowInEasing
        )
    }

    // Scale values for press states
    object Scale {
        const val PRESSED = 0.97f
        const val NORMAL = 1f
        const val EXPANDED = 1.02f
    }
}
