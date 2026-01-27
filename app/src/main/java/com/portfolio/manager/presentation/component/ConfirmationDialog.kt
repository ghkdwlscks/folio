package com.portfolio.manager.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A reusable confirmation dialog for destructive actions.
 *
 * @param title The dialog title
 * @param message The confirmation message
 * @param confirmText Text for the confirm button (default: "Delete")
 * @param confirmColor Color for the confirm button (default: error color)
 * @param dismissText Text for the dismiss button (default: "Cancel")
 * @param onConfirm Called when user confirms the action
 * @param onDismiss Called when user dismisses the dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    confirmColor: Color = MaterialTheme.colorScheme.error,
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * A confirmation dialog specifically for delete operations.
 * Convenience wrapper around [ConfirmationDialog] with delete-specific defaults.
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
        confirmText = "Delete",
        confirmColor = MaterialTheme.colorScheme.error,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
