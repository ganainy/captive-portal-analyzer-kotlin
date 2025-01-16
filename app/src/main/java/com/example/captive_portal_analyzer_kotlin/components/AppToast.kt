package com.example.captive_portal_analyzer_kotlin.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class ToastState {
    object Hidden : ToastState()
    data class Shown(
        var title: String? = null,
        val message: String,
        val style: ToastStyle = ToastStyle.SUCCESS,
        val duration: Long? = 2000L
    ) : ToastState()
}

enum class ToastStyle {
    SUCCESS,
    ERROR,
}


@Composable
fun AppToast(
    toastState: ToastState,
    context: Context = LocalContext.current,
    onDismissRequest: () -> Unit
) {
    LaunchedEffect(toastState) {
        if (toastState is ToastState.Shown) {
            var message = toastState.message

            // If title is provided, combine it with the message
            if (!toastState.title.isNullOrEmpty()) {
                message = "${toastState.title}\n$message"
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    message,
                    if ((toastState.duration ?: 2000L) > 3000L) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }

            delay(toastState.duration ?: 2000L)
            onDismissRequest()
        }
    }
}