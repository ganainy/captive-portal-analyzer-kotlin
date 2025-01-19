package com.example.captive_portal_analyzer_kotlin.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


/**
 * A composable function that displays a loading indicator. It displays a circular
 * progress indicator and optional message. The message is updated every 500ms
 * to create a "..." effect.
 *
 * @param message the message to display while loading. If null, no message is displayed.
 * @param modifier the modifier to apply to the indicator.
 */
@Composable
fun LoadingIndicator(message: String? = null, modifier: Modifier = Modifier) {
    var loadingMessage by remember { mutableStateOf(message ?: "") }

    // Coroutine to update the dots in the message dynamically
    LaunchedEffect(message) {
        if (message != null) { // Only start the animation if a message is provided
            while (true) {
                delay(500)
                loadingMessage = when (loadingMessage) {
                    "$message." -> "$message.."
                    "$message.." -> "$message..."
                    else -> "$message."
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Centered content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (loadingMessage.isNullOrBlank()) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(50.dp)
            )
            }

            // Show the text only if a message is provided
            if (!loadingMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loadingMessage,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,showSystemUi = true)
@Composable
fun LoadingIndicatorPreview() {
    LoadingIndicator(message = "Loading")
}

@Preview(showSystemUi = true)
@Composable
fun LoadingIndicatorPreview2() {
    LoadingIndicator()
}