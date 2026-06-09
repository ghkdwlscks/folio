package com.portfolio.manager.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.portfolio.manager.presentation.theme.LocalAppStrings
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
    val strings = LocalAppStrings.current
    val haptic = rememberHapticFeedback()
    val resolvedConfirmText = if (confirmText == "Confirm") strings.ok else confirmText
    val resolvedDismissText = if (dismissText == "Cancel") strings.cancel else dismissText

    BaseDialog(onDismiss = onDismiss) {
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
                    Text(resolvedDismissText)
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
                    Text(resolvedConfirmText)
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
    val strings = LocalAppStrings.current

    ConfirmationDialog(
        title = title,
        message = message,
        icon = Icons.Outlined.Delete,
        iconTint = MaterialTheme.colorScheme.error,
        confirmText = strings.delete,
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
    val strings = LocalAppStrings.current

    ConfirmationDialog(
        title = title,
        message = message,
        icon = Icons.Outlined.Warning,
        iconTint = MaterialTheme.colorScheme.tertiary,
        confirmText = if (confirmText == "Continue") strings.ok else confirmText,
        confirmColor = MaterialTheme.colorScheme.tertiary,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
