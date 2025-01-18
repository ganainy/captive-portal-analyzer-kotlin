package com.example.captive_portal_analyzer_kotlin.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Represents the state of a toast notification.
 */
sealed class ToastState {
    /**
     * State indicating that the toast is hidden.
     */
    object Hidden : ToastState()

    /**
     * State indicating that the toast is shown.
     * @param title Optional title of the toast.
     * @param message Required message of the toast.
     * @param style The style of the toast (default is SUCCESS).
     * @param duration Duration in milliseconds for which the toast should be shown (default is 2000L).
     */
    data class Shown(
        var title: String? = null,
        val message: String,
        val style: ToastStyle = ToastStyle.SUCCESS,
        val duration: Long? = 2000L
    ) : ToastState()
}

/**
 * Enum representing the style of the toast.
 */
enum class ToastStyle {
    SUCCESS,
    ERROR,
}

/**
 * Composable function to display a toast notification.
 * @param toastState The current state of the toast.
 * @param context The Android context to use for the toast (default is the current context).
 * @param onDismissRequest Callback to be invoked when the toast is dismissed.
 */
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