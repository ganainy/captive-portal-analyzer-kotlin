package com.example.captive_portal_analyzer_kotlin.screens.analysis


import NetworkSessionRepository
import PacketCaptureTab
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.BuildConfig
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.MockWebView
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Configuration data classes remain unchanged
data class AnalysisScreenConfig(
    val repository: NetworkSessionRepository,
    val sessionManager: NetworkSessionManager,
    val mainViewModel: MainViewModel,
)

data class NavigationConfig(
    val onNavigateToSessionList: () -> Unit,
    val onNavigateToManualConnect: () -> Unit,
    val onNavigateToSetupPCAPDroidScreen: () -> Unit
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
    val captureState: MainViewModel.CaptureState,
    val statusMessage: String,
    val targetPcapName: String?
)

// Callback interfaces remain unchanged
interface AnalysisCallbacks {
    val showToast: (String, ToastStyle) -> Unit
    val navigateToSessionList: () -> Unit
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
    fun forceStopAnalysis()
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

    // for testing purposes only, set packet capture to always be on
    if (BuildConfig.IS_APP_IN_DEBUG_MODE) {
        screenConfig.mainViewModel.updateIsPacketCaptureEnabled(true)
    }

    val captureState by screenConfig.mainViewModel.captureState.collectAsStateWithLifecycle()
    val statusMessage by screenConfig.mainViewModel.statusMessage.collectAsStateWithLifecycle()
    val targetPcapName by screenConfig.mainViewModel.targetPcapName.collectAsStateWithLifecycle()
    val copiedPcapFileUri by screenConfig.mainViewModel.copiedPcapFileUri.collectAsStateWithLifecycle()
    val selectedTabIndex by screenConfig.mainViewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val isPacketCaptureEnabled by screenConfig.mainViewModel.isPacketCaptureEnabled.collectAsStateWithLifecycle()


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
    val analysisStatus: AnalysisStatus =  uiData.analysisStatus


    val uiState by analysisViewModel.uiState.collectAsState()

    val analysisCallbacks = object : AnalysisCallbacks {
        override val showToast = screenConfig.mainViewModel::showToast
        override val navigateToSessionList = navigationConfig.onNavigateToSessionList
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
            analysisViewModel.stopAnalysis()

        override fun forceStopAnalysis() =
            analysisViewModel.forceStopAnalysis()

        override fun switchWebViewType(showToast: (String, ToastStyle) -> Unit) =
            analysisViewModel.switchWebViewType(showToast)
    }

    val captureActions = object : CaptureActions {
        override fun requestStopCapture() = screenConfig.mainViewModel.requestStopCapture()
        override fun requestStatusUpdate() = screenConfig.mainViewModel.requestGetStatus()
    }

    AnalysisScreenContent(
        captureState = captureState,
        statusMessage = statusMessage,
        targetPcapName = targetPcapName,
        uiState = uiState,
        uiData = uiData,
        analysisCallbacks = analysisCallbacks,
        webViewActions = webViewActions,
        captureActions = captureActions,
        onOpenFile = intentLaunchConfig.onOpenFile,
        copiedPcapFileUri = copiedPcapFileUri,
        onNavigateToManualConnect = navigationConfig.onNavigateToManualConnect,
        onNavigateToSetupPCAPDroid = navigationConfig.onNavigateToSetupPCAPDroidScreen,
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
        updateIsPacketCaptureEnabled = { isEnabled ->
            screenConfig.mainViewModel.updateIsPacketCaptureEnabled(isEnabled)
        },
        isPacketCaptureEnabled = isPacketCaptureEnabled,
        analysisStatus = analysisStatus
    )
}

