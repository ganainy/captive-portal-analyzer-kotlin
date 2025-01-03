package com.example.captive_portal_analyzer_kotlin.screens.analysis

import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.utils.LocalOrRemoteCaptiveChecker
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import detectCaptivePortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


sealed class AnalysisUiState {
    data class Loading(val messageStringResource: Int) : AnalysisUiState()
    object CaptiveUrlDetected : AnalysisUiState()
    object AnalysisComplete : AnalysisUiState()
    enum class ErrorType {
        CannotDetectCaptiveUrl,
        Unknown,
    }

    data class Error(val type: ErrorType) : AnalysisUiState()
}

enum class WebViewType {
    NormalWebView,
    CustomWebView
}

class AnalysisViewModel(
    application: Application,
    private val sessionManager: NetworkSessionManager,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState =
        MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _portalUrl = MutableStateFlow<String?>(null)
    val portalUrl: StateFlow<String?> get() = _portalUrl.asStateFlow()

    private val _webViewType = MutableStateFlow<WebViewType>(WebViewType.CustomWebView)
    val webViewType = _webViewType.asStateFlow()

    // Flag to track if the hint has been shown
    private val _showedHint = MutableStateFlow<Boolean>(false)
    val showedHint = _showedHint.asStateFlow()

    init {


        /* //testing
          _uiState.value = AnalysisUiState.CaptiveUrlDetected("http://captive.ganainy.online")
         updateUrl("http://captive.ganainy.online")*/
    }


    fun getCaptivePortalAddress(showToast: (message: String, style: ToastStyle) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val portalUrl = detectCaptivePortal(context)
                if (portalUrl != null) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected
                    updateUrl(portalUrl)
                    val sessionId = sessionManager.getCurrentSessionId()
                    if (sessionId != null) {
                        sessionManager.savePortalUrl(
                            portalUrl = portalUrl,
                            sessionId = sessionId
                        )
                    } else {
                        showToast(context.getString(R.string.unknown_session_id), ToastStyle.ERROR)
                    }
                    detectLocalOrRemoteCaptivePortal(
                        context,
                        showToast = showToast
                    )

                } else {
                    _uiState.value =
                        AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value =
                        AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
                }
            }
        }
    }

    private fun detectLocalOrRemoteCaptivePortal(
        context: Context,
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        val analyzer = LocalOrRemoteCaptiveChecker(context)

        viewModelScope.launch {
            try {
                val result = analyzer.analyzePortal()

                val sessionId = sessionManager.getCurrentSessionId()
                if (sessionId != null) {
                    sessionManager.saveIsCaptiveLocal(
                        value = result.isLocal,
                        sessionId = sessionId
                    )
                } else {
                    showToast(context.getString(R.string.unknown_session_id), ToastStyle.ERROR)
                }
            } catch (e: IOException) {
                println("Failed to analyze portal: ${e.message}")
            }
        }

    }

    // Update the URL, which will be passed to the UI to show in the WebView
    fun updateUrl(newUrl: String) {
        _portalUrl.value = newUrl
    }

    //save the request including body to local db
    suspend fun saveWebViewRequest(request: WebViewRequest) {
        val currentSessionId = sessionManager.getCurrentSessionId()
        val customWebViewRequest =
            getCustomWebViewRequestFromWebViewRequest(request, currentSessionId)
        repository.insertRequest(
            customWebViewRequest
        )
    }

    private fun getCustomWebViewRequestFromWebViewRequest(
        request: WebViewRequest,
        sessionId: String?
    ): CustomWebViewRequestEntity {
        return CustomWebViewRequestEntity(
            sessionId = sessionId,
            url = request.url,
            method = request.method,
            headers = request.headers.toString(),
            body = request.body,
            type = request.type.name,
        )
    }


    //save the request with no body to local db
    suspend fun saveWebResourceRequest(request: WebResourceRequest?) {
        if (request != null) {
            val currentSessionId = sessionManager.getCurrentSessionId()
            val customWebViewRequest =
                getCustomWebViewRequestFromWebResourceRequest(request, currentSessionId)
            repository.insertRequest(
                customWebViewRequest
            )
        }
    }

    private fun getCustomWebViewRequestFromWebResourceRequest(
        request: WebResourceRequest,
        sessionId: String?
    ): CustomWebViewRequestEntity {
        return CustomWebViewRequestEntity(
            sessionId = sessionId,
            url = request.url.toString(),
            method = request.method,
            headers = request.requestHeaders.toString(),
        )
    }

    fun switchWebViewType(
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        when (_webViewType.value) {
            WebViewType.NormalWebView -> _webViewType.value = WebViewType.CustomWebView
            WebViewType.CustomWebView -> _webViewType.value = WebViewType.NormalWebView
        }
        showToast(context.getString(R.string.switched_detection_method), ToastStyle.SUCCESS)
    }

    fun saveWebpageContent(
        webView: WebView,
        url: String,
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        viewModelScope.launch {
            val currentSessionId = sessionManager.getCurrentSessionId()
            if (currentSessionId == null) {
                showToast(context.getString(R.string.unknown_session_id), ToastStyle.ERROR)
            } else {
                // Capture HTML content
                webView.evaluateJavascript(
                    "(function() { return document.documentElement.outerHTML; })();",
                ) { html ->
                    // Capture JavaScript content
                    webView.evaluateJavascript(
                        """
                (function() {
                    var scripts = document.getElementsByTagName('script');
                    var jsContent = '';
                    for(var i = 0; i < scripts.length; i++) {
                        if(scripts[i].src) {
                            jsContent += '// External Script: ' + scripts[i].src + '\n';
                        } else {
                            jsContent += scripts[i].innerHTML + '\n';
                        }
                    }
                    return jsContent;
                })();
                """.trimIndent()
                    ) { javascript ->
                        // Save to database
                        viewModelScope.launch(Dispatchers.IO) {
                            val content = WebpageContentEntity(
                                url = url,
                                htmlContent = html.unescapeJsonString(),
                                jsContent = javascript.unescapeJsonString(),
                                sessionId = currentSessionId,
                                timestamp = System.currentTimeMillis(),
                            )
                            repository.insertWebpageContent(content)
                        }
                    }
                }
            }
        }
    }


    private suspend fun hasFullInternetAccess(context: Context): Boolean {
        // Check network connectivity first
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isNetworkConnected = networkCapabilities?.run {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                    ))
        } == true

        // If not connected, return false
        if (!isNetworkConnected) {
            return false
        }

        // Try to ping a website to check if internet is accessible
        return try {
            val url = URL("https://www.google.com") // Use any reliable website
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 2000 // Set timeout to 2 seconds
                readTimeout = 2000
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == HttpURLConnection.HTTP_OK // Check if we received a successful response
        } catch (e: Exception) {
            false // If any error occurs (no internet, timeout, etc.), return false
        }
    }

    fun stopAnalysis(onUncompletedAnalysis: () -> Unit) {
        _uiState.value = AnalysisUiState.Loading(R.string.processing_request)
        viewModelScope.launch(Dispatchers.IO) {
            if (hasFullInternetAccess(context)) {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.AnalysisComplete
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected
                    onUncompletedAnalysis()
                }
            }
        }

    }


    fun takeScreenshot(webView: WebView, url: String) {
        // Launch a coroutine on the Main dispatcher for WebView operations
        viewModelScope.launch(Dispatchers.Main) {
            // Get session ID on IO thread
            val currentSessionId = withContext(Dispatchers.IO) {
                sessionManager.getCurrentSessionId()
            }

            // Capture bitmap on Main thread since it involves WebView
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android 8.0 (API 26) or later
                webView.capturePicture()?.let { picture ->
                    if (picture.width > 0 && picture.height > 0) {
                        Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
                            .apply { picture.draw(Canvas(this)) }
                    } else null
                }
            } else {
                // Fallback for older versions
                webView.isDrawingCacheEnabled = true
                val drawingCache = webView.drawingCache
                if (drawingCache != null && drawingCache.width > 0 && drawingCache.height > 0) {
                    Bitmap.createBitmap(drawingCache)
                } else null
            }

            // Switch to IO thread for file operations and database insertion
            withContext(Dispatchers.IO) {
                bitmap?.let {
                    val directory =
                        File(webView.context.getExternalFilesDir(null), "$currentSessionId/screenshots")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val fileName = "${System.currentTimeMillis()}.png"
                    val file = File(directory, fileName)

                    try {
                        FileOutputStream(file).use { out ->
                            it.compress(Bitmap.CompressFormat.PNG, 100, out)
                            Log.i("WebViewScreenshot", "Screenshot saved to: ${file.absolutePath}")
                        }

                        // Insert into the database
                        val screenshotEntity = ScreenshotEntity(
                            sessionId = currentSessionId!!,
                            timestamp = System.currentTimeMillis(),
                            path = file.absolutePath,
                            size = "${file.length()} bytes",
                            url = url
                        )
                        repository.insertScreenshot(screenshotEntity) // Already on IO thread
                    } catch (e: IOException) {
                        Log.e("WebViewScreenshot", "Error saving screenshot: ${e.message}")
                    }
                } ?: Log.e("WebViewScreenshot", "Invalid screenshot bitmap dimensions")
            }
        }
    }


    fun updateShowedHint(showedHint: Boolean) {
        _showedHint.value = showedHint
    }


}

private fun String.unescapeJsonString(): String {
    return if (this.startsWith("\"") && this.endsWith("\"")) {
        this.substring(1, this.length - 1)
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    } else {
        this
    }
}


class AnalysisViewModelFactory(
    private val application: Application,
    private val sessionManager: NetworkSessionManager,
    private val repository: NetworkSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(
                application,
                sessionManager,
                repository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


