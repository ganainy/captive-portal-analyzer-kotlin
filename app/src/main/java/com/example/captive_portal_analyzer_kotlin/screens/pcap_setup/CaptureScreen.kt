package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup


import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Define typealias for the callback for clarity
typealias IntentLauncher = (Intent) -> Unit

@Composable
fun CaptureScreen(
    // Callbacks to the Activity/Host to actually launch the intents
    onStartIntentLaunchRequested: IntentLauncher,
    onStopIntentLaunchRequested: IntentLauncher,
    onStatusIntentLaunchRequested: IntentLauncher,
    // Obtain ViewModel using lifecycle-viewmodel-compose artifact
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel
) {
    // Collect StateFlow using lifecycle-aware collection
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val receivedPacketCount by viewModel.receivedPacketCount.collectAsStateWithLifecycle()

    // Get context for creating the start intent
    val context = LocalContext.current

    CaptureScreenContent(
        modifier = modifier,
        captureState = captureState,
        statusMessage = statusMessage,
        receivedPacketCount = receivedPacketCount,
        // Pass lambdas that call ViewModel requests AND trigger Activity callbacks
        onStartClick = {
            viewModel.requestStartCapture() // Tell VM the intention
            // Immediately request launch *if* state changed to STARTING
            // (Or rely on Activity observing a dedicated signal Flow/Channel from VM - more complex)
            // Simpler: Assume VM state updated and let Activity handle it via the callback
            // Need to create the intent here or have VM expose it? Let's create in VM and pass via callback
            onStartIntentLaunchRequested(viewModel.createStartIntent(context)) // Ask Activity to launch
        },
        onStopClick = {
            viewModel.requestStopCapture()
            onStopIntentLaunchRequested(viewModel.createStopIntent())
        },
        onStatusClick = {
            viewModel.requestGetStatus()
            onStatusIntentLaunchRequested(viewModel.createGetStatusIntent())
        }
    )
}

@Composable
fun CaptureScreenContent(
    modifier: Modifier = Modifier,
    captureState: CaptureState,
    statusMessage: String,
    receivedPacketCount: Long,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onStatusClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartClick,
                // Enable based on VM state
                enabled = captureState == CaptureState.IDLE || captureState == CaptureState.STOPPED || captureState == CaptureState.ERROR,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Capture")
            }
            Button(
                onClick = onStopClick,
                // Enable based on VM state
                enabled = captureState == CaptureState.RUNNING || captureState == CaptureState.STARTING,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Capture")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Button
        Button(
            onClick = onStatusClick,
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Status")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Display Section
        StatusInfoRow(label = "State:", value = captureState.name)
        Spacer(modifier = Modifier.height(8.dp))
        StatusInfoRow(label = "Message:", value = statusMessage)
        Spacer(modifier = Modifier.height(8.dp))
        StatusInfoRow(label = "Packets Received:", value = receivedPacketCount.toString())
    }
}

@Composable
fun StatusInfoRow(label: String, value: String) {
    // (Same as before)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(140.dp) // Adjust width as needed
        )
        Text(text = value)
    }
}

// --- Preview ---

@Preview(showBackground = true)
@Composable
fun PreviewCaptureScreenIdle() {
    MaterialTheme {
        CaptureScreenContent(
            captureState = CaptureState.IDLE,
            statusMessage = "Ready.",
            receivedPacketCount = 0,
            onStartClick = {},
            onStopClick = {},
            onStatusClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCaptureScreenRunning() {
    MaterialTheme {
        CaptureScreenContent(
            captureState = CaptureState.RUNNING,
            statusMessage = "Running, 123 packets",
            receivedPacketCount = 123,
            onStartClick = {},
            onStopClick = {},
            onStatusClick = {}
        )
    }
}