@Composable
private fun AnalysisScreenContent(
    captureState: MainViewModel.CaptureState,
    statusMessage: String,
    targetPcapName: String?,
    uiState: AnalysisUiState,
    uiData: AnalysisUiData,
    analysisCallbacks: AnalysisCallbacks,
    webViewActions: WebViewActions,
    captureActions: CaptureActions,
    onOpenFile: FileOpener,
    copiedPcapFileUri: Uri?,
    selectedTabIndex: Int,
    isPacketCaptureEnabled: Boolean,
    onNavigateToManualConnect: () -> Unit,
    getCaptivePortalAddress: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    updateSelectedTabIndex: (Int) -> Unit,
    onNavigateToSetupPCAPDroid: () -> Unit,
    updateIsPacketCaptureEnabled: (Boolean) -> Unit,
    analysisStatus: AnalysisStatus,
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
                0 -> Box(modifier = Modifier.fillMaxSize()) {
                    val effectiveUiState =
                        if (BuildConfig.IS_APP_IN_DEBUG_MODE) AnalysisUiState.CaptiveUrlDetected else uiState
                    when (effectiveUiState) {
                        is AnalysisUiState.Loading -> LoadingIndicator(
                            message = stringResource((uiState as AnalysisUiState.Loading).messageStringResource),
                            modifier = Modifier
                        )

                        is AnalysisUiState.CaptiveUrlDetected -> CaptivePortalWebsiteContent(
                            config = WebViewContentConfig(
                                webViewType = uiData.webViewType,
                                portalUrl = uiData.portalUrl,
                                showedHint = uiData.showedHint,
                                contentPadding = PaddingValues(0.dp),
                                captureState = captureState,
                                statusMessage = statusMessage,
                                targetPcapName = targetPcapName
                            ),
                            analysisCallbacks = analysisCallbacks,
                            webViewActions = webViewActions,
                            captureActions = captureActions,
                            selectedTabIndex = selectedTabIndex,
                            setSelectTabIndex = updateSelectedTabIndex

                        )

                        is AnalysisUiState.Error -> AnalysisError(
                            contentPadding = PaddingValues(0.dp),
                            uiState = uiState,
                            onRetry = getCaptivePortalAddress,
                            analysisCallbacks = analysisCallbacks,
                            onNavigateToManualConnect = onNavigateToManualConnect
                        )

                        AnalysisUiState.AnalysisComplete -> analysisCallbacks.navigateToSessionList()
                        AnalysisUiState.PreferenceSetup -> PreferenceSetupContent(
                            captureState = captureState,
                            onStartCapture = onStartCapture,
                            getCaptivePortalAddress = getCaptivePortalAddress,
                            onNavigateToSetupPCAPDroid = onNavigateToSetupPCAPDroid,
                            updateIsPacketCaptureEnabled = { isEnabled ->
                                updateIsPacketCaptureEnabled(isEnabled)
                            }
                        )
                    }
                }

                1 -> PacketCaptureTab(
                    captureState = captureState,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStatusCheck = onStatusCheck,
                    statusMessage = statusMessage,
                    onOpenFile = onOpenFile,
                    captureActions = captureActions,
                    webViewActions = webViewActions,
                    analysisCallbacks = analysisCallbacks,
                    copiedPcapFileUri = copiedPcapFileUri,
                    targetPcapName = targetPcapName,
                    isPacketCaptureEnabled = isPacketCaptureEnabled,
                    analysisStatus = analysisStatus,
                    updateSelectedTabIndex = updateSelectedTabIndex,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferenceSetupContent(
    captureState: MainViewModel.CaptureState,
    onStartCapture: () -> Unit,
    getCaptivePortalAddress: () -> Unit,
    onNavigateToSetupPCAPDroid: () -> Unit,
    updateIsPacketCaptureEnabled: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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

        // Buttons Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RoundCornerButton(
                onClick = {
                    if (captureState == MainViewModel.CaptureState.IDLE) {
                        onStartCapture()
                        updateIsPacketCaptureEnabled(true)
                    }
                },
                buttonText = stringResource(R.string.continue_with_packet_capture),
                trailingIcon = painterResource(id = R.drawable.arrow_forward_ios_24px),
                modifier = Modifier.fillMaxWidth(),
                enabled = captureState != MainViewModel.CaptureState.RUNNING
            )

            GhostButton(
                onClick = { getCaptivePortalAddress()
                    updateIsPacketCaptureEnabled(false)

                          },
                text = stringResource(R.string.continue_without_packet_capture),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        // Capture State Indicator
        if (captureState == MainViewModel.CaptureState.RUNNING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.capturing),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Handle captive portal detection
        LaunchedEffect(captureState) {
            if (captureState == MainViewModel.CaptureState.RUNNING) {
                getCaptivePortalAddress()
            }
        }
    }
}


@Composable
private fun CaptivePortalWebsiteContent(
    config: WebViewContentConfig,
    analysisCallbacks: AnalysisCallbacks,
    webViewActions: WebViewActions,
    captureActions: CaptureActions,
    selectedTabIndex: Int,
    setSelectTabIndex: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webView = remember { WebView(context) }

    DisposableEffect(webView) { onDispose { webView.destroy() } }

    WebViewInteractionContent(
        config = config,
        webView = webView,
        coroutineScope = coroutineScope,
        analysisCallbacks = analysisCallbacks,
        webViewActions = webViewActions,
        selectedTabIndex = selectedTabIndex,
        setSelectTabIndex = setSelectTabIndex
    )
}

@Composable
private fun WebViewInteractionContent(
    config: WebViewContentConfig,
    webView: WebView,
    coroutineScope: CoroutineScope,
    analysisCallbacks: AnalysisCallbacks,
    webViewActions: WebViewActions,
    selectedTabIndex: Int,
    setSelectTabIndex: (Int) -> Unit
) {
    val effectiveWebViewType =
        if (BuildConfig.IS_APP_IN_DEBUG_MODE) WebViewType.TestingWebView else config.webViewType

    Column(modifier = Modifier.fillMaxSize()) {
        HintTextWithIcon(
            hint = if (effectiveWebViewType == WebViewType.CustomWebView)
                stringResource(R.string.hint_backup_webview)
            else stringResource(R.string.hint_custom_webview)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(1.dp)
        ) {
            Text(
                text = if (effectiveWebViewType == WebViewType.CustomWebView) stringResource(R.string.custom_webview)
                else stringResource(R.string.normal_webview),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (LocalInspectionMode.current) {
                MockWebView()
            } else {
                when (effectiveWebViewType) {
                    WebViewType.NormalWebView -> NormalWebView(
                        portalUrl = config.portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                webViewActions.saveWebResourceRequest(
                                    request
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(config.contentPadding),
                        captureAndSaveContent = { wv, content ->
                            coroutineScope.launch {
                                webViewActions.saveWebpageContent(
                                    wv,
                                    content,
                                    analysisCallbacks.showToast
                                )
                            }
                        },
                        takeScreenshot = webViewActions::takeScreenshot
                    )

                    WebViewType.CustomWebView -> CustomWebView(
                        portalUrl = config.portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                webViewActions.saveWebViewRequest(
                                    request
                                )
                            }
                        },
                        takeScreenshot = webViewActions::takeScreenshot,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(config.contentPadding),
                        captureAndSaveContent = { wv, content ->
                            coroutineScope.launch {
                                webViewActions.saveWebpageContent(
                                    wv,
                                    content,
                                    analysisCallbacks.showToast
                                )
                            }
                        },
                        showedHint = config.showedHint,
                        updateShowedHint = webViewActions::updateShowedHint
                    )

                    WebViewType.TestingWebView -> TestingWebView()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GhostButton(
                modifier = Modifier.weight(1f),
                onClick = { webViewActions.switchWebViewType(analysisCallbacks.showToast) },
                text = stringResource(R.string.switch_browser_type)
            )

            RoundCornerButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    setSelectTabIndex(1)  // Switch to Packet Capture tab
                },
                buttonText = stringResource(R.string.continuee),
                trailingIcon =painterResource(id = R.drawable.arrow_forward_ios_24px)
            )
        }
    }
}

/**
 * Testing WebView for debug mode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TestingWebView() {
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

        if (BuildConfig.IS_APP_IN_DEBUG_MODE) {
            Text(
                text = "DEBUG MODE: Testing WebView Active",
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }
}


/**
 * Error state UI.
 */
@Composable
private fun AnalysisError(
    contentPadding: PaddingValues,
    uiState: AnalysisUiState,
    onRetry: () -> Unit,
    analysisCallbacks: AnalysisCallbacks,
    onNavigateToManualConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
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
            HintTextWithIcon(hint = hint, rowAllignment = Alignment.Center)
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
                GhostButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    onClick = { onRetry() },
                    text = stringResource(R.string.retry)
                )
            }
        }
    }
}

