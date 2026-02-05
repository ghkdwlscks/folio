package com.portfolio.manager.presentation.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Provides haptic feedback for UI interactions.
 * Wraps Android's haptic feedback system for easy use in Compose.
 */
class HapticFeedbackHelper(private val view: View) {

    /**
     * Light tap feedback for selections and toggles.
     */
    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Standard click feedback for button presses.
     */
    fun click() {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Heavier feedback for long press actions.
     */
    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Confirmation feedback for successful actions.
     */
    fun confirm() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /**
     * Rejection feedback for errors or invalid actions.
     */
    fun reject() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
}

/**
 * Remember a HapticFeedbackHelper instance for the current composition.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val view = LocalView.current
    return remember(view) { HapticFeedbackHelper(view) }
}
