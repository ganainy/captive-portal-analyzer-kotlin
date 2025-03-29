
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.InProgressOverlay
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisCallbacks
import com.example.captive_portal_analyzer_kotlin.screens.analysis.CaptureActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.FileOpener
import com.example.captive_portal_analyzer_kotlin.screens.analysis.WebViewActions
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun PacketCaptureTab(
    captureState: CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    onOpenFile: FileOpener,
    captureActions: CaptureActions,
    webViewActions: WebViewActions,
    analysisCallbacks: AnalysisCallbacks,
    copiedPcapFileUri: Uri?,
    modifier: Modifier = Modifier,
    targetPcapName: String?,
    isPacketCaptureEnabled: Boolean,
) {
    val context = LocalContext.current

    // if user enabled packet capture show the steps to store the .pcap file otherwise only show end analysis button
    if (isPacketCaptureEnabled) {
        PacketCaptureEnabledContent(
            modifier,
            captureState,
            onStartCapture,
            onStopCapture,
            onStatusCheck,
            statusMessage,
            copiedPcapFileUri,
            onOpenFile,
            targetPcapName,
            context,
            webViewActions,
            analysisCallbacks
        )

    } else {
        PacketCaptureDisabledContent(webViewActions, context, analysisCallbacks)
    }


}

@Composable
private fun PacketCaptureDisabledContent(
    webViewActions: WebViewActions,
    context: Context,
    analysisCallbacks: AnalysisCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Icon
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Disabled status",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = "Packet Capture Disabled",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Description
        Text(
            text = "You choose to continue without Packet capture. End the analysis to proceed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // End analysis Button
        RoundCornerButton(
            modifier = Modifier
                .padding(end = 8.dp),
            onClick = {
                webViewActions.stopAnalysis {
                    showUncompletedAnalysisDialog(
                        context,
                        analysisCallbacks.hideDialog,
                        analysisCallbacks.showDialog,
                        analysisCallbacks.navigateToSessionList
                    )
                }
            },
            buttonText = stringResource(R.string.end_analysis),
        )
    }
}


@Composable
private fun PacketCaptureEnabledContent(
    modifier: Modifier,
    captureState: CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    copiedPcapFileUri: Uri?,
    onOpenFile: FileOpener,
    targetPcapName: String?,
    context: Context,
    webViewActions: WebViewActions,
    analysisCallbacks: AnalysisCallbacks
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), // Enable scrolling if content overflows
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Step 1: Stop Capture
        if (captureState == CaptureState.FILE_READY) {
            CompletedOverlay(
            ) {
                Step1Content(
                    captureState = captureState,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStatusCheck = onStatusCheck,
                    statusMessage = statusMessage
                )
            }
        } else {
            InProgressOverlay(
            ) {
                Step1Content(
                    captureState = captureState,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStatusCheck = onStatusCheck,
                    statusMessage = statusMessage
                )
            }
        }

        // Step 2: Select and Copy PCAP File
        if (copiedPcapFileUri != null) {
            CompletedOverlay(
            ) {
                Step2Content(
                    captureState = captureState,
                    onOpenFile = onOpenFile,
                    statusMessage = statusMessage,
                    copiedPcapFileUri = copiedPcapFileUri,
                    targetPcapName = targetPcapName
                )
            }
        } else {
            InProgressOverlay(
            ) {
                Step2Content(
                    captureState = captureState,
                    onOpenFile = onOpenFile,
                    statusMessage = statusMessage,
                    copiedPcapFileUri = copiedPcapFileUri,
                    targetPcapName = targetPcapName
                )
            }
        }

        // Step 3: End Analysis
        InProgressOverlay(
        ) {
            Step3Content(
                context = context,
                webViewActions = webViewActions,
                analysisCallbacks = analysisCallbacks,
                captureState = captureState,
                copiedPcapFileUri = copiedPcapFileUri

            )
        }
    }
}

