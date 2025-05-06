package com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui


import NetworkSessionRepository
import PacketCaptureTabComposable
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.PcapDroidPacketCaptureStatus
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.OnStartEffect
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.webview_tab.WebViewTabComposable
import com.example.captive_portal_analyzer_kotlin.screens.analysis.testing.TestingWebViewClient
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager

// Configuration data classes remain unchanged
data class AnalysisScreenConfig(
    val repository: NetworkSessionRepository,
    val sessionManager: NetworkSessionManager,
    val mainViewModel: MainViewModel,
)

data class NavigationConfig(
    val onNavigateToSessionList: () -> Unit,
    val onNavigateToManualConnect: () -> Unit,
    val onNavigateToSetupPCAPDroid: () -> Unit,
    val onNavigateToScreenshotFlagging: () -> Unit
)

data class IntentLaunchConfig(
    val onStartIntent: IntentLauncher,
    val onStopIntent: IntentLauncher,
    val onStatusIntent: IntentLauncher,
    val onOpenFile: FileOpener
)

data class WebViewContentConfig(
    val webViewType: WebViewType,
    val portalUrl: String?,
    val showedHint: Boolean,
    val contentPadding: PaddingValues,
    val captureState: MainViewModel.PcapDroidCaptureState,
    val statusMessage: String,
    val targetPcapName: String?
)

// Callback interfaces remain unchanged
interface AnalysisCallbacks {
    val showToast: (String, ToastStyle) -> Unit
}

interface WebViewActions {
    suspend fun saveWebResourceRequest(request: WebResourceRequest?)
    suspend fun saveWebpageContent(
        webView: WebView,
        url: String,
        showToast: (String, ToastStyle) -> Unit
    )

    fun takeScreenshot(webView: WebView, url: String)
    suspend fun saveWebViewRequest(request: WebViewRequest)
    fun updateShowedHint(showed: Boolean)
    fun stopAnalysis()
    fun switchWebViewType(showToast: (String, ToastStyle) -> Unit)
}

interface CaptureActions {
    fun requestStopCapture()
    fun requestStatusUpdate()
}

typealias IntentLauncher = (Intent) -> Unit
typealias FileOpener = (fileName: String) -> Unit

@Composable
fun AnalysisScreen(
    screenConfig: AnalysisScreenConfig,
    navigationConfig: NavigationConfig,
    intentLaunchConfig: IntentLaunchConfig
) {


  // Only call the status check once during initialization to get the current capture status of the PCAPDroid app
  LaunchedEffect(Unit) {
      intentLaunchConfig.onStatusIntent(screenConfig.mainViewModel.createGetStatusIntent())
  }

    val captureState by screenConfig.mainViewModel.captureState.collectAsStateWithLifecycle()
    val statusMessage by screenConfig.mainViewModel.statusMessage.collectAsStateWithLifecycle()
    val targetPcapName by screenConfig.mainViewModel.targetPcapName.collectAsStateWithLifecycle()
    val copiedPcapFileUri by screenConfig.mainViewModel.copiedPcapFileUri.collectAsStateWithLifecycle()
    val selectedTabIndex by screenConfig.mainViewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val pcapDroidPacketCaptureStatus by screenConfig.mainViewModel.pcapDroidPacketCaptureStatus.collectAsStateWithLifecycle()


    val analysisViewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = screenConfig.repository,
            sessionManager = screenConfig.sessionManager,
            showToast = screenConfig.mainViewModel::showToast,
            mainViewModel = screenConfig.mainViewModel
        )
    )

    val uiData by analysisViewModel.uiData.collectAsState()


    val uiState by analysisViewModel.uiState.collectAsState()

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = screenConfig.mainViewModel::showToast
    }

    val webViewActions = object : WebViewActions {
        override suspend fun saveWebResourceRequest(request: WebResourceRequest?) =
            analysisViewModel.saveWebResourceRequest(request)

        override suspend fun saveWebpageContent(
            webView: WebView,
            url: String,
            showToast: (String, ToastStyle) -> Unit
        ) =
            analysisViewModel.saveWebpageContent(webView, url, showToast)

        override fun takeScreenshot(webView: WebView, url: String) =
            analysisViewModel.takeScreenshot(webView, url)

        override suspend fun saveWebViewRequest(request: WebViewRequest) =
            analysisViewModel.saveWebViewRequest(request)

        override fun updateShowedHint(showed: Boolean) =
            analysisViewModel.updateShowedHint(showed)

        override fun stopAnalysis() =
            analysisViewModel.stopAnalysis(navigationConfig.onNavigateToScreenshotFlagging)


        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) =
            analysisViewModel.switchWebViewType(showToast)
    }


    AnalysisScreenContent(
        captureState = captureState,
        statusMessage = statusMessage,
        targetPcapName = targetPcapName,
        uiState = uiState,
        uiData = uiData,
        analysisCallbacks = analysisCallbacks,
        webViewActions = webViewActions,
        onOpenFile = intentLaunchConfig.onOpenFile,
        copiedPcapFileUri = copiedPcapFileUri,
        onNavigateToManualConnect = navigationConfig.onNavigateToManualConnect,
        onNavigateToSetupPCAPDroid = navigationConfig.onNavigateToSetupPCAPDroid,
        getCaptivePortalAddress = { analysisViewModel.getCaptivePortalAddress(analysisCallbacks.showToast) },
        onStartCapture = {
            screenConfig.mainViewModel.requestStartCapture()
            intentLaunchConfig.onStartIntent(screenConfig.mainViewModel.createStartIntent())
        },
        onStopCapture = {
            screenConfig.mainViewModel.requestStopCapture()
            intentLaunchConfig.onStopIntent(screenConfig.mainViewModel.createStopIntent())
        },
        onStatusCheck = {
            intentLaunchConfig.onStatusIntent(screenConfig.mainViewModel.createGetStatusIntent())
        },
        selectedTabIndex = selectedTabIndex,
        updateSelectedTabIndex = screenConfig.mainViewModel::setSelectedTabIndex,
        updatePcapDroidPacketCaptureStatus = { pcapDroidPacketCaptureStatus ->
            screenConfig.mainViewModel.updatePcapDroidPacketCaptureStatus(
                pcapDroidPacketCaptureStatus
            )
        },
        pcapDroidPacketCaptureStatus = pcapDroidPacketCaptureStatus,
        storePcapFileToSession =
            analysisViewModel::storePcapFilePathInTheSession,
        isPCAPDroidInstalled = analysisViewModel::isPcapDroidAppInstalled,
        markAnalysisAsComplete = analysisViewModel::markAnalysisAsComplete,
    )

    // Handle navigation to the next screen when analysis is complete
    LaunchedEffect(uiState) {
        if (uiState == AnalysisUiState.AnalysisCompleteNavigateToNextScreen) {
            analysisViewModel.resetViewModelState()
            screenConfig.mainViewModel.resetPacketCaptureState()
            navigationConfig.onNavigateToScreenshotFlagging()
        }
    }

}

