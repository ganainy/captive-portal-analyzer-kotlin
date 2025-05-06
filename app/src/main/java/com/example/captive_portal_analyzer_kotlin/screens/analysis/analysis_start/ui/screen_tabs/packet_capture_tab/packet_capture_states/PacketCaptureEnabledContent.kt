package com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states

import CompletedOverlay
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.InProgressOverlay
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.FileOpener
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * Composable content displayed when packet capture mode is enabled.
 * Shows the multi-step process for completing the analysis and saving the PCAP file.
 */
@Composable
 fun PacketCaptureEnabledContent(
    modifier: Modifier = Modifier,
    pcapdroidCaptureState: MainViewModel.PcapDroidCaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    copiedPcapFileUri: Uri?,
    onOpenFile: FileOpener,
    targetPcapName: String?,
    webViewActions: WebViewActions,
    storePcapFileToSession: () -> Unit,
    markAnalysisAsComplete: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ){

        // Nested Column for SCROLLABLE step content
        Column(
            modifier = Modifier
                .weight(1f) // Takes up available vertical space
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Step 1: Stop Capture
            if (pcapdroidCaptureState == MainViewModel.PcapDroidCaptureState.FILE_READY
                || pcapdroidCaptureState == MainViewModel.PcapDroidCaptureState.WRONG_FILE_PICKED //this is a step3 error but should affect step2 being completed
            ) {
                // This was CompletedOverlay in the original code
                CompletedOverlay {
                    StepOneContent(
                        captureState = pcapdroidCaptureState,
                        onStartCapture = onStartCapture,
                        onStopCapture = onStopCapture,
                        onStatusCheck = onStatusCheck,
                        statusMessage = statusMessage,
                    )
                }
            } else {
                // This was InProgressOverlay in the original code
                InProgressOverlay {
                    StepOneContent(
                        captureState = pcapdroidCaptureState,
                        onStartCapture = onStartCapture,
                        onStopCapture = onStopCapture,
                        onStatusCheck = onStatusCheck,
                        statusMessage = statusMessage,
                    )
                }
            }

            // Step 2: Select and Copy PCAP File
            if (copiedPcapFileUri != null) {
                // This was CompletedOverlay in the original code
                CompletedOverlay {
                    StepTwoContent(
                        captureState = pcapdroidCaptureState,
                        onOpenFile = onOpenFile,
                        statusMessage = statusMessage,
                        copiedPcapFileUri = copiedPcapFileUri,
                        storePcapFileToSession = storePcapFileToSession,
                        targetPcapName = targetPcapName
                    )
                }
            } else {
                // This was InProgressOverlay in the original code
                InProgressOverlay {
                    StepTwoContent(
                        captureState = pcapdroidCaptureState,
                        onOpenFile = onOpenFile,
                        statusMessage = statusMessage,
                        copiedPcapFileUri = copiedPcapFileUri,
                        targetPcapName = targetPcapName
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }


        // Step 4 Content (Finish Button)
        val isFinishButtonEnabled = pcapdroidCaptureState == MainViewModel.PcapDroidCaptureState.FILE_READY &&
                copiedPcapFileUri != null

        RoundCornerButton(
            modifier = modifier,
            onClick = markAnalysisAsComplete,
            buttonText = stringResource(R.string.save_pcap_file),
            trailingIcon = painterResource(id = R.drawable.pcapdroid),
            enabled = isFinishButtonEnabled,
            isLoading = pcapdroidCaptureState in listOf(
                MainViewModel.PcapDroidCaptureState.STARTING,
                MainViewModel.PcapDroidCaptureState.STOPPING
            )
        )
    }
}



// Step 1 Content - Remains the same, could be moved inside PacketCaptureEnabledContent
@Composable
internal fun StepOneContent(
    captureState: MainViewModel.PcapDroidCaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.step_1), style = MaterialTheme.typography.titleLarge)

        Text(
            text = stringResource(R.string.please_stop_packet_capture_after_finishing_interacting_with_the_webview),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.capture_state),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    StatusBadge(captureState)
                }

                Text(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

            }
        }

        ControlButton(
            captureState = captureState,
            onStartCapture = onStartCapture,
            onStopCapture = onStopCapture,
            onStatusCheck = onStatusCheck,
        )
    }
}


