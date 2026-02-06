package com.portfolio.manager.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.portfolio.manager.presentation.theme.AppAnimations

/**
 * Default enter animation for dialogs: fade + scale in.
 */
object DialogAnimations {
    val defaultEnter: EnterTransition
        @Composable get() = fadeIn(tween(AppAnimations.Duration.FAST)) +
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(AppAnimations.Duration.NORMAL)
                )

    val defaultExit: ExitTransition
        @Composable get() = fadeOut(tween(AppAnimations.Duration.FAST)) +
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(AppAnimations.Duration.FAST)
                )
}

/**
 * Base dialog wrapper that provides common dialog structure:
 * - Dialog with platform-independent width
 * - Centered content with animation
 * - Surface with rounded corners and elevation
 *
 * @param onDismiss Called when the dialog should be dismissed
 * @param horizontalPadding Padding around the dialog surface
 * @param maxHeightFraction Optional maximum height as fraction of screen (0f-1f)
 * @param enterTransition Animation when dialog appears
 * @param exitTransition Animation when dialog disappears
 * @param content The dialog content
 */
@Composable
fun BaseDialog(
    onDismiss: () -> Unit,
    horizontalPadding: Dp = 32.dp,
    maxHeightFraction: Float? = null,
    enterTransition: EnterTransition = DialogAnimations.defaultEnter,
    exitTransition: ExitTransition = DialogAnimations.defaultExit,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = enterTransition,
                exit = exitTransition
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (maxHeightFraction != null) {
                                Modifier.fillMaxHeight(maxHeightFraction)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    content()
                }
            }
        }
    }
}