@Composable
private fun AnalysisScreenContent(
    captureState: MainViewModel.PcapDroidCaptureState,
    statusMessage: String,
    targetPcapName: String?,
    uiState: AnalysisUiState,
    uiData: AnalysisUiData,
    analysisCallbacks: AnalysisCallbacks,
    webViewActions: WebViewActions,
    onOpenFile: FileOpener,
    copiedPcapFileUri: Uri?,
    selectedTabIndex: Int,
    pcapDroidPacketCaptureStatus: PcapDroidPacketCaptureStatus,
    onNavigateToManualConnect: () -> Unit,
    getCaptivePortalAddress: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    updateSelectedTabIndex: (Int) -> Unit,
    onNavigateToSetupPCAPDroid: () -> Unit,
    updatePcapDroidPacketCaptureStatus: (PcapDroidPacketCaptureStatus) -> Unit,
    storePcapFileToSession:  () -> Unit,
    isPCAPDroidInstalled : () -> Boolean,
    markAnalysisAsComplete : () -> Unit,
) {

    val tabTitles = listOf("WebView", "Packet Capture")

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { updateSelectedTabIndex(index) },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> WebViewTabComposable(
                    uiState = uiState,
                    uiData = uiData,
                    captureState = captureState,
                    statusMessage = statusMessage,
                    targetPcapName = targetPcapName,
                    analysisCallbacks = analysisCallbacks,
                    webViewActions = webViewActions,
                    updateSelectedTabIndex = updateSelectedTabIndex,
                    getCaptivePortalAddress = getCaptivePortalAddress,
                    onNavigateToManualConnect = onNavigateToManualConnect,
                    onStartCapture = onStartCapture,
                    onStatusCheck = onStatusCheck,
                    onNavigateToSetupPCAPDroid = onNavigateToSetupPCAPDroid,
                    updatePcapDroidPacketCaptureStatus = updatePcapDroidPacketCaptureStatus,
                    isPCAPDroidInstalled = isPCAPDroidInstalled
                )

                1 -> PacketCaptureTabComposable(
                    captureState = captureState,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStatusCheck = onStatusCheck,
                    statusMessage = statusMessage,
                    onOpenFile = onOpenFile,
                    webViewActions = webViewActions,
                    copiedPcapFileUri = copiedPcapFileUri,
                    targetPcapName = targetPcapName,
                    pcapDroidPacketCaptureStatus = pcapDroidPacketCaptureStatus,
                    updateSelectedTabIndex = updateSelectedTabIndex,
                    storePcapFileToSession = storePcapFileToSession,
                    markAnalysisAsComplete = markAnalysisAsComplete,
                )
            }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferenceSetupContent(
    captureState: MainViewModel.PcapDroidCaptureState,
    isPCAPDroidInstalled: () -> Boolean,
    onStartCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    getCaptivePortalAddress: () -> Unit,
    onNavigateToSetupPCAPDroid: () -> Unit,
    updatePcapDroidPacketCaptureStatus: (PcapDroidPacketCaptureStatus) -> Unit,
    modifier: Modifier = Modifier
) {

    OnStartEffect {
        // Code to run when the app starts, if user minimized the app and comes back recheck
        // capture status just to be extra sure
        onStatusCheck()
    }

    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)

    ) {
        // Header Section
        Text(
            text = stringResource(R.string.choose_your_analysis_mode),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.this_app_offers_two_analysis_modes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.default_uses_custom_webview) +
                            stringResource(R.string.advanced_uses_pcapdroid_for_packet_capture_custom_webview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // PCAPDroid Setup Section
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.in_order_to_use_the_advanced_mode_please_make_sure_to_if_you_haven_t_already))
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(stringResource(R.string.setup_pcapdroid))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNavigateToSetupPCAPDroid() }
        )

        if (!isPCAPDroidInstalled()){
            HintTextWithIcon(
                hint = stringResource(R.string.please_install_setup_pcapdroid_from_the_link_above_to_use_the_advanced_mode),
            )
        }

        // Buttons Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            // show warning to user if a capture is already running
            if (captureState == MainViewModel.PcapDroidCaptureState.RUNNING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HintTextWithIcon(
                            hint = stringResource(R.string.please_stop_running_pcap_droid_capture_before_continuing),
                            modifier = Modifier.weight(1f),
                            color = Color.Red,
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.refresh_24px),
                            contentDescription = "Refresh status",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                                .clickable { onStatusCheck() },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
            }

            // Continue with Packet Capture Button
            RoundCornerButton(
                onClick = {
                        onStartCapture()
                        updatePcapDroidPacketCaptureStatus(PcapDroidPacketCaptureStatus.PCAPDROID_CAPTURE_ENABLED)
                        getCaptivePortalAddress()
                },
                buttonText = stringResource(R.string.continue_with_packet_capture),
                trailingIcon = painterResource(id = R.drawable.arrow_forward_ios_24px),
                modifier = Modifier.fillMaxWidth(),
                enabled = captureState != MainViewModel.PcapDroidCaptureState.RUNNING && isPCAPDroidInstalled()
            )

            // Continue without Packet Capture Button
            GhostButton(
                onClick = { getCaptivePortalAddress()
                    updatePcapDroidPacketCaptureStatus(PcapDroidPacketCaptureStatus.PCAPDROID_CAPTURE_DISABLED)

                          },
                buttonText = stringResource(R.string.continue_without_packet_capture),
                modifier = Modifier
                    .fillMaxWidth()
                    ,
            )
        }

    }
}




