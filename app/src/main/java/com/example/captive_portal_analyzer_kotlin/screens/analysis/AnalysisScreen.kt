package com.example.captive_portal_analyzer_kotlin.screens.analysis

import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.MenuItem
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.OfflineScreenshotRepository
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.launch

@Composable
fun AnalysisScreen(
    offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    offlineWebpageContentRepository: OfflineWebpageContentRepository,
    screenshotRepository: OfflineScreenshotRepository,
    navigateToReport: () -> Unit,
    navigateBack: () -> Unit,
    navigateToAbout: () -> Unit,
    sessionManager: NetworkSessionManager,
    sharedViewModel: SharedViewModel,
) {
    val analysisViewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
            offlineWebpageContentRepository = offlineWebpageContentRepository,
            screenshotRepository = screenshotRepository,
            sessionManager = sessionManager
        )
    )

    val uiState by analysisViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val portalUrl by analysisViewModel.portalUrl.collectAsState()
    val shouldShowNormalWebView by analysisViewModel.shouldShowNormalWebView.collectAsState()

    val showToast= { message:String, style:ToastStyle ->
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
        topBar = {
            ToolbarWithMenu(
                title = stringResource(id = R.string.analysis_screen_title),
                menuItems = listOf(
                    MenuItem(
                        iconPath = R.drawable.stop,
                        itemName = stringResource(id = R.string.stop_analysis),
                        onClick = {
                            analysisViewModel.stopAnalysis(onUncompletedAnalysis= {
                                sharedViewModel.showDialog(
                                    title = context.getString(R.string.warning),
                                    message =context.getString(R.string.it_looks_like_you_still_have_no_full_internet_connection_please_complete_the_login_process_of_the_captive_portal_before_stopping_the_analysis),
                                    confirmText =  context.getString(R.string.stop_analysis_anyway),
                                    dismissText = context.getString(R.string.dismiss),
                                    onConfirm = navigateToReport,
                                    onDismiss = sharedViewModel::hideDialog
                                )
                            },)
                        }
                    ),

                    MenuItem(
                        iconPath = R.drawable.web,
                        itemName = if (shouldShowNormalWebView) {
                            stringResource(id = R.string.return_original_detection_method)
                        } else {
                            stringResource(id = R.string.change_detection_method)
                        },
                        onClick = {
                            analysisViewModel.showNormalWebView(true,showToast)
                        }
                    ),
                    MenuItem(
                        iconPath = R.drawable.about,
                        itemName = stringResource(id = R.string.about),
                        onClick = {
                            navigateToAbout()
                        }
                    ),

                    )
            )
        }
    ) { contentPadding ->

        when (uiState) {

            is AnalysisUiState.Loading -> {
                CustomProgressIndicator(
                    message = stringResource((uiState as AnalysisUiState.Loading).messageStringResource),
                    modifier = Modifier.padding(contentPadding)
                )
            }

            is AnalysisUiState.CaptiveUrlDetected -> {
                if (shouldShowNormalWebView) {
                    NormalWebView(
                        portalUrl = portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                analysisViewModel.saveWebResourceRequest(
                                    request
                                )
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

                } else {
                    CustomWebView(
                        portalUrl = portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                analysisViewModel.saveWebViewRequest(
                                    request
                                )
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
                        }
                    )
                }

            }

            is AnalysisUiState.Error -> {
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
                        Text(
                            text = stringResource((uiState as AnalysisUiState.Error).messageStringResource),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Button(
                            onClick = {
                                analysisViewModel.getCaptivePortalAddress(showToast)
                            }
                        ) {
                            Text(stringResource(id = R.string.retry_captive_detection))
                        }
                    }
                }
            }

            AnalysisUiState.AnalysisComplete -> navigateToReport()
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
    modifier: Modifier
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
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun HintInfoBox(context: Context, modifier: Modifier) {
    var showInfoBox1 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AlertDialogState.getNeverSeeAgainState(context, "info_box_1")
            .collect { neverSeeAgain ->
                showInfoBox1 = !neverSeeAgain
            }
    }

    if (showInfoBox1) {
        NeverSeeAgainAlertDialog(
            title = stringResource(R.string.hint),
            message = stringResource(R.string.login_to_captive_then_click_end_analysis),
            preferenceKey = "info_box_1",
            onDismiss = { showInfoBox1 = false },
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

