package com.yourcompany.yourapp.ui

import CaptureState
import CaptureViewModel
import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

typealias IntentLauncher = (Intent) -> Unit
// Callback specifically for opening the file, passing the expected filename
typealias FileOpener = (fileName: String) -> Unit

@Composable
fun CaptureScreen(
    onStartIntentLaunchRequested: IntentLauncher,
    onStopIntentLaunchRequested: IntentLauncher,
    onStatusIntentLaunchRequested: IntentLauncher,
    onOpenFileRequested: FileOpener,
    viewModel: CaptureViewModel,
    modifier: Modifier = Modifier
) {
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val targetPcapName by viewModel.targetPcapName.collectAsStateWithLifecycle()

    // Used only if filtering by app name in createStartIntent
    // val context = LocalContext.current

    CaptureScreenContent(
        modifier = modifier,
        captureState = captureState,
        statusMessage = statusMessage,
        targetPcapName = targetPcapName,
        onStartClick = {
            viewModel.requestStartCapture()
            // Use createStartIntent which now sets pcap_file mode
            // Pass context only if createStartIntent requires it
            onStartIntentLaunchRequested(viewModel.createStartIntent(/* context */))
        },
        onStopClick = {
            viewModel.requestStopCapture()
            onStopIntentLaunchRequested(viewModel.createStopIntent())
        },
        onStatusClick = {
            viewModel.requestGetStatus()
            onStatusIntentLaunchRequested(viewModel.createGetStatusIntent())
        },
        onOpenFileClick = {
            targetPcapName?.let { filename ->
                onOpenFileRequested(filename) // Trigger Activity action
                // Optionally update VM state after requesting open
                viewModel.fileProcessingDone()
            }
        }
    )
}

@Composable
fun CaptureScreenContent(
    modifier: Modifier = Modifier,
    captureState: CaptureState,
    statusMessage: String,
    targetPcapName: String?,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onStatusClick: () -> Unit,
    onOpenFileClick: () -> Unit // New click handler
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action Buttons Row
        Row(/* ... same as before ... */) {
            Button(
                onClick = onStartClick,
                enabled = captureState == CaptureState.IDLE || captureState == CaptureState.STOPPED || captureState == CaptureState.ERROR || captureState == CaptureState.FILE_READY,
                modifier = Modifier.weight(1f)
            ) { Text("Start Capture") }
            Button(
                onClick = onStopClick,
                enabled = captureState == CaptureState.RUNNING || captureState == CaptureState.STARTING,
                modifier = Modifier.weight(1f)
            ) { Text("Stop Capture") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStatusClick,
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Get Status") }

        Spacer(modifier = Modifier.height(16.dp)) // More space

        // *** Button to Open/Process File ***
        Button(
            onClick = onOpenFileClick,
            enabled = captureState == CaptureState.FILE_READY && targetPcapName != null, // Enabled when file should be ready
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (targetPcapName != null) "Open '$targetPcapName'" else "Open Capture File")
        }


        Spacer(modifier = Modifier.height(24.dp))

        // Status Display Section
        StatusInfoRow(label = "State:", value = captureState.name)
        Spacer(modifier = Modifier.height(8.dp))
        StatusInfoRow(label = "Message:", value = statusMessage)
        // Removed Packet Count Row
    }
}

@Composable
fun StatusInfoRow(label: String, value: String) {
    // (Same as before)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp)) // Adjusted width
        Text(value)
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun PreviewCaptureScreenFileReady() {
    MaterialTheme {
        CaptureScreenContent(
            captureState = CaptureState.FILE_READY,
            statusMessage = "Capture stopped. File 'my_app_capture.pcap' should be ready.",
            targetPcapName = "my_app_capture.pcap",
            onStartClick = {},
            onStopClick = {},
            onStatusClick = {},
            onOpenFileClick = {}
        )
    }
}