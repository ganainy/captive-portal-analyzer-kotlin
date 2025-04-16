package com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.screen_tabs.webview_tab

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.captive_portal_analyzer_kotlin.components.MockWebView
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisInternetStatus
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiData
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.analysis.WebViewType
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisCallbacks
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisError
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.PreferenceSetupContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.TestingWebView
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewContentConfig
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
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
    var effectiveUiData = uiData
    //for testing only, force captive portal detected state, set captive portal url to google.com
    if (BuildConfig.DEBUG_SET_ANALYSIS_STATE_AS_CAPTIVE_PORTAL_DETECTED) {
        effectiveUiState = AnalysisUiState.CaptiveUrlDetected
        effectiveUiData = uiData.copy(portalUrl = "https://www.google.com")
    }


    Box(modifier = Modifier.fillMaxSize()) {
        when (effectiveUiState) {
            is AnalysisUiState.Loading -> LoadingIndicator(
                message = stringResource(effectiveUiState.messageStringResource),
                modifier = Modifier
            )

            is AnalysisUiState.CaptiveUrlDetected -> CaptivePortalWebsiteContent(
                config = WebViewContentConfig(
                    webViewType = effectiveUiData.webViewType,
                    portalUrl = effectiveUiData.portalUrl,
                    showedHint = effectiveUiData.showedHint,
                    contentPadding = PaddingValues(0.dp), // Padding applied inside now
                    captureState = captureState,
                    statusMessage = statusMessage,
                    targetPcapName = targetPcapName
                ),
                analysisCallbacks = analysisCallbacks,
                webViewActions = webViewActions,
                setSelectTabIndex = updateSelectedTabIndex

            )

            is AnalysisUiState.Error -> AnalysisError(
                uiState = effectiveUiState,
                onRetry = getCaptivePortalAddress,
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
    // Remember the WebView instance
    val webView = remember {
        // Avoid creating WebView in preview
        if (context is androidx.activity.ComponentActivity) {
            WebView(context).apply {
                // Apply common settings once here if desired
                settings.apply {
                    @SuppressLint("SetJavaScriptEnabled") // Keep if needed
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true // Often useful with loadWithOverviewMode
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                setWebContentsDebuggingEnabled(true) // Enable debugging
            }
        } else {
            // Return a dummy or null for preview environments if strict checks are needed
            null // Or handle appropriately for previews
        }
    }

    // Use DisposableEffect for cleanup
    DisposableEffect(webView) {
        onDispose {
            webView?.destroy()
        }
    }

    // Pass the remembered webView instance
    if (webView != null) {
        WebViewInteractionContent(
            config = config,
            webView = webView,
            coroutineScope = coroutineScope,
            analysisCallbacks = analysisCallbacks,
            webViewActions = webViewActions,
            setSelectTabIndex = setSelectTabIndex
        )
    } else if (LocalInspectionMode.current) {
        // Show placeholder in Preview if webview couldn't be created
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("WebView Preview Placeholder")
        }
    }
    // Handle cases where webview is null in a real environment if necessary
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
    val isPreview = LocalInspectionMode.current // Check if in preview mode

    // State to hold the current URL displayed in the address bar
    var currentUrl by remember { mutableStateOf(config.portalUrl ?: "about:blank") }

    // Callback to update the URL state when the page finishes loading
    val onUrlChanged: (String) -> Unit = { newUrl ->
        currentUrl = newUrl
    }

    Column(modifier = Modifier.fillMaxSize().padding(config.contentPadding)) { // Apply padding here

        // --- Address Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp) // Padding for the address bar
                .clip(RoundedCornerShape(8.dp)) // Slightly rounded corners for the bar
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .height(48.dp) // Fixed height for the address bar
                .padding(horizontal = 4.dp), // Inner padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- WebView Type Chip ---
            val webViewLabel = when (effectiveWebViewType) {
                WebViewType.NormalWebView -> "Basic" // Shorter labels
                WebViewType.CustomWebView -> "Custom"
                WebViewType.TestingWebView -> "Testing"
            }
            CustomChip(
                label = webViewLabel,
                onClick = { /* Chip click action if needed */ },
                isSelected = true, // Assuming it visually represents the current type
                modifier = Modifier.padding(end = 4.dp) // Space between chip and URL
            )

            // --- URL Display ---
            Text(
                text = currentUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f) // Takes available space
                    .padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // --- Refresh Icon Button ---
            IconButton(
                onClick = {
                    // Only reload if not in preview and webview exists
                    if (!isPreview) {
                        webView.reload()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh_webview),
                    tint = MaterialTheme.colorScheme.primary // Or onSurfaceVariant
                )
            }
        } // End of Address Bar Row

        Spacer(modifier = Modifier.height(8.dp)) // Space between address bar and webview

        // --- WebView Container ---
        Box( // This Box wraps ONLY the WebView
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(1.dp)
        ) {
            // --- WebView Content ---
            if (isPreview) {
                MockWebView()
            } else {
                when (effectiveWebViewType) {
                    WebViewType.NormalWebView -> NormalWebView(
                        portalUrl = config.portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                webViewActions.saveWebResourceRequest(request)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        captureAndSaveContent = { wv, content ->
                            coroutineScope.launch {
                                webViewActions.saveWebpageContent(wv, content, analysisCallbacks.showToast)
                            }
                        },
                        takeScreenshot = webViewActions::takeScreenshot,
                        onUrlChanged = onUrlChanged
                    )

                    WebViewType.CustomWebView -> CustomWebView(
                        portalUrl = config.portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                webViewActions.saveWebViewRequest(request)
                            }
                        },
                        takeScreenshot = webViewActions::takeScreenshot,
                        modifier = Modifier.fillMaxSize(),
                        captureAndSaveContent = { wv, content ->
                            coroutineScope.launch {
                                webViewActions.saveWebpageContent(wv, content, analysisCallbacks.showToast)
                            }
                        },
                        showedHint = config.showedHint,
                        updateShowedHint = webViewActions::updateShowedHint,
                        onUrlChanged = onUrlChanged
                    )

                    WebViewType.TestingWebView -> TestingWebView()
                }
            }
            // Removed the old Row with Chip and Refresh Icon from here
        } // End of WebView Box wrapper

        // --- Bottom Buttons ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GhostButton(
                modifier = Modifier.weight(1f),
                onClick = { webViewActions.switchWebViewType(analysisCallbacks.showToast) },
                buttonText = stringResource(R.string.switch_browser_type)
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
    webView: WebView, // Receive instance
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    updateShowedHint: (Boolean) -> Unit,
    showedHint: Boolean,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        WebViewWithCustomClient(
            portalUrl = portalUrl,
            webView = webView,
            saveWebRequest = saveWebRequest,
            modifier = Modifier.fillMaxSize(),
            captureAndSaveContent = captureAndSaveContent,
            takeScreenshot = takeScreenshot,
            onUrlChanged = onUrlChanged
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
    onUrlChanged: (String) -> Unit,
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
                            onUrlChanged(url) // Update the URL state
                            takeScreenshot(view, url)
                            captureAndSaveContent(view, url)
                        }
                    }
                }
                webView
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val currentLoadedUrl = view.url
                // Load URL only if it's different from the currently loaded one or if it's the initial load
                if (portalUrl != null && portalUrl != currentLoadedUrl) {
                    // Avoid reloading unnecessarily if the update block recomposes but URL is same
                    if (view.originalUrl != portalUrl) { // Check original URL to prevent loop on redirects sometimes
                        view.loadUrl(portalUrl)
                    }
                } else if (portalUrl == null && currentLoadedUrl != null && currentLoadedUrl != "about:blank") {
                    // Optional: Load about:blank if portalUrl becomes null and something is loaded
                    view.loadUrl("about:blank")
                }
                // Reload is handled by the refresh button calling webView.reload() externally
            }
        )
    }
}


