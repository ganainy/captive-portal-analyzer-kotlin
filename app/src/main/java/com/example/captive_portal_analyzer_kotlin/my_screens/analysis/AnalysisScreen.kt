package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.CustomSnackBar
import com.example.captive_portal_analyzer_kotlin.components.InfoBox
import com.example.captive_portal_analyzer_kotlin.components.InfoBoxState
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnalysisScreen(
    dataRepository: IDataRepository,
    navigateToReport: () -> Unit,
    navigateBack: () -> Unit
) {
    val viewModel: AnalysisViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val portalUrl = viewModel.portalUrl.value // Observe the URL from ViewModel
    val isWebViewLoading = viewModel.isWebViewLoading.value // Observe the URL from ViewModel
    var webView by remember { mutableStateOf(WebView(context)) }


    Scaffold(
        topBar = {
            ToolbarWithMenu(
                openMenu = {
                    scope.launch {
                        drawerState.open() // Open the drawer when the menu icon is clicked
                    }
                },
                title = stringResource(id = R.string.analysis_screen_title)
            )
        }
    ) { contentPadding ->
        // Handle different UI states
        when (uiState) {
            is AnalysisUiState.Loading -> {
                // Show loading indicator
                CustomProgressIndicator(message = stringResource((uiState as AnalysisUiState.Loading).messageStringResource),)
            }
            is AnalysisUiState.CaptiveUrlDetected -> {
                //  captive URL detected
                MainContent(contentPadding, isWebViewLoading, webView, portalUrl, viewModel)
            }
            is AnalysisUiState.Error -> {
                // Show error message
                CustomSnackBar(
                    message = stringResource((uiState as AnalysisUiState.Error).messageStringResource),
                    onDismiss = navigateBack
                )
            }
        }
    }


}


/**
 * Shows a hint InfoBox at the center of the screen.

 * @param context the current context
 * @param modifier the modifier to apply to the InfoBox
 */
@Composable
private fun HintInfoBox(context: Context, modifier: Modifier) {
    var showInfoBox1 by remember { mutableStateOf(false) }

    // Check the value of "info_box_1" from DataStore before showing the InfoBox
    LaunchedEffect(Unit) {
        InfoBoxState.getNeverSeeAgainState(context, "info_box_1")
            .collect { neverSeeAgain ->
                showInfoBox1 = !neverSeeAgain // Show only if not marked as "never see again"
            }
    }

    if (showInfoBox1) {
        InfoBox(
            title = stringResource(R.string.hint),
            message = stringResource(R.string.login_to_captive_then_click_end_analysis),
            preferenceKey = "info_box_1",
            onDismiss = { showInfoBox1 = false },modifier = modifier
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainContent(
    contentPadding: PaddingValues,
    isWebViewLoading: Boolean,
    webView: WebView,
    portalUrl: String,
    viewModel: AnalysisViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {


        WebViewWithCustomClient(
            webView = webView,
            modifier = Modifier.fillMaxSize(),
            portalUrl,
            updateLoadingState = viewModel::setWebviewLoadingStatus
        )

        HintInfoBox(LocalContext.current, modifier = Modifier.align(Alignment.Center))

        // Button at the bottom center
        Button(
            onClick = {
                // TODO: Handle the button click
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(text = stringResource(R.string.end_analysis))
        }
    }
}

/*@Composable
private fun NormalWebView(
    webView: WebView,
    portalUrl: String="https://captive.ganainy.online",
) {

        AndroidView(factory = {
            val customWebViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Handle all URLs within the WebView
                    view.loadUrl(request.url.toString())
                    return true
                }
            }
            webView.webViewClient = customWebViewClient
            webView.loadUrl(portalUrl)
            webView
        }, modifier = Modifier.fillMaxSize())
    }*/


@Composable
fun WebViewWithCustomClient(webView: WebView,modifier: Modifier,portalUrl:String,updateLoadingState: (Boolean) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { context ->

            webView.apply {
                webViewClient =  object : RequestInspectorWebViewClient(webView) {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        webViewRequest: WebViewRequest
                    ): WebResourceResponse? {
                        // ("handle request manually based on data from webViewRequest and return custom response")
                        return super.shouldInterceptRequest(view, webViewRequest)
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        updateLoadingState(true)
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        updateLoadingState(false)
                        super.onPageFinished(view, url)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        // Load all links in the WebView
                        view.loadUrl(request.url.toString())
                        return true // Indicates WebView will handle the URL
                    }

                    @Suppress("deprecation") // For older Android versions
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        view.loadUrl(url)
                        return true // Indicates WebView will handle the URL
                    }
                }
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true;
                settings.useWideViewPort = true
                settings.builtInZoomControls = true;
                settings.domStorageEnabled = true
                setWebContentsDebuggingEnabled(true)
            }
        },
        update = { webView ->
            /* load a URL */
            webView.loadUrl(portalUrl)

        }
    )
}