/**
 * Testing WebView for debug mode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TestingWebView(modifier: Modifier = Modifier) {
    Box {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                setWebContentsDebuggingEnabled(true)
                setWebViewClient(TestingWebViewClient())
                loadUrl("http://httpforever.com/")
            }
        })
    }
}


/**
 * Error state UI.
 */
@Composable
internal fun AnalysisError(
    uiState: AnalysisUiState,
    onRetry: () -> Unit,
    onNavigateToManualConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val error = uiState as AnalysisUiState.Error
            var text = ""
            var hint = ""
            when (error.type) {
                AnalysisUiState.ErrorType.CannotDetectCaptiveUrl -> {
                    text = stringResource(R.string.couldnt_detect_captive_url)
                    hint = stringResource(R.string.are_you_sure_current_network_has_captive_portal)
                }

                AnalysisUiState.ErrorType.Unknown -> text =
                    stringResource(R.string.something_went_wrong)

                AnalysisUiState.ErrorType.NoInternet -> text =
                    stringResource(R.string.no_internet_connection_detected)

                AnalysisUiState.ErrorType.Timeout -> text =
                    stringResource(R.string.timeout_while_loading_captive_portal)

                AnalysisUiState.ErrorType.NetworkError -> text =
                    stringResource(R.string.network_error_while_loading_captive_portal)

                AnalysisUiState.ErrorType.PermissionDenied -> text =
                    stringResource(R.string.permission_error_while_loading_captive_portal)

                AnalysisUiState.ErrorType.InvalidState -> text =
                    stringResource(R.string.invalid_state_error_while_loading_captive_portal)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            HintTextWithIcon(
                hint = hint,
                rowAllignment = Alignment.Center,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundCornerButton(
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp),
                    onClick = { onNavigateToManualConnect() },
                    buttonText = stringResource(R.string.connect_to_another_network)
                )
                Spacer(modifier = Modifier.width(16.dp))
                GhostButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    onClick = { onRetry() },
                    buttonText = stringResource(R.string.retry)
                )
            }
        }
    }
}