@Composable
private fun ControlButton(
    captureState: MainViewModel.PcapDroidCaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, label, onClick, isEnabled) = when (captureState) {
        MainViewModel.PcapDroidCaptureState.RUNNING ->
            Quad(R.drawable.stop_24px, stringResource(R.string.stop), onStopCapture, true)

        MainViewModel.PcapDroidCaptureState.STOPPED, MainViewModel.PcapDroidCaptureState.IDLE, MainViewModel.PcapDroidCaptureState.FILE_READY ->
            Quad(R.drawable.play_arrow_24px, stringResource(R.string.start), onStartCapture, true)

        else -> // STARTING, STOPPING, WRONG_FILE_PICKED (button is disabled)
            Quad(null, stringResource(R.string.loading), onStatusCheck, false)
    }

    // The enabled state also depends on the analysis completion status
    val buttonEnabled = isEnabled &&
            captureState != MainViewModel.PcapDroidCaptureState.STARTING &&
            captureState != MainViewModel.PcapDroidCaptureState.STOPPING

    RoundCornerButton(
        modifier = modifier,
        onClick = { if (buttonEnabled) onClick() }, // Only trigger onClick if truly enabled
        buttonText = label,
        trailingIcon = icon?.let { painterResource(id = it) },
        enabled = buttonEnabled,
        isLoading = captureState in listOf(
            MainViewModel.PcapDroidCaptureState.STARTING,
            MainViewModel.PcapDroidCaptureState.STOPPING
        )
    )
}

// Helper data class for button configuration - Remains the same
 data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