/**
 * Normal WebView without request body interception.
 */
@Composable
private fun NormalWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebResourceRequest?) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            saveWebRequest(request)
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            takeScreenshot(view, url)
                            captureAndSaveContent(view, url)
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        loadWithOverviewMode = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                    }
                    setWebContentsDebuggingEnabled(true)
                    webView
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (portalUrl != null) {
                    view.loadUrl(portalUrl)
                }
            }
        )
    }
}

/**
 * Custom WebView with request body interception.
 */
@Composable
private fun CustomWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    updateShowedHint: (Boolean) -> Unit,
    showedHint: Boolean,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        WebViewWithCustomClient(
            portalUrl = portalUrl,
            webView = webView,
            saveWebRequest = saveWebRequest,
            modifier = Modifier.fillMaxSize(),
            captureAndSaveContent = captureAndSaveContent,
            takeScreenshot = takeScreenshot
        )

        HintInfoBox(
            context = LocalContext.current,
            modifier = Modifier.align(Alignment.Center),
            showedHint = showedHint,
            updateShowedHint = updateShowedHint
        )
    }
}

/**
 * Hint info box for user guidance.
 */
@Composable
private fun HintInfoBox(
    context: Context,
    modifier: Modifier,
    updateShowedHint: (Boolean) -> Unit = {},
    showedHint: Boolean
) {
    var showInfoBox1 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AlertDialogState.getNeverSeeAgainState(context, "info_box_1")
            .collect { neverSeeAgain ->
                showInfoBox1 = !neverSeeAgain
            }
    }

    if (showInfoBox1 && !showedHint) {
        NeverSeeAgainAlertDialog(
            title = stringResource(R.string.hint),
            message = stringResource(R.string.login_to_captive_then_click_end_analysis),
            preferenceKey = "info_box_1",
            onDismiss = {
                showInfoBox1 = false
                updateShowedHint(true)
            },
            modifier = modifier
        )
    }
}

