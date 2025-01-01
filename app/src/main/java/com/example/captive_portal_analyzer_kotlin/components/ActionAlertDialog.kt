package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable


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

sealed class DialogState {
    object Hidden : DialogState()
    data class Shown(
        val title: String,
        val message: String,
        val confirmText: String = "OK",
        val dismissText: String = "Cancel",
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : DialogState()
}