/**
 * WebView with custom client for request interception.
 */
@SuppressLint("SetJavaScriptEnabled") // Keep if needed
@Composable
private fun WebViewWithCustomClient(
    portalUrl: String?,
    webView: WebView, // Receive instance
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : RequestInspectorWebViewClient(webView) {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            onUrlChanged(url) // Update the URL state
                            takeScreenshot(view, url)
                            captureAndSaveContent(view, url)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            webViewRequest: WebViewRequest
                        ): WebResourceResponse? {
                            Log.i(
                                "RequestInspectorWebView",
                                "Intercepting request: ${webViewRequest.url}"
                            )
                            saveWebRequest(webViewRequest)
                            return null // Let WebView handle the request normally
                        }
                    }
                }
                webView
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val currentLoadedUrl = view.url
                // Load URL only if it's different from the currently loaded one or if it's the initial load
                if (portalUrl != null && portalUrl != currentLoadedUrl) {
                    // Avoid reloading unnecessarily if the update block recomposes but URL is same
                    if (view.originalUrl != portalUrl) { // Check original URL to prevent loop on redirects sometimes
                        view.loadUrl(portalUrl)
                    }
                } else if (portalUrl == null && currentLoadedUrl != null && currentLoadedUrl != "about:blank") {
                    // Optional: Load about:blank if portalUrl becomes null and something is loaded
                    view.loadUrl("about:blank")
                }
                // Reload is handled by the refresh button calling webView.reload() externally
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