// Previews

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
private fun AnalysisScreenContentPreview_Success() {
    AppTheme {
        val captureState = MainViewModel.PcapDroidCaptureState.RUNNING
        val statusMessage = "Capturing to file..."
        val targetPcapName = "captive_portal_capture.pcap"

        val analysisCallbacks = object : AnalysisCallbacks {
            override val showToast = { _: String, _: ToastStyle -> }
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
        }

        val captureActions = object : CaptureActions {
            override fun requestStopCapture() {}
            override fun requestStatusUpdate() {}
        }

        val uiState = AnalysisUiState.CaptiveUrlDetected
        val uiData = AnalysisUiData(
            portalUrl = "https://captive.example.com",
            webViewType = WebViewType.NormalWebView,
            showedHint = false
        )

        AnalysisScreenContent(
            captureState = captureState,
            statusMessage = statusMessage,
            targetPcapName = targetPcapName,
            uiState = uiState,
            uiData = uiData,
            analysisCallbacks = analysisCallbacks,
            webViewActions = webViewActions,
            onOpenFile = {},
            copiedPcapFileUri = null,
            selectedTabIndex = 0,
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.PCAPDROID_CAPTURE_ENABLED,
            onNavigateToManualConnect = {},
            getCaptivePortalAddress = {},
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            updateSelectedTabIndex = {},
            onNavigateToSetupPCAPDroid = {},
            updatePcapDroidPacketCaptureStatus = {},
            storePcapFileToSession = {},
            isPCAPDroidInstalled = { true },
            markAnalysisAsComplete =  {},
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
private fun AnalysisScreenContentPreview_Error() {
    AppTheme {
        val captureState = MainViewModel.PcapDroidCaptureState.STOPPED
        val statusMessage = "Capture stopped"
        val targetPcapName = null

        val analysisCallbacks = object : AnalysisCallbacks {
            override val showToast = { _: String, _: ToastStyle -> }
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
        }

        val captureActions = object : CaptureActions {
            override fun requestStopCapture() {}
            override fun requestStatusUpdate() {}
        }

        val uiState = AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
        val uiData = AnalysisUiData(
            portalUrl = null,
            webViewType = WebViewType.NormalWebView,
            showedHint = false
        )

        AnalysisScreenContent(
            captureState = captureState,
            statusMessage = statusMessage,
            targetPcapName = targetPcapName,
            uiState = uiState,
            uiData = uiData,
            analysisCallbacks = analysisCallbacks,
            webViewActions = webViewActions,
            onOpenFile = {},
            copiedPcapFileUri = null,
            selectedTabIndex = 0,
            pcapDroidPacketCaptureStatus = PcapDroidPacketCaptureStatus.PCAPDROID_CAPTURE_ENABLED,
            onNavigateToManualConnect = {},
            getCaptivePortalAddress = {},
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            updateSelectedTabIndex = {},
            onNavigateToSetupPCAPDroid = {},
            updatePcapDroidPacketCaptureStatus = {},
            storePcapFileToSession = {},
            isPCAPDroidInstalled = {true},
            markAnalysisAsComplete =  {},
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
private fun PreferenceSetupContentPreview_CaptureIdle() {
    AppTheme {
        PreferenceSetupContent(
            captureState = MainViewModel.PcapDroidCaptureState.IDLE,
            onStartCapture = {},
            getCaptivePortalAddress = {},
            onNavigateToSetupPCAPDroid = {},
            updatePcapDroidPacketCaptureStatus = {},
            isPCAPDroidInstalled ={true},
            onStatusCheck = {},
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
private fun PreferenceSetupContentPreview_CaptureRunning() {
    AppTheme {
        PreferenceSetupContent(
            captureState = MainViewModel.PcapDroidCaptureState.RUNNING,
            onStartCapture = {},
            getCaptivePortalAddress = {},
            onNavigateToSetupPCAPDroid = {},
            updatePcapDroidPacketCaptureStatus = {},
            isPCAPDroidInstalled ={true},
            onStatusCheck = {},
        )
    }
}