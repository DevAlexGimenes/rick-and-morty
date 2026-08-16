package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

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
        // Material3's AlertDialog renders through androidx.compose.ui.window.Dialog, which opens
        // a brand-new Android Window/composition root - it does NOT inherit
        // App.kt's NavHost-level `testTagsAsResourceId = true` (that flag only propagates within
        // its own composition subtree). Re-declaring it here is required for Maestro's `id:`
        // selectors to find CONFIRM_BUTTON_TEST_TAG/DISMISS_BUTTON_TEST_TAG on Android (issue #58,
        // Phase 1) - without it those testTags exist in Compose's own semantics tree but never
        // reach the platform accessibility tree Maestro reads.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(CONFIRM_BUTTON_TEST_TAG),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(DISMISS_BUTTON_TEST_TAG),
            ) { Text(dismissLabel) }
        },
    )
}

/**
 * Maestro E2E test hooks (issue #58, Phase 1). [androidx.compose.ui.platform.testTag] is a
 * testing-only semantics property - real accessibility services never read it.
 */
private const val CONFIRM_BUTTON_TEST_TAG = "confirm_exit_dialog_confirm"
private const val DISMISS_BUTTON_TEST_TAG = "confirm_exit_dialog_dismiss"
