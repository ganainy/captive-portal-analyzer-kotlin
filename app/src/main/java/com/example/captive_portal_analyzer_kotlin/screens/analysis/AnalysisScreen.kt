package com.example.captive_portal_analyzer_kotlin.screens.analysis

import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator


import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.HintText
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.launch

@Composable
fun AnalysisScreen(
    repository :  NetworkSessionRepository,
    navigateToSessionList: () -> Unit,
    navigateToManualConnect: () -> Unit,
    sessionManager: NetworkSessionManager,
    sharedViewModel: SharedViewModel,
) {
    val analysisViewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            sessionManager = sessionManager,
            showToast = sharedViewModel::showToast
        )
    )

    val uiState by analysisViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val portalUrl by analysisViewModel.portalUrl.collectAsState()
    val webViewType by analysisViewModel.webViewType.collectAsState()
    val showedHint by analysisViewModel.showedHint.collectAsState()

    val showToast = { message: String, style: ToastStyle ->
        sharedViewModel.showToast(
            message = message, style = style,
        )
    }
    analysisViewModel.getCaptivePortalAddress(showToast)

    val webView = remember {
        WebView(context)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    Scaffold(

    ) { contentPadding ->


        when (uiState) {

            is AnalysisUiState.Loading -> {
                LoadingIndicator(
                    message = stringResource((uiState as AnalysisUiState.Loading).messageStringResource),
                    modifier = Modifier.padding(contentPadding)
                )
            }

            is AnalysisUiState.CaptiveUrlDetected -> {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // WebView container with weight to take all available space
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(1.dp)
                    ) {

                        when (webViewType) {
                            WebViewType.NormalWebView -> {

                                NormalWebView(
                                    portalUrl = portalUrl,
                                    webView = webView,
                                    saveWebRequest = { request ->
                                        coroutineScope.launch {
                                            analysisViewModel.saveWebResourceRequest(request)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding),
                                    captureAndSaveContent = { webView, content ->
                                        coroutineScope.launch {
                                            analysisViewModel.saveWebpageContent(
                                                webView,
                                                content,
                                                showToast
                                            )
                                        }
                                    },
                                    takeScreenshot = analysisViewModel::takeScreenshot
                                )


                            }

                            WebViewType.CustomWebView -> {

                                CustomWebView(
                                    portalUrl = portalUrl,
                                    webView = webView,
                                    saveWebRequest = { request ->
                                        coroutineScope.launch {
                                            analysisViewModel.saveWebViewRequest(request)
                                        }
                                    },
                                    takeScreenshot = analysisViewModel::takeScreenshot,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding),
                                    captureAndSaveContent = { webView, content ->
                                        coroutineScope.launch {
                                            analysisViewModel.saveWebpageContent(
                                                webView,
                                                content,
                                                showToast
                                            )
                                        }
                                    },
                                    showedHint = showedHint,
                                    updateShowedHint = analysisViewModel::updateShowedHint
                                )

                            }
                            }
                        }



                    // Other elements will wrap their content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
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
                                onClick = {
                                    analysisViewModel.stopAnalysis(onUncompletedAnalysis = {
                                        showUncompletedAnalysisDialog(
                                            context = context,
                                            hideDialog = sharedViewModel::hideDialog,
                                            showDialog = sharedViewModel::showDialog,
                                            navigateToSessionList = navigateToSessionList
                                        )
                                    })
                                },
                                buttonText = stringResource(id = R.string.end_analysis)
                            )

                            GhostButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                onClick = {
                                    analysisViewModel.switchWebViewType(
                                        showToast = showToast
                                    )
                                },
                                text = stringResource(id = R.string.switch_browser_type)
                            )
                        }
                    }
                }
            }

            is AnalysisUiState.Error -> {
                AnalysisError(
                    contentPadding,
                    uiState,
                    analysisViewModel::getCaptivePortalAddress,
                    showToast,
                    navigateToManualConnect
                )
            }

            AnalysisUiState.AnalysisComplete -> navigateToSessionList()
        }
    }
}



// Extract dialog configuration into a separate function
private fun showUncompletedAnalysisDialog(context: Context,hideDialog: () -> Unit,
                                          showDialog: (title: String, message: String, confirmText: String, dismissText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) -> Unit, navigateToSessionList: () -> Unit) {
    showDialog(
        context.getString(R.string.warning),
        context.getString(R.string.it_looks_like_you_still_have_no_full_internet_connection_please_complete_the_login_process_of_the_captive_portal_before_stopping_the_analysis),
        context.getString(R.string.stop_analysis_anyway),
         context.getString(R.string.dismiss),
         navigateToSessionList,
        hideDialog,
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(showBackground = true)
private fun AnalysisErrorPreview() {
    AppTheme {
        AnalysisError(
            contentPadding = PaddingValues(),
            uiState = AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl),
            getCaptivePortalAddress = {},
            showToast = { _, _ -> },
            navigateToManualConnect = {}
        )
    }
}

@Composable
private fun AnalysisError(
    contentPadding: PaddingValues,
    uiState: AnalysisUiState,
    getCaptivePortalAddress: (showToast: (String, ToastStyle) -> Unit) -> Unit,
    showToast: (String, ToastStyle) -> Unit,
    navigateToManualConnect: () -> Unit
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

                AnalysisUiState.ErrorType.Unknown -> {
                    text = stringResource(R.string.something_went_wrong)
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            HintText(
                hint = hint,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), // Optional padding for better UI spacing
                horizontalArrangement = Arrangement.SpaceBetween // Ensures buttons are spaced apart
            ) {
                RoundCornerButton(
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp), // Add spacing between buttons
                    onClick = {
                        navigateToManualConnect()
                    },
                    buttonText = stringResource(id = R.string.connect_to_another_network)
                )

                GhostButton(
                    modifier = Modifier
                        .weight(1f) // Distribute available space equally
                        .padding(end = 8.dp), // Add spacing between buttons
                    onClick = {
                        getCaptivePortalAddress(showToast)
                    },
                    text = stringResource(id = R.string.retry)
                )

            }

        }
    }
}

/*WebView that intercepts request body using JS injection*/
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

                        // Called when a page finishes loading
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            takeScreenshot(
                                view,
                                url,
                            ) // Capture screenshot when a new page finishes loading
                            captureAndSaveContent(
                                view,
                                url
                            ) //capture and save webpage content (HTML + JS)
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


/*WebView that intercepts request body using JS injection*/
@Composable
private fun CustomWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    updateShowedHint: (Boolean) -> Unit,
    modifier: Modifier,
    showedHint: Boolean
) {

    Box(
        modifier = modifier
    ) {

        WebViewWithCustomClient(
            portalUrl = portalUrl,
            webView = webView,
            saveWebRequest = saveWebRequest,
            modifier = Modifier.fillMaxSize(),
            captureAndSaveContent = captureAndSaveContent,
            takeScreenshot = takeScreenshot,
        )

        HintInfoBox(
            context = LocalContext.current,
            modifier = Modifier.align(Alignment.Center),
            showedHint = showedHint,
            updateShowedHint =updateShowedHint
        )
    }
}

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
            onDismiss = { showInfoBox1 = false
                updateShowedHint(true)
                        },
            modifier = modifier
        )
    }
}


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

                        // Called when a page finishes loading
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            takeScreenshot(
                                view,
                                url,
                            ) // Capture screenshot when a new page finishes loading
                            captureAndSaveContent(
                                view,
                                url
                            ) //capture and save webpage content (HTML + JS)
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

