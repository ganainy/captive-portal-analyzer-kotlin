package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.captive_portal_analyzer_kotlin.components.MenuItem
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.convertBSSIDToFileName
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnalysisScreen(
    offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    navigateToReport: () -> Unit,
    navigateBack: () -> Unit,
    navigateToAbout: () -> Unit,
    offlineWebpageContentRepository: OfflineWebpageContentRepository
) {
    val viewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
            offlineWebpageContentRepository = offlineWebpageContentRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val portalUrl by viewModel.portalUrl.collectAsState()
    val shouldShowNormalWebView by viewModel.shouldShowNormalWebView.collectAsState()

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
                                navigateToReport()
                        }
                    ),
                    MenuItem(
                        iconPath = R.drawable.reload,
                        itemName = stringResource(id = R.string.reload),
                        onClick = {
                            viewModel.showNormalWebView(true)
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
                if (shouldShowNormalWebView){
                    NormalWebView(
                        portalUrl = portalUrl,
                        webView = webView,
                        saveWebRequest = { request -> coroutineScope.launch {viewModel.saveWebResourceRequest(request) }},
                        bssid = viewModel.getCurrentNetworkUniqueIdentifier(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        captureAndSaveContent = { webView,content -> coroutineScope.launch {viewModel.saveWebpageContent(webView,content)} }
                    )

                }else{
                    CustomWebView(
                        portalUrl = portalUrl,
                        webView = webView,
                        saveWebRequest = { request -> coroutineScope.launch {viewModel.saveWebViewRequest(request) }},
                        bssid = viewModel.getCurrentNetworkUniqueIdentifier(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        captureAndSaveContent = { webView,content -> coroutineScope.launch {viewModel.saveWebpageContent(webView,content)} }
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

/*WebView that intercepts request body using JS injection*/
@Composable
private fun NormalWebView(
    portalUrl: String,
    webView: WebView,
    bssid: String,
    saveWebRequest: (WebResourceRequest?) -> Unit,
    captureAndSaveContent: (WebView,String) -> Unit,
    modifier: Modifier
) {

    Box(modifier = modifier) {
        AndroidView(
            factory = { webView.apply {
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
                        takeScreenshot(view,url,bssid) // Capture screenshot when a new page finishes loading
                        captureAndSaveContent(view,url) //capture and save webpage content (HTML + JS)
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
            }}
            ,
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.loadUrl(portalUrl)
            }
        )

    }
}


/*WebView that intercepts request body using JS injection*/
@Composable
private fun CustomWebView(
    portalUrl: String,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView,String) -> Unit,
    bssid: String,
    modifier: Modifier
) {

    Box(
        modifier = modifier
    ) {

            WebViewWithCustomClient(
                portalUrl = portalUrl,
                webView = webView,
                saveWebRequest = saveWebRequest,
                bssid = bssid,
                modifier = Modifier.fillMaxSize(),
                captureAndSaveContent = captureAndSaveContent
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
    captureAndSaveContent: (WebView,String) -> Unit,
    bssid: String,
    modifier: Modifier
) {

    Box(modifier = modifier) {
        AndroidView(
            factory = { webView.apply {
                webViewClient = object : RequestInspectorWebViewClient(webView) {

                                        // Called when a page finishes loading
                                        override fun onPageFinished(view: WebView, url: String) {
                                            super.onPageFinished(view, url)
                                            takeScreenshot(view,url,bssid) // Capture screenshot when a new page finishes loading
                                            captureAndSaveContent(view,url) //capture and save webpage content (HTML + JS)
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            webViewRequest: WebViewRequest
                                        ): WebResourceResponse? {
                                            Log.i("RequestInspectorWebView", "Sending request from WebView: $webViewRequest")
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
            }}
            ,
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                    view.loadUrl(portalUrl)
            }
        )

    }
}

private fun takeScreenshot(webView: WebView, url: String,bssid: String) {
    val formatted_bssid= convertBSSIDToFileName(bssid)
    val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // For Android 8.0 (API 26) or later
        webView.capturePicture()?.let { picture ->
            // Check for valid dimensions before creating the bitmap
            if (picture.width > 0 && picture.height > 0) {
                Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
                    .apply { picture.draw(Canvas(this)) }
            } else {
                null
            }
        }
    } else {
        // Fallback for older versions
        webView.isDrawingCacheEnabled = true
        val drawingCache = webView.drawingCache
        if (drawingCache != null && drawingCache.width > 0 && drawingCache.height > 0) {
            Bitmap.createBitmap(drawingCache)
        } else {
            null
        }
    }

    bitmap?.let {
        // Define the path where the image will be saved
        val directory = File(webView.context.getExternalFilesDir(null), "$formatted_bssid/screenshots")
        if (!directory.exists()) {
            directory.mkdirs() // Create the directory if it doesn't exist
        }

        // Define the file name for the image
        val fileName = "${System.currentTimeMillis()}.png"
        val file = File(directory, fileName)

        try {
            // Save the bitmap to the file
            FileOutputStream(file).use { out ->
                it.compress(Bitmap.CompressFormat.PNG, 100, out) // Compress the bitmap as PNG
                Log.i("WebViewScreenshot", "Screenshot saved to: ${file.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e("WebViewScreenshot", "Error saving screenshot: ${e.message}")
        }
    } ?: Log.e("WebViewScreenshot", "Invalid screenshot bitmap dimensions")
}