// Step 1 Content
@Composable
private fun Step1Content(
    captureState: CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Step 1", style = MaterialTheme.typography.titleLarge)

        Text(
            text = "Please stop packet capture after finishing interacting with the WebView.",
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
                        text = "Capture State",
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

// Step 2 Content
@Composable
private fun Step2Content(
    captureState: CaptureState,
    onOpenFile: FileOpener,
    statusMessage: String,
    copiedPcapFileUri: Uri?,
    targetPcapName: String?
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Step 2", style = MaterialTheme.typography.titleLarge)

        Text(
            text = "Please press Open File and select this file: $targetPcapName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        RoundCornerButton(
            modifier = Modifier
                .padding(end = 8.dp),
            onClick = {
                val fileName = statusMessage.split(" ").lastOrNull { it.endsWith(".pcap") }
                    ?: "capture.pcap"
                onOpenFile(fileName)
            },
            buttonText = "Open File",
            trailingIcon = painterResource(id = R.drawable.folder_open_24px),
            enabled = captureState == CaptureState.FILE_READY
        )



        copiedPcapFileUri?.let {
            Text(
                text = "Filed copied as: $copiedPcapFileUri",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Step 3 Content
@Composable
private fun Step3Content(
    context: Context,
    webViewActions: WebViewActions,
    analysisCallbacks: AnalysisCallbacks,
    captureState: CaptureState,
    copiedPcapFileUri: Uri?
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Step 3", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "End analysis and save results.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        RoundCornerButton(
            modifier = Modifier
                .padding(end = 8.dp),
            onClick = {
                webViewActions.stopAnalysis {
                    showUncompletedAnalysisDialog(
                        context,
                        analysisCallbacks.hideDialog,
                        analysisCallbacks.showDialog,
                        analysisCallbacks.navigateToSessionList
                    )
                }
            },
            buttonText = stringResource(R.string.end_analysis),
            enabled = captureState == CaptureState.FILE_READY && copiedPcapFileUri != null,

            )
    }
}


@Composable
private fun StatusBadge(captureState: CaptureState) {
    val (textColor, backgroundColor) = when (captureState) {
        CaptureState.IDLE, CaptureState.STOPPED -> Color.Gray to Color.Gray.copy(alpha = 0.1f)
        CaptureState.STARTING, CaptureState.STOPPING -> Color.Blue to Color.Blue.copy(alpha = 0.1f)
        CaptureState.RUNNING -> Color.Green to Color.Green.copy(alpha = 0.1f)
        CaptureState.FILE_READY -> Color.Green to Color.Green.copy(alpha = 0.1f)
        CaptureState.ERROR -> Color.Red to Color.Red.copy(alpha = 0.1f)
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


@Composable
private fun ControlButton(
    captureState: CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label, onClick, isEnabled) = when (captureState) {
        CaptureState.RUNNING ->
            Quad(R.drawable.stop_24px, "Stop", onStopCapture, true)

        CaptureState.STOPPED, CaptureState.IDLE, CaptureState.FILE_READY ->
            Quad(R.drawable.play_arrow_24px, "Start", onStartCapture, true)

        else ->
            Quad(null, "Loading", onStatusCheck, false)
    }

    RoundCornerButton(
        modifier = modifier,
        onClick = { if (isEnabled) onClick() },
        buttonText = label,
        trailingIcon = icon?.let { painterResource(id = it) },
        enabled = isEnabled,
        isLoading = captureState in listOf(CaptureState.STARTING, CaptureState.STOPPING)
    )
}

// Helper class to disable ripple effect when button is disabled
private class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}

// Helper data class for button configuration
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


/**
 * Shows a dialog for uncompleted analysis warning.
 */
private fun showUncompletedAnalysisDialog(
    context: Context,
    hideDialog: () -> Unit,
    showDialog: (String, String, String, String, () -> Unit, () -> Unit) -> Unit,
    navigateToSessionList: () -> Unit
) {
    showDialog(
        context.getString(R.string.warning),
        context.getString(R.string.it_looks_like_you_still_have_no_full_internet_connection_please_complete_the_login_process_of_the_captive_portal_before_stopping_the_analysis),
        context.getString(R.string.stop_analysis_anyway),
        context.getString(R.string.dismiss),
        navigateToSessionList,
        hideDialog
    )
}

//Previews


@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
private fun PacketCaptureTabPreview_Idle() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
        override val showDialog =
            { _: String, _: String, _: String, _: String, _: () -> Unit, _: () -> Unit -> }
        override val hideDialog = {}
        override val navigateToSessionList = {}
    }

    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(
            webView: WebView,
            url: String,
            showToast: (String, ToastStyle) -> Unit
        ) {
        }

        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis(onUncompleted: () -> Unit) {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
    }

    AppTheme {
        PacketCaptureTab(
            captureState = CaptureState.IDLE,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Waiting...",
            onOpenFile = {},
            captureActions = captureActions,
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null,
            targetPcapName = "capture.pcap",
            isPacketCaptureEnabled = true,
        )
    }
}

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
private fun PacketCaptureTabPreview_Disabled() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
        override val showDialog =
            { _: String, _: String, _: String, _: String, _: () -> Unit, _: () -> Unit -> }
        override val hideDialog = {}
        override val navigateToSessionList = {}
    }

    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(
            webView: WebView,
            url: String,
            showToast: (String, ToastStyle) -> Unit
        ) {
        }

        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis(onUncompleted: () -> Unit) {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
    }

    AppTheme {
        PacketCaptureTab(
            captureState = CaptureState.IDLE,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Waiting...",
            onOpenFile = {},
            captureActions = captureActions,
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null,
            targetPcapName = "capture.pcap",
            isPacketCaptureEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
private fun PacketCaptureTabPreview_Completed() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
        override val showDialog =
            { _: String, _: String, _: String, _: String, _: () -> Unit, _: () -> Unit -> }
        override val hideDialog = {}
        override val navigateToSessionList = {}
    }

    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(
            webView: WebView,
            url: String,
            showToast: (String, ToastStyle) -> Unit
        ) {
        }

        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis(onUncompleted: () -> Unit) {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
    }

    AppTheme {
        PacketCaptureTab(
            captureState = CaptureState.FILE_READY,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Capturing competed.",
            onOpenFile = {},
            captureActions = captureActions,
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null,
            targetPcapName = "capture.pcap",
            isPacketCaptureEnabled = true,
        )
    }
}

