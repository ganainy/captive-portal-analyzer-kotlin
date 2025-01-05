package com.example.captive_portal_analyzer_kotlin.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Composable
fun LoadingIndicator(message: String? = null, modifier: Modifier=Modifier) {
    var loadingMessage by remember { mutableStateOf("${message}.") }
    val scope = rememberCoroutineScope()

    // Coroutine to update the message every 500ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // Delay for half a second before updating the text
            loadingMessage = when (loadingMessage) {
                "${message}." -> "${message}.."
                "${message}.." -> "${message}..."
                else -> "${message}."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(50.dp)
            )
            message?.let {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = loadingMessage,  // Use the dynamic message
                    color = Color.White,
                    modifier = Modifier.padding(16.dp) // Add padding to the text
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