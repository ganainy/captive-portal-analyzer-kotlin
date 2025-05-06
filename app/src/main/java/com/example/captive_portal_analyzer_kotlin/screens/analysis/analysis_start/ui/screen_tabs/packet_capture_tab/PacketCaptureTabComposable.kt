
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.PcapDroidPacketCaptureStatus
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisCallbacks
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisInternetStatus
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.FileOpener
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states.PacketCaptureDisabledContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states.PacketCaptureEnabledContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states.PacketCaptureInitialContent
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme





@Composable
internal fun PacketCaptureTabComposable(
    captureState: MainViewModel.PcapDroidCaptureState,
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
    // val context = LocalContext.current // Not strictly needed in this top level anymore

    when (pcapDroidPacketCaptureStatus) {
        PcapDroidPacketCaptureStatus.INITIAL -> PacketCaptureInitialContent()

        PcapDroidPacketCaptureStatus.DISABLED -> PacketCaptureDisabledContent(
            webViewActions = webViewActions,
            analysisInternetStatus = analysisInternetStatus,
            updateSelectedTabIndex = updateSelectedTabIndex,
            markAnalysisAsComplete = markAnalysisAsComplete,
        )

        PcapDroidPacketCaptureStatus.ENABLED -> PacketCaptureEnabledContent(
            modifier = modifier,
            pcapdroidCaptureState = captureState,
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
}



// Previews - Updated to use the main composable with different states
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
private fun PacketCaptureTabPreview_InitialState() {
    // Mock objects for dependencies
    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(webView: WebView, url: String, showToast: (String, ToastStyle) -> Unit) {}
        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {}
    }
    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.PcapDroidCaptureState.IDLE, // Doesn't matter for INITIAL state display
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "", // Doesn't matter for INITIAL state display
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null, // Doesn't matter for INITIAL state display
            targetPcapName = null, // Doesn't matter for INITIAL state display
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.INITIAL, // Key status
            analysisInternetStatus = AnalysisInternetStatus.NO_INTERNET_ACCESS, // Doesn't matter for INITIAL state display
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected, // Doesn't matter for INITIAL state display
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
    // Mock objects for dependencies
    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(webView: WebView, url: String, showToast: (String, ToastStyle) -> Unit) {}
        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {}
    }
    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.PcapDroidCaptureState.IDLE, // Doesn't matter for DISABLED state display
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "", // Doesn't matter for DISABLED state display
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null, // Doesn't matter for DISABLED state display
            targetPcapName = null, // Doesn't matter for DISABLED state display
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.DISABLED, // Key status
            analysisInternetStatus = AnalysisInternetStatus.NO_INTERNET_ACCESS, // Relevant for the button in disabled state
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected, // Doesn't matter for DISABLED state display
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
    // Mock objects for dependencies
    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(webView: WebView, url: String, showToast: (String, ToastStyle) -> Unit) {}
        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {}
    }
    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.PcapDroidCaptureState.RUNNING, // Example enabled state
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Capturing...", // Example status
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = null, // Example file not yet copied
            targetPcapName = "capture.pcap", // Example target name
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.ENABLED, // Key status
            analysisInternetStatus = AnalysisInternetStatus.NO_INTERNET_ACCESS, // Example internet status
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected, // Example UI state
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
    // Mock objects for dependencies
    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) {}
        override suspend fun saveWebpageContent(webView: WebView, url: String, showToast: (String, ToastStyle) -> Unit) {}
        override fun takeScreenshot(webView: WebView, url: String) {}
        override suspend fun saveWebViewRequest(request: WebViewRequest) {}
        override fun updateShowedHint(showed: Boolean) {}
        override fun stopAnalysis() {}
        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) {}
        override fun forceStopAnalysis() {}
    }
    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = { _: String, _: ToastStyle -> }
    }

    AppTheme {
        PacketCaptureTabComposable(
            captureState = MainViewModel.PcapDroidCaptureState.FILE_READY, // Completed state for capture
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            statusMessage = "Capturing completed. File is ready: capture.pcap", // Status including file name
            onOpenFile = {},
            webViewActions = webViewActions,
            analysisCallbacks = analysisCallbacks,
            copiedPcapFileUri = "content://com.example.captive_portal_analyzer_kotlin.provider/shared_files/capture.pcap".toUri(), // File URI is available
            targetPcapName = "capture.pcap",
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.ENABLED, // Key status
            analysisInternetStatus = AnalysisInternetStatus.FULL_INTERNET_ACCESS, // Analysis is complete
            updateSelectedTabIndex = {},
            storePcapFileToSession = {},
            resetViewModelState = {},
            uiState = AnalysisUiState.CaptiveUrlDetected, // Example UI state
            markAnalysisAsComplete = {},
        )
    }
}