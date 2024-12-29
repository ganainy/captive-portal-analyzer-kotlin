package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable


@Composable
fun ActionAlertDialog(
    dialogData: ActionAlertDialogData,
) {
    if (dialogData.showDialog) {
        AlertDialog(
            onDismissRequest = dialogData.onDismiss,
            title = { Text(text = dialogData.title) },
            text = { Text(text = dialogData.message) },
            confirmButton = {
                TextButton(onClick = {
                    dialogData.onConfirm()
                    dialogData.onDismiss()
                }) {
                    Text(dialogData.confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = dialogData.onDismiss) {
                    Text(dialogData.dismissButtonText)
                }
            }
        )
    }
}

data class ActionAlertDialogData(
    var showDialog: Boolean,
    val title: String,
    val message: String,
    val confirmButtonText: String,
    val dismissButtonText: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)