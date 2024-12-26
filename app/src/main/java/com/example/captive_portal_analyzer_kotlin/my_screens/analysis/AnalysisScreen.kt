package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.URLUtil
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.InfoBox
import com.example.captive_portal_analyzer_kotlin.components.InfoBoxState
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import com.example.captive_portal_analyzer_kotlin.room.CustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnalysisScreen(
    offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    navigateToReport: () -> Unit,
    navigateBack: () -> Unit
) {
    val viewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val portalUrl by viewModel.portalUrl.collectAsState()

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
                openMenu = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                title = stringResource(id = R.string.analysis_screen_title)
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
                MainContent(
                    portalUrl = portalUrl,
                    webView = webView,
                    saveWebRequest = { request -> coroutineScope.launch {viewModel.saveRequest(request) }},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
            is AnalysisUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource((uiState as AnalysisUiState.Error).messageStringResource),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Red
                    )
                }
            }
        }
    }
}


@Composable
private fun MainContent(
    portalUrl: String,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
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
        InfoBoxState.getNeverSeeAgainState(context, "info_box_1")
            .collect { neverSeeAgain ->
                showInfoBox1 = !neverSeeAgain
            }
    }

    if (showInfoBox1) {
        InfoBox(
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
    portalUrl: String,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    modifier: Modifier
) {

    Box(modifier = modifier) {
        AndroidView(
            factory = { webView.apply {
                webViewClient = object : RequestInspectorWebViewClient(webView) {

                    override fun shouldInterceptRequest(
                        view: WebView,
                        webViewRequest: WebViewRequest
                    ): WebResourceResponse? {
                        Log.i("RequestInspectorWebView", "Sending request from WebView: $webViewRequest")
                        saveWebRequest(webViewRequest)
                        return null
                    }



                    /*             override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        view.loadUrl(request.url.toString())
                        return true
                    }*/
                }

                settings.apply {
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                setWebContentsDebuggingEnabled(true)
                webView
            }},
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (URLUtil.isValidUrl(portalUrl)) {
                    view.loadUrl(portalUrl)
                }
            }
        )

    }
}