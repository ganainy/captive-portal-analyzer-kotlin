
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.core.net.toUri
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.PcapDroidPacketCaptureStatus
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.InProgressOverlay
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisInternetStatus
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisCallbacks
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.CaptureActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.FileOpener
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.screen_tabs.packet_capture_tab.composables.EndAnalysis_PacketCaptureDisabled
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.screen_tabs.packet_capture_tab.composables.EndAnalysis_PacketCaptureEnabled
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

@Composable
internal fun PacketCaptureTabComposable(
    captureState: MainViewModel.CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    onOpenFile: FileOpener,
    webViewActions: WebViewActions,
    analysisCallbacks: AnalysisCallbacks,
    copiedPcapFileUri: Uri?,
    modifier: Modifier = Modifier,
    targetPcapName: String?,
    pcapDroidPacketCaptureStatus: PcapDroidPacketCaptureStatus,
    analysisInternetStatus: AnalysisInternetStatus,
    updateSelectedTabIndex: (Int) -> Unit,
    storePcapFileToSession: () -> Unit,
    resetViewModelState: () -> Unit,
    uiState: AnalysisUiState,
    markAnalysisAsComplete: () -> Unit
) {
    val context = LocalContext.current
    // if user enabled packet capture show the steps to store the .pcap file otherwise only show end analysis button
    when (pcapDroidPacketCaptureStatus) {

        PcapDroidPacketCaptureStatus.ENABLED -> {
            PacketCaptureEnabledContent(
                modifier = modifier,
                captureState = captureState,
                onStartCapture = onStartCapture,
                onStopCapture = onStopCapture,
                onStatusCheck = onStatusCheck,
                statusMessage = statusMessage,
                copiedPcapFileUri = copiedPcapFileUri,
                onOpenFile = onOpenFile,
                targetPcapName = targetPcapName,
                webViewActions = webViewActions,
                analysisInternetStatus = analysisInternetStatus,
                updateSelectedTabIndex = updateSelectedTabIndex,
                storePcapFileToSession = storePcapFileToSession,
                markAnalysisAsComplete = markAnalysisAsComplete,
            )
        }

        PcapDroidPacketCaptureStatus.INITIAL -> PacketCaptureInitialContent()

        PcapDroidPacketCaptureStatus.DISABLED -> PacketCaptureDisabledContent(
            webViewActions = webViewActions,
            analysisInternetStatus = analysisInternetStatus,
            updateSelectedTabIndex = updateSelectedTabIndex,
            markAnalysisAsComplete = markAnalysisAsComplete,
        )

    }


}