/**
 * WebView with custom client for request interception.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewWithCustomClient(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : RequestInspectorWebViewClient(webView) {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            takeScreenshot(view, url)
                            captureAndSaveContent(view, url)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            webViewRequest: WebViewRequest
                        ): WebResourceResponse? {
                            Log.i(
                                "RequestInspectorWebView",
                                "Sending request from WebView: $webViewRequest"
                            )
                            saveWebRequest(webViewRequest)
                            return null
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        loadWithOverviewMode = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                    }
                    setWebContentsDebuggingEnabled(true)
                    webView
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (portalUrl != null) {
                    view.loadUrl(portalUrl)
                }
            }
        )
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
        val captureState = MainViewModel.CaptureState.RUNNING
        val statusMessage = "Capturing to file..."
        val targetPcapName = "captive_portal_capture.pcap"

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
            captureActions = captureActions,
            onOpenFile = {},
            copiedPcapFileUri = null,
            selectedTabIndex = 0,
            isPacketCaptureEnabled = true,
            onNavigateToManualConnect = {},
            getCaptivePortalAddress = {},
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            updateSelectedTabIndex = {},
            onNavigateToSetupPCAPDroid = {},
            updateIsPacketCaptureEnabled = {},
            analysisStatus = AnalysisStatus.NotCompleted,
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
        val captureState = MainViewModel.CaptureState.STOPPED
        val statusMessage = "Capture stopped"
        val targetPcapName = null

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
            captureActions = captureActions,
            onOpenFile = {},
            copiedPcapFileUri = null,
            selectedTabIndex = 0,
            isPacketCaptureEnabled = true,
            onNavigateToManualConnect = {},
            getCaptivePortalAddress = {},
            onStartCapture = {},
            onStopCapture = {},
            onStatusCheck = {},
            updateSelectedTabIndex = {},
            onNavigateToSetupPCAPDroid = {},
            updateIsPacketCaptureEnabled = {},
            analysisStatus = AnalysisStatus.NotCompleted,
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
private fun PreferenceSetupContentPreview() {
    AppTheme {
        PreferenceSetupContent(
            captureState = MainViewModel.CaptureState.IDLE,
            onStartCapture = {},
            getCaptivePortalAddress = {},
            onNavigateToSetupPCAPDroid = {},
            updateIsPacketCaptureEnabled = {},
        )
    }
}