// Helper composables (StatusBadge, ControlButton) remain the same
@Composable
private fun StatusBadge(captureState: MainViewModel.PcapDroidCaptureState) {
    val (textColor, backgroundColor) = when (captureState) {
        MainViewModel.PcapDroidCaptureState.IDLE, MainViewModel.PcapDroidCaptureState.STOPPED -> Color.Gray to Color.Gray.copy(
            alpha = 0.1f
        )

        MainViewModel.PcapDroidCaptureState.STARTING, MainViewModel.PcapDroidCaptureState.STOPPING -> Color.Blue to Color.Blue.copy(
            alpha = 0.1f
        )

        MainViewModel.PcapDroidCaptureState.RUNNING -> Color.Green to Color.Green.copy(alpha = 0.1f)
        MainViewModel.PcapDroidCaptureState.FILE_READY -> Color.Green to Color.Green.copy(alpha = 0.1f)
        MainViewModel.PcapDroidCaptureState.WRONG_FILE_PICKED -> Color.Red to Color.Red.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = captureState.name.lowercase(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Step 3 Content - Remains the same, could be moved inside PacketCaptureEnabledContent
@Composable
internal fun StepTwoContent(
    captureState: MainViewModel.PcapDroidCaptureState,
    onOpenFile: FileOpener,
    storePcapFileToSession: () -> Unit = {},
    statusMessage: String,
    copiedPcapFileUri: Uri?,
    targetPcapName: String?
) {

    //todo move this logic to viewmodel
    // Check if the capture state is FILE_READY and store the pcap file to session
    // This logic feels out of place in a UI composable.
    if (captureState == MainViewModel.PcapDroidCaptureState.FILE_READY) {
        storePcapFileToSession()
    }


    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.step_2), style = MaterialTheme.typography.titleLarge)

        // what to do description
        Text(
            text = stringResource(
                R.string.please_press_open_file_and_select_this_file,
                targetPcapName ?: "capture.pcap"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        // error text if user select wrong file
        if (captureState == MainViewModel.PcapDroidCaptureState.WRONG_FILE_PICKED) {
            Text(
                text = stringResource(R.string.please_select_correct_file),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        RoundCornerButton(
            modifier = Modifier
                .padding(end = 8.dp),
            onClick = {
                // Extract file name from status message or use default
                val fileName = statusMessage.split(" ").lastOrNull { it.endsWith(".pcap") }
                    ?: "capture.pcap"
                onOpenFile(fileName)
            },
            buttonText = stringResource(R.string.open_file),
            trailingIcon = painterResource(id = R.drawable.folder_open_24px),
            enabled = captureState == MainViewModel.PcapDroidCaptureState.FILE_READY ||
                    captureState == MainViewModel.PcapDroidCaptureState.WRONG_FILE_PICKED, // keep button enabled if wrong file is picked to allow user to select another file
        )

        copiedPcapFileUri?.let {
            Text(
                text = stringResource(R.string.filed_copied_as, copiedPcapFileUri),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}




// --- Previews ---

// Simple mock lambdas
internal val mockUnitLambda = {}
internal val mockIntLambda: (Int) -> Unit = { index -> println("Tab index updated: $index") }
private val mockStorePcap: () -> Unit = { println("Mock store PCAP called") }


// --- Mock Implementations for Previews ---

// Simple mock for WebViewActions
internal val mockWebViewActions = object : WebViewActions {
    override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
    override suspend fun saveWebpageContent(
        webView: WebView, url: String, showToast: (String, ToastStyle) -> Unit
    ) {
    }

    override fun takeScreenshot(webView: WebView, url: String) {}
    override suspend fun saveWebViewRequest(request: WebViewRequest) {}
    override fun updateShowedHint(showed: Boolean) {}
    override fun stopAnalysis() {}
    override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
}

// Simple mock for FileOpener
private val mockFileOpener: FileOpener = { fileName ->
    println("Mock FileOpener called for: $fileName")
    // In a real preview, you might use a dummy Uri or just log
}


// --- PacketCaptureEnabledContent Previews ---

@Preview(
    showBackground = true,
    name = "Enabled - Start (No Internet, Idle Capture)",
    device = "spec:width=411dp,height=891dp"
)
@Preview(
    showBackground = true,
    name = "Enabled - Start (No Internet, Idle Capture) - Tablet Night",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PacketCaptureEnabledContentPreview_Start() {
    AppTheme {
        PacketCaptureEnabledContent(
            pcapdroidCaptureState = MainViewModel.PcapDroidCaptureState.IDLE,
            onStartCapture = mockUnitLambda,
            onStopCapture = mockUnitLambda,
            onStatusCheck = mockUnitLambda,
            statusMessage = "Capture is idle.",
            copiedPcapFileUri = null,
            onOpenFile = mockFileOpener,
            targetPcapName = "capture.pcap",
            webViewActions = mockWebViewActions,
            storePcapFileToSession = mockStorePcap,
            markAnalysisAsComplete = mockUnitLambda
        )
    }
}

@Preview(
    showBackground = true,
    name = "Enabled - Analysis Running, Capture Running",
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PacketCaptureEnabledContentPreview_AnalysisRunningCaptureRunning() {
    AppTheme {
        PacketCaptureEnabledContent(
            pcapdroidCaptureState = MainViewModel.PcapDroidCaptureState.RUNNING, // Step 2 InProgress, Stop button enabled
            onStartCapture = mockUnitLambda,
            onStopCapture = mockUnitLambda,
            onStatusCheck = mockUnitLambda,
            statusMessage = "Capture is running...",
            copiedPcapFileUri = null, // Step 3 InProgress
            onOpenFile = mockFileOpener,
            targetPcapName = "capture.pcap",
            webViewActions = mockWebViewActions,
            storePcapFileToSession = mockStorePcap,
            markAnalysisAsComplete = mockUnitLambda,
        )
    }
}

@Preview(
    showBackground = true,
    name = "Enabled - Analysis Complete, Capture Running",
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PacketCaptureEnabledContentPreview_AnalysisCompleteCaptureRunning() {
    AppTheme {
        PacketCaptureEnabledContent(
            pcapdroidCaptureState = MainViewModel.PcapDroidCaptureState.RUNNING, // Step 2 InProgress, Stop button enabled
            onStartCapture = mockUnitLambda,
            onStopCapture = mockUnitLambda,
            onStatusCheck = mockUnitLambda,
            statusMessage = "Capture is running...",
            copiedPcapFileUri = null, // Step 3 InProgress
            onOpenFile = mockFileOpener,
            targetPcapName = "capture.pcap",
            webViewActions = mockWebViewActions,
            storePcapFileToSession = mockStorePcap,
            markAnalysisAsComplete = mockUnitLambda
        )
    }
}

@Preview(
    showBackground = true,
    name = "Enabled - Analysis Complete, Capture Stopped (File Ready)",
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PacketCaptureEnabledContentPreview_AnalysisCompleteCaptureStopped() {
    AppTheme {
        PacketCaptureEnabledContent(
            pcapdroidCaptureState = MainViewModel.PcapDroidCaptureState.FILE_READY, // Step 2 Completed, Start button enabled (but disabled by Step 1 status in ControlButton logic) -> Let's fix ControlButton logic for clarity
            onStartCapture = mockUnitLambda,
            onStopCapture = mockUnitLambda,
            onStatusCheck = mockUnitLambda,
            statusMessage = "Capture stopped. File ready: capture.pcap", // Status includes file name
            copiedPcapFileUri = null, // Step 3 InProgress
            onOpenFile = mockFileOpener,
            targetPcapName = "capture.pcap",
            webViewActions = mockWebViewActions,
            storePcapFileToSession = mockStorePcap,
            markAnalysisAsComplete = mockUnitLambda
        )
    }
}

@Preview(
    showBackground = true,
    name = "Enabled - Analysis Complete, Capture Stopped (Wrong File)",
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PacketCaptureEnabledContentPreview_WrongFilePicked() {
    AppTheme {
        PacketCaptureEnabledContent(
            pcapdroidCaptureState = MainViewModel.PcapDroidCaptureState.WRONG_FILE_PICKED, // Step 2 Completed, shows error message in Step 3
            onStartCapture = mockUnitLambda,
            onStopCapture = mockUnitLambda,
            onStatusCheck = mockUnitLambda,
            statusMessage = "Capture stopped.", // Status might not have file name if wrong file picked
            copiedPcapFileUri = null, // Step 3 InProgress (still need correct file)
            onOpenFile = mockFileOpener,
            targetPcapName = "capture.pcap",
            webViewActions = mockWebViewActions,
            storePcapFileToSession = mockStorePcap,
            markAnalysisAsComplete = mockUnitLambda
        )
    }
}


@Preview(
    showBackground = true,
    name = "Enabled - All Steps Complete (Finish Enabled)",
    device = "spec:width=411dp,height=891dp"
)
@Preview(
    showBackground = true,
    name = "Enabled - All Steps Complete (Finish Enabled) - Tablet",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun PacketCaptureEnabledContentPreview_Completed() {
    AppTheme {
        PacketCaptureEnabledContent(
            pcapdroidCaptureState = MainViewModel.PcapDroidCaptureState.FILE_READY, // Step 2 Completed
            onStartCapture = mockUnitLambda,
            onStopCapture = mockUnitLambda,
            onStatusCheck = mockUnitLambda,
            statusMessage = "Capture completed. File ready: capture.pcap",
            copiedPcapFileUri = "content://mock/path/to/copied_capture.pcap".toUri(), // Step 3 Completed
            onOpenFile = mockFileOpener,
            targetPcapName = "capture.pcap",
            webViewActions = mockWebViewActions,
            storePcapFileToSession = mockStorePcap,
            markAnalysisAsComplete = mockUnitLambda
        )
    }
}