@Composable
fun PacketCaptureInitialContent() {
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
            painter = painterResource(id = R.drawable.info),
            contentDescription = "Initial status",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = stringResource(R.string.select_capture_mode),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Description
        Text(
            text = stringResource(R.string.select_capture_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun PacketCaptureDisabledContent(
    webViewActions: WebViewActions,
    analysisInternetStatus: AnalysisInternetStatus,
    updateSelectedTabIndex: (Int) -> Unit,
    markAnalysisAsComplete: () -> Unit,
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
            text = stringResource(R.string.packet_capture_disabled),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Description
        Text(
            text = stringResource(R.string.you_choose_to_continue_without_packet_capture_end_the_analysis_to_proceed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        EndAnalysis_PacketCaptureDisabled(
            onStopAnalysis = webViewActions::stopAnalysis,
            analysisInternetStatus = analysisInternetStatus,
            updateSelectedTabIndex = updateSelectedTabIndex,
            markAnalysisAsComplete = markAnalysisAsComplete,
        )
    }
}


@Composable
private fun PacketCaptureEnabledContent(
    modifier: Modifier,
    captureState: MainViewModel.CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    copiedPcapFileUri: Uri?,
    onOpenFile: FileOpener,
    targetPcapName: String?,
    webViewActions: WebViewActions,
    analysisInternetStatus: AnalysisInternetStatus,
    updateSelectedTabIndex: (Int) -> Unit,
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


        // Step 1: End Analysis
        if (analysisInternetStatus == AnalysisInternetStatus.FULL_INTERNET_ACCESS) {
            CompletedOverlay(
            ) {
                StepOneContent(
                    webViewActions = webViewActions,
                    analysisInternetStatus = analysisInternetStatus,
                    onUpdateSelectedTabIndex = updateSelectedTabIndex,
                )
            }
        } else {
            InProgressOverlay(
            ) {
                StepOneContent(
                    webViewActions = webViewActions,
                    analysisInternetStatus = analysisInternetStatus,
                    onUpdateSelectedTabIndex = updateSelectedTabIndex,
                )
            }
        }

        // Step 2: Stop Capture
        if (captureState == MainViewModel.CaptureState.FILE_READY
            || captureState == MainViewModel.CaptureState.WRONG_FILE_PICKED //this is a step3 error but should affect step2 being completed
        ) {
            CompletedOverlay(
            ) {
                StepTwoContent(
                    captureState = captureState,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStatusCheck = onStatusCheck,
                    statusMessage = statusMessage,
                    analysisInternetStatus = analysisInternetStatus,
                )
            }
        } else {
            InProgressOverlay(
            ) {
                StepTwoContent(
                    captureState = captureState,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStatusCheck = onStatusCheck,
                    statusMessage = statusMessage,
                    analysisInternetStatus = analysisInternetStatus
                )
            }
        }

        // Step 3: Select and Copy PCAP File
        if (copiedPcapFileUri != null) {
            CompletedOverlay(
            ) {
                StepThreeContent(
                    captureState = captureState,
                    onOpenFile = onOpenFile,
                    statusMessage = statusMessage,
                    copiedPcapFileUri = copiedPcapFileUri,
                    storePcapFileToSession = storePcapFileToSession,
                    targetPcapName = targetPcapName
                )
            }
        } else {
            InProgressOverlay(
            ) {
                StepThreeContent(
                    captureState = captureState,
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
        val isFinishButtonEnabled = captureState == MainViewModel.CaptureState.FILE_READY &&
                copiedPcapFileUri != null && analysisInternetStatus == AnalysisInternetStatus.FULL_INTERNET_ACCESS

        RoundCornerButton(
            modifier = modifier,
            onClick = markAnalysisAsComplete,
            buttonText = stringResource(R.string.finish),
            trailingIcon = painterResource(id = R.drawable.sports_score_24px),
            enabled = isFinishButtonEnabled,
            isLoading = captureState in listOf(
                MainViewModel.CaptureState.STARTING,
                MainViewModel.CaptureState.STOPPING
            )
        )
    }
}

// Step 2 Content
@Composable
private fun StepTwoContent(
    captureState: MainViewModel.CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    analysisInternetStatus: AnalysisInternetStatus
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.step_2), style = MaterialTheme.typography.titleLarge)

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
            isWebViewAnalysisCompleted = analysisInternetStatus == AnalysisInternetStatus.FULL_INTERNET_ACCESS
        )
    }
}

// Step 3 Content
@Composable
private fun StepThreeContent(
    captureState: MainViewModel.CaptureState,
    onOpenFile: FileOpener,
    storePcapFileToSession: () -> Unit = {},
    statusMessage: String,
    copiedPcapFileUri: Uri?,
    targetPcapName: String?
) {

    //todo move this logic to viewmodel
    // Check if the capture state is FILE_READY and store the pcap file to session
    if (captureState == MainViewModel.CaptureState.FILE_READY) {
        storePcapFileToSession()
    }


    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.step_3), style = MaterialTheme.typography.titleLarge)

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
        if (captureState == MainViewModel.CaptureState.WRONG_FILE_PICKED) {
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
                val fileName = statusMessage.split(" ").lastOrNull { it.endsWith(".pcap") }
                    ?: "capture.pcap"
                onOpenFile(fileName)
            },
            buttonText = stringResource(R.string.open_file),
            trailingIcon = painterResource(id = R.drawable.folder_open_24px),
            enabled = captureState == MainViewModel.CaptureState.FILE_READY ||
                    captureState == MainViewModel.CaptureState.WRONG_FILE_PICKED, // keep button enabled if wrong file is picked to allow user to select another file
        )



        copiedPcapFileUri?.let {
            Text(
                text = stringResource(R.string.filed_copied_as, copiedPcapFileUri),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Step 1 Content
@Composable
private fun StepOneContent(
    webViewActions: WebViewActions,
    analysisInternetStatus: AnalysisInternetStatus,
    onUpdateSelectedTabIndex: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.step_1), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.end_analysis_and_save_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        EndAnalysis_PacketCaptureEnabled(
            onStopAnalysis = webViewActions::stopAnalysis,
            onForceStopAnalysis = webViewActions::forceStopAnalysis,
            analysisInternetStatus = analysisInternetStatus,
            onUpdateSelectedTabIndex = onUpdateSelectedTabIndex,
        )

    }
}


