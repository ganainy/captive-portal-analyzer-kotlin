package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable


/**
 * Composable function to display an alert dialog with two buttons.
 *
 * The title, message, confirm text, dismiss text, on confirm action and on dismiss action
 * are passed in via the [DialogState] which is a sealed class with two subclasses: [Hidden]
 * and [Shown].
 *
 * If the dialog state is [Hidden], nothing is displayed.
 *
 * If the dialog state is [Shown], an [AlertDialog] is displayed with the title, message,
 * confirm button and dismiss button.
 *
 * The confirm button will call the on confirm action when clicked, and the dismiss button
 * will call the on dismiss action when clicked. After either button is clicked, the
 * [onDismissRequest] lambda will be called.
 *
 * The [onDismissRequest] lambda is used to dismiss the dialog. You should call this when
 * you want the dialog to be dismissed.
 *
 * @param dialogState the state of the dialog. If [Hidden], nothing is displayed. If
 * [Shown], an alert dialog is displayed.
 * @param onDismissRequest a lambda that will be called when the dialog should be
 * dismissed.
 */
@Composable
fun ActionAlertDialog(
    dialogState: DialogState,
    onDismissRequest: () -> Unit
) {
    if (dialogState is DialogState.Shown) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = dialogState.title) },
            text = { Text(text = dialogState.message) },
            confirmButton = {
                TextButton(onClick = {
                    dialogState.onConfirm()
                    onDismissRequest()
                }) {
                    Text(dialogState.confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialogState.onDismiss()
                    onDismissRequest()
                }) {
                    Text(dialogState.dismissText)
                }
            }
        )
    }
}

/**
 * Represents the state of the dialog.
 */
sealed class DialogState {
    /**
     * Represents a hidden state where the dialog is not visible.
     */
    object Hidden : DialogState()

    /**
     * Represents a shown state where the dialog is visible with title, message
     * and buttons for confirming or dismissing actions.
     *
     * @property title The title of the dialog.
     * @property message The message displayed in the dialog.
     * @property confirmText The text for the confirm button.
     * @property dismissText The text for the dismiss button.
     * @property onConfirm Callback function executed on confirm action.
     * @property onDismiss Callback function executed on dismiss action.
     */
    data class Shown(
        val title: String,
        val message: String,
        val confirmText: String = "OK",
        val dismissText: String = "Cancel",
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : DialogState()
}
