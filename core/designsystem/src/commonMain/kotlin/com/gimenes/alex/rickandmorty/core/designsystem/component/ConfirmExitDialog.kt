package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Shared "are you sure you want to leave" confirmation dialog (issue #55), generalized from
 * `GuessCharacterScreen`'s pre-existing mid-streak exit-confirmation `AlertDialog` (issue #16,
 * hardened in #17) so that dialog and Trivia Quiz's new mid-question leave-confirmation both
 * render through one shared component instead of two near-identical `AlertDialog` call sites.
 * Deliberately generic (not `ExitQuizDialog`/`ExitStreakDialog`) since every piece of copy is
 * caller-supplied - callers own the exact in-voice wording for their own flow.
 */
@Composable
fun ConfirmExitDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}