@Composable
private fun StatusBadge(captureState: MainViewModel.CaptureState) {
    val (textColor, backgroundColor) = when (captureState) {
        MainViewModel.CaptureState.IDLE, MainViewModel.CaptureState.STOPPED -> Color.Gray to Color.Gray.copy(
            alpha = 0.1f
        )

        MainViewModel.CaptureState.STARTING, MainViewModel.CaptureState.STOPPING -> Color.Blue to Color.Blue.copy(
            alpha = 0.1f
        )

        MainViewModel.CaptureState.RUNNING -> Color.Green to Color.Green.copy(alpha = 0.1f)
        MainViewModel.CaptureState.FILE_READY -> Color.Green to Color.Green.copy(alpha = 0.1f)
        MainViewModel.CaptureState.WRONG_FILE_PICKED -> Color.Red to Color.Red.copy(alpha = 0.1f)
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
    captureState: MainViewModel.CaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    modifier: Modifier = Modifier,
    isWebViewAnalysisCompleted: Boolean
) {
    val (icon, label, onClick, isEnabled) = when (captureState) {
        MainViewModel.CaptureState.RUNNING ->
            Quad(R.drawable.stop_24px, stringResource(R.string.stop), onStopCapture, true)

        MainViewModel.CaptureState.STOPPED, MainViewModel.CaptureState.IDLE, MainViewModel.CaptureState.FILE_READY ->
            Quad(R.drawable.play_arrow_24px, stringResource(R.string.start), onStartCapture, true)

        else ->
            Quad(null, stringResource(R.string.loading), onStatusCheck, false)
    }

    RoundCornerButton(
        modifier = modifier,
        onClick = { if (isEnabled) onClick() },
        buttonText = label,
        trailingIcon = icon?.let { painterResource(id = it) },
        enabled = isEnabled && isWebViewAnalysisCompleted,
        isLoading = captureState in listOf(
            MainViewModel.CaptureState.STARTING,
            MainViewModel.CaptureState.STOPPING
        )
    )
}

// Helper data class for button configuration
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


//Previews

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
private fun PacketCaptureTabPreview_InitialState() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
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
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {

        }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.CaptureState.FILE_READY,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Capturing completed.",
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null,
            targetPcapName = "capture.pcap",
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.INITIAL,
            analysisInternetStatus = AnalysisInternetStatus.FULL_INTERNET_ACCESS,
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected,
            markAnalysisAsComplete = {},
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
private fun PacketCaptureTabPreview_CaptureEnabled() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
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
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {

        }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.CaptureState.IDLE,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Waiting...",
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null,
            targetPcapName = "capture.pcap",
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.ENABLED,
            analysisInternetStatus = AnalysisInternetStatus.NO_INTERNET_ACCESS,
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected,
            markAnalysisAsComplete = {},
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
private fun PacketCaptureTabPreview_CaptureDisabled() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
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
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {

        }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.CaptureState.IDLE,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Waiting...",
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null,
            targetPcapName = "capture.pcap",
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.DISABLED,
            analysisInternetStatus = AnalysisInternetStatus.NO_INTERNET_ACCESS,
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected,
            markAnalysisAsComplete = {},
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
private fun PacketCaptureTabPreview_CaptureCompleted() {

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() {}
        override fun requestStatusUpdate() {}
    }

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
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
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {

        }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.CaptureState.FILE_READY,
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Capturing completed.",
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = "copied_capture.pcap".toUri(),
         targetPcapName = "capture.pcap",
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.ENABLED,
            analysisInternetStatus = AnalysisInternetStatus.FULL_INTERNET_ACCESS,
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected,
            markAnalysisAsComplete = {},
        )
    }
}

