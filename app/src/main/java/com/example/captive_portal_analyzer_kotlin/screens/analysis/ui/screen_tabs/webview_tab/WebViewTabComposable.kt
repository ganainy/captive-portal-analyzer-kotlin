package com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.screen_tabs.webview_tab

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.BuildConfig
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.PcapDroidPacketCaptureStatus
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.CustomChip
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.LongPressHintPopup
import com.example.captive_portal_analyzer_kotlin.components.MockWebView
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiData
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.analysis.WebViewType
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisCallbacks
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisError
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.PreferenceSetupContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.TestingWebView
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewContentConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun WebViewTabComposable(
    uiState: AnalysisUiState,
    uiData: AnalysisUiData,
    captureState: MainViewModel.CaptureState,
    statusMessage: String,
    targetPcapName: String?,
    analysisCallbacks: AnalysisCallbacks,
    webViewActions: WebViewActions,
    updateSelectedTabIndex: (Int) -> Unit,
    getCaptivePortalAddress: () -> Unit,
    onNavigateToManualConnect: () -> Unit,
    onStartCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    onNavigateToSetupPCAPDroid: () -> Unit,
    updatePcapDroidPacketCaptureStatus: (PcapDroidPacketCaptureStatus) -> Unit,
    isPCAPDroidInstalled: () -> Boolean
) {

    var effectiveUiState = uiState
    //for testing only, force captive portal detected state
    if (BuildConfig.DEBUG_SET_ANALYSIS_STATE_AS_CAPTIVE_PORTAL_DETECTED) {
        effectiveUiState = AnalysisUiState.CaptiveUrlDetected
    }


    Box(modifier = Modifier.fillMaxSize()) {
        when (effectiveUiState) {
            is AnalysisUiState.Loading -> LoadingIndicator(
                message = stringResource(effectiveUiState.messageStringResource),
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
                setSelectTabIndex = updateSelectedTabIndex

            )

            is AnalysisUiState.Error -> AnalysisError(
                contentPadding = PaddingValues(0.dp),
                uiState = effectiveUiState,
                onRetry = getCaptivePortalAddress,
                analysisCallbacks = analysisCallbacks,
                onNavigateToManualConnect = onNavigateToManualConnect
            )

            AnalysisUiState.AnalysisCompleteNavigateToNextScreen -> {
                //do nothing because we will handle navigation on analysis complete in the AnalysisScreen.kt
            }

            AnalysisUiState.PreferenceSetup -> PreferenceSetupContent(
                captureState = captureState,
                onStartCapture = onStartCapture,
                onStatusCheck = onStatusCheck,
                getCaptivePortalAddress = getCaptivePortalAddress,
                onNavigateToSetupPCAPDroid = onNavigateToSetupPCAPDroid,
                updatePcapDroidPacketCaptureStatus = { pcapDroidPacketCaptureStatus ->
                    updatePcapDroidPacketCaptureStatus(pcapDroidPacketCaptureStatus)
                },
                isPCAPDroidInstalled = isPCAPDroidInstalled
            )
        }
    }
}

@Composable
internal fun CaptivePortalWebsiteContent(
    config: WebViewContentConfig,
    analysisCallbacks: AnalysisCallbacks,
    webViewActions: WebViewActions,
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
    setSelectTabIndex: (Int) -> Unit
) {
    val effectiveWebViewType =
        if (BuildConfig.DEBUG_USE_TESTING_WEBVIEW) WebViewType.TestingWebView else config.webViewType

    Column(modifier = Modifier.fillMaxSize()) {


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

                Box() {
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

                    // --- Added CustomChip ---
                    val webViewLabel = when (effectiveWebViewType) {
                        WebViewType.NormalWebView -> "Basic WebView (Backup)"
                        WebViewType.CustomWebView -> "Custom WebView (Recommended)"
                        WebViewType.TestingWebView -> "Testing WebView "
                    }
                    CustomChip(
                        label = webViewLabel,
                        onClick = {
                        },
                        isSelected = true,
                        modifier = Modifier
                            .align(Alignment.TopEnd) // Position the chip at the top-right
                            .padding(top = 8.dp, end = 8.dp) // Add padding for spacing from edges
                    )
                }

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            LongPressHintPopup(
                popupHint = { Text(stringResource(R.string.the_app_has_two_types_of_webview_supported_if_the_custom_webview_recommended_doesnt_load_the_captive_website_properly_use_it_to_switch_to_the_backup_webview)) },
                anchor = {
                    GhostButton(
                        modifier = Modifier.weight(1f),
                        onClick = { webViewActions.switchWebViewType(analysisCallbacks.showToast) },
                        buttonText = stringResource(R.string.switch_browser_type)
                    )
                }
            )

            RoundCornerButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    setSelectTabIndex(1)  // Switch to Packet Capture tab
                },
                buttonText = stringResource(R.string.continuee),
                trailingIcon = painterResource(id = R.drawable.arrow_forward_ios_24px)
            )
        }
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
