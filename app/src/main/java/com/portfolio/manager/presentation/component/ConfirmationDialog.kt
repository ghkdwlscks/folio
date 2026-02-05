package com.portfolio.manager.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.portfolio.manager.presentation.theme.AppAnimations
import com.portfolio.manager.presentation.util.rememberHapticFeedback

/**
 * A modern Material 3 confirmation dialog with animations.
 *
 * @param title The dialog title
 * @param message The confirmation message
 * @param icon Optional icon to display above the title
 * @param iconTint Color for the icon (default: primary color)
 * @param confirmText Text for the confirm button (default: "Confirm")
 * @param confirmColor Color for the confirm button (default: primary color)
 * @param dismissText Text for the dismiss button (default: "Cancel")
 * @param onConfirm Called when user confirms the action
 * @param onDismiss Called when user dismisses the dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    confirmText: String = "Confirm",
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()

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
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(AppAnimations.Duration.FAST)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(AppAnimations.Duration.NORMAL)
                        ),
                exit = fadeOut(tween(AppAnimations.Duration.FAST)) +
                        scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(AppAnimations.Duration.FAST)
                        )
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = iconTint
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Message
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dismiss button (tonal)
                            FilledTonalButton(
                                onClick = {
                                    haptic.tick()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(dismissText)
                            }

                            // Confirm button (filled)
                            Button(
                                onClick = {
                                    haptic.click()
                                    onConfirm()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = confirmColor
                                )
                            ) {
                                Text(confirmText)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A confirmation dialog specifically for delete operations.
 * Includes a delete icon and error-colored confirm button.
 */
@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = title,
        message = message,
        icon = Icons.Outlined.Delete,
        iconTint = MaterialTheme.colorScheme.error,
        confirmText = "Delete",
        confirmColor = MaterialTheme.colorScheme.error,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * A confirmation dialog for warning/caution operations.
 * Includes a warning icon and warning-colored confirm button.
 */
@Composable
fun WarningConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Continue",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = title,
        message = message,
        icon = Icons.Outlined.Warning,
        iconTint = MaterialTheme.colorScheme.tertiary,
        confirmText = confirmText,
        confirmColor = MaterialTheme.colorScheme.tertiary,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
