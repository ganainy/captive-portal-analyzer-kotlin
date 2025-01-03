package com.example.captive_portal_analyzer_kotlin.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

@Composable
fun CustomSnackBar(
    message: String,
    onDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(true) }

    // Get navigation bars padding
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    LaunchedEffect(message, showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite,
                actionLabel = "Dismiss"
            )
            showSnackbar = false
            onDismiss()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            // Add navigation bar padding to the bottom
            modifier = Modifier.padding(
                bottom = navigationBarsPadding.calculateBottomPadding() + 16.dp
            ),
            snackbar = { snackbarData ->
                Snackbar(
                    action = {
                        TextButton(
                            onClick = {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        ) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        )
    }
}