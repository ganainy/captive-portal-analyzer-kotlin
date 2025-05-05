package com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.screen_tabs.webview_tab

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiData
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.analysis.WebViewType
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisCallbacks
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisError
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.PreferenceSetupContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.TestingWebView
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.WebViewContentConfig
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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


// --- Placeholder/Mock Classes (Replace with your actual implementations) ---

// Assuming these types/interfaces/data classes exist in your project
data class WebViewContentConfig(
    val portalUrl: String?,
    val contentPadding: PaddingValues = PaddingValues(0.dp),
    val showUrlBar: Boolean = true, // Add this config if you want to hide the bar
    val showedHint: Boolean = false
)

enum class WebViewType {
    NormalWebView, CustomWebView, TestingWebView
}

interface AnalysisCallbacks {
    fun showToast(message: String)
}

interface WebViewActions {
    fun saveWebResourceRequest(request: WebResourceRequest)
    fun saveWebViewRequest(request: WebResourceRequest) // Assuming this is different or specific to CustomWebView
    fun saveWebpageContent(webView: WebView, content: String?, showToast: (String) -> Unit)
    fun takeScreenshot(webView: WebView): Bitmap?
    fun switchWebViewType(showToast: (String) -> Unit)
    fun updateShowedHint(showed: Boolean)
}

// Mock BuildConfig for preview
object BuildConfig {
    const val DEBUG_USE_TESTING_WEBVIEW = false
}

// Mock Composables for preview and non-preview
@Composable fun CustomChip(label: String, onClick: () -> Unit, isSelected: Boolean, modifier: Modifier) {
    // Basic Chip representation
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick // Assuming onClick is handled by Surface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable fun MockWebView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.LightGray.copy(alpha = 0.3f))
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Mock WebView Content", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable fun NormalWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebResourceRequest) -> Unit,
    modifier: Modifier = Modifier,
    captureAndSaveContent: (WebView, String?) -> Unit,
    takeScreenshot: (WebView) -> Bitmap?,
    onUrlChanged: (String) -> Unit // Added URL change callback
) {
    // Your actual WebView implementation logic
    // This mock version doesn't actually load URLs or report changes
    MockWebView(modifier)
    // In a real implementation, your WebViewClient should call onUrlChanged
    // For example, in onPageFinished: onUrlChanged(webView.url ?: "about:blank")
}

@Composable fun CustomWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebResourceRequest) -> Unit,
    takeScreenshot: (WebView) -> Bitmap?,
    modifier: Modifier = Modifier,
    captureAndSaveContent: (WebView, String?) -> Unit,
    showedHint: Boolean,
    updateShowedHint: (Boolean) -> Unit,
    onUrlChanged: (String) -> Unit // Added URL change callback
) {
    // Your actual Custom WebView implementation logic
    MockWebView(modifier)
    // In a real implementation, your WebViewClient should call onUrlChanged
    // For example, in onPageFinished: onUrlChanged(webView.url ?: "about:blank")
}

@Composable fun TestingWebView(modifier: Modifier = Modifier) {
    // Your actual Testing WebView implementation logic
    MockWebView(modifier)
    // This mock version doesn't actually load URLs or report changes
}

@Composable fun GhostButton(onClick: () -> Unit, buttonText: String, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(buttonText)
    }
}

@Composable fun RoundCornerButton(onClick: () -> Unit, buttonText: String, modifier: Modifier = Modifier, trailingIcon: androidx.compose.ui.graphics.painter.Painter? = null) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp) // Example rounded shape
    ) {
        Text(buttonText)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(painter = trailingIcon, contentDescription = null)
        }
    }
}

// Mock R.string and R.drawable for preview
object R {
    object string {
        const val refresh_webview = "Refresh"
        const val switch_browser_type = "Switch Type"
        const val continuee = "Continue"
        const val back_webview = "Back" // Added for back button content description
        const val enter_url = "Enter URL" // Added for TextField placeholder/label
    }
    object drawable {
        val arrow_forward_ios_24px = 0 // Mock drawable ID
    }
}

// --- End Placeholder/Mock Classes ---


@OptIn(ExperimentalMaterial3Api::class) // Required for OutlinedTextFieldDefaults
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

    // State to hold the current URL displayed in the address bar and reflected in TextField
    var currentUrl by remember { mutableStateOf(config.portalUrl ?: "about:blank") }

    // State for the text field's content, which the user can edit
    var addressBarText by remember { mutableStateOf(currentUrl) }

    // Sync the address bar text with the actual loaded URL whenever it changes
    LaunchedEffect(currentUrl) {
        addressBarText = currentUrl
    }

    // Callback to update the URL state when the page finishes loading
    // This is crucial to sync the UI state with the WebView's actual URL
    val onUrlChanged: (String) -> Unit = { newUrl ->
        currentUrl = newUrl
        // addressBarText will be updated by the LaunchedEffect
    }

    // Get the keyboard controller to hide the keyboard on navigation
    val keyboardController = LocalSoftwareKeyboardController.current

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
            // --- Back Button ---
            IconButton(
                onClick = {
                    if (!isPreview) {
                        webView.goBack()
                    }
                },
                // Enable only if WebView can go back (and not in preview)
                // Note: webView.canGoBack() might not be immediately accurate
                // depending on WebView state lifecycle, but is the standard way to check.
                // For more robust control, you might need to update a state based on
                // WebViewClient's onPageStarted/Finished or onHistoryItemChanged.
                enabled = !isPreview && webView.canGoBack()
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_webview),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // --- WebView Type Chip ---
            val webViewLabel = when (effectiveWebViewType) {
                WebViewType.NormalWebView -> "Basic" // Shorter labels
                WebViewType.CustomWebView -> "Custom"
                WebViewType.TestingWebView -> "Testing"
            }
            CustomChip(
                label = webViewLabel,
                onClick = {
                    // Only allow switching if not in preview
                    if (!isPreview) {
                        webViewActions.switchWebViewType(analysisCallbacks.showToast)
                    }
                },
                isSelected = true, // Assuming it visually represents the current type
                modifier = Modifier.padding(end = 4.dp) // Space between chip and URL
            )

            // --- Modifiable URL Text Field ---
            OutlinedTextField(
                value = addressBarText,
                onValueChange = { addressBarText = it },
                modifier = Modifier
                    .weight(1f) // Takes available space
                    .padding(horizontal = 4.dp), // Inner padding around the text field itself
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        // Only load URL if not in preview
                        if (!isPreview) {
                            val url = addressBarText.trim()
                            if (url.isNotEmpty()) {
                                // Basic URL scheme handling
                                val validatedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                                    url
                                } else {
                                    "https://$url" // Default to https
                                }
                                webView.loadUrl(validatedUrl)
                                keyboardController?.hide() // Hide keyboard after navigating
                            }
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent, // Hide default border
                    unfocusedBorderColor = Color.Transparent, // Hide default border
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                // Optional: Add a placeholder
                // placeholder = { Text(stringResource(R.string.enter_url)) }
            )

            // --- Refresh Icon Button ---
            IconButton(
                onClick = {
                    // Only reload if not in preview
                    if (!isPreview) {
                        webView.reload()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh_webview),
                    tint = MaterialTheme.colorScheme.primary
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
                        onUrlChanged = onUrlChanged // Pass the callback
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
                        onUrlChanged = onUrlChanged // Pass the callback
                    )

                    WebViewType.TestingWebView -> TestingWebView() // No URL changes in testing mock?
                }
            }
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
                onClick = {
                    // Only allow switching if not in preview
                    if (!isPreview) {
                        webViewActions.switchWebViewType(analysisCallbacks.showToast)
                    }
                },
                buttonText = stringResource(R.string.switch_browser_type)
            )

            RoundCornerButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    // Only allow action if not in preview
                    if (!isPreview) {
                        setSelectTabIndex(1)  // Switch to Packet Capture tab
                    }
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

                        // Handle SSL errors - add this method
                        @SuppressLint("WebViewClientOnReceivedSslError")
                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                            // For captive portal analyzer, we want to proceed despite SSL errors
                            handler.proceed()
                            // Optionally log the error
                            Log.d("WebViewSSL", "Proceeding despite SSL error: ${error.primaryError}")
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


                        // Handle SSL errors
                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                            // For captive portal analyzer, we want to proceed despite SSL errors
                            handler.proceed()
                            // Optionally log the error
                            Log.d("WebViewSSL", "Proceeding despite SSL error: ${error.primaryError}")
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


