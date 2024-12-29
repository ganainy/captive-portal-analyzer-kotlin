package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.room.WebpageContent
import com.example.captive_portal_analyzer_kotlin.room.toCustomWebViewRequest
import com.example.captive_portal_analyzer_kotlin.utils.LocalOrRemoteCaptiveChecker
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import detectCaptivePortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


sealed class AnalysisUiState {
    data class Loading(val messageStringResource: Int) : AnalysisUiState()
    data class CaptiveUrlDetected(val captiveUrl: String) : AnalysisUiState()
    data class Error(val messageStringResource: Int) : AnalysisUiState()
}

class AnalysisViewModel(
    application: Application,
    private val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    private val offlineWebpageContentRepository: OfflineWebpageContentRepository,
    private val sessionManager: NetworkSessionManager
)
    : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _portalUrl = MutableStateFlow<String?>(null)
    val portalUrl: StateFlow<String?> get() = _portalUrl.asStateFlow()

    private val _shouldShowNormalWebView = MutableStateFlow<Boolean>(false)
    val shouldShowNormalWebView = _shouldShowNormalWebView.asStateFlow()



    private val _toast = MutableStateFlow<Pair<Boolean, String>?>(null)
    val toast = _toast.asStateFlow()

    init {

        getCaptivePortalAddress()

       /* //testing
         _uiState.value = AnalysisUiState.CaptiveUrlDetected("http://captive.ganainy.online")
        updateUrl("http://captive.ganainy.online")*/
    }




    fun getCaptivePortalAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val portalUrl = detectCaptivePortal(context)
                if (portalUrl != null) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected(portalUrl)
                    updateUrl(portalUrl)
                    sessionManager.savePortalUrl(portalUrl)
                    detectLocalOrRemoteCaptivePortal(context)
                    _toast.value = Pair(true, context.getString(R.string.detected_captive_portal_url))


                } else {
                    _uiState.value = AnalysisUiState.Error(R.string.no_captive_portal_detected)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                _uiState.value = AnalysisUiState.Error(R.string.no_captive_portal_detected)
            }
            }
        }
    }

    private fun detectLocalOrRemoteCaptivePortal(context: Context) {
        val analyzer = LocalOrRemoteCaptiveChecker(context)

        viewModelScope.launch {
            try {
                val result = analyzer.analyzePortal()
                sessionManager.saveIsCaptiveLocal(result.isLocal)
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
        val bssid = getCurrentNetworkUniqueIdentifier()

        offlineCustomWebViewRequestsRepository.insertItem(request.toCustomWebViewRequest(bssid))
    }

    //save the request with no body to local db
    suspend fun saveWebResourceRequest(request: WebResourceRequest?) {
        if (request != null) {
            val bssid = getCurrentNetworkUniqueIdentifier()
            offlineCustomWebViewRequestsRepository.insertItem(request.toCustomWebViewRequest(bssid))
        }
    }

    fun getCurrentNetworkUniqueIdentifier(): String {
        val wifiMan = context.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager
        val wifiInfo = wifiMan.connectionInfo
        val bssid = wifiInfo.bssid
        return bssid
    }

    fun showNormalWebView(shouldShowNormalWebView: Boolean) {
        _shouldShowNormalWebView.value=shouldShowNormalWebView
        _toast.value = Pair(true , context.getString(R.string.switched_detection_method))
        }

    fun saveWebpageContent(webView: WebView,url: String) {
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
                    val content = WebpageContent(
                        url = url,
                        html = html.unescapeJsonString(),
                        javascript = javascript.unescapeJsonString()
                    )
                    offlineWebpageContentRepository.insertContent(content)
                }
            }
        }

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
    private val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    private val offlineWebpageContentRepository: OfflineWebpageContentRepository,
    private val sessionManager: NetworkSessionManager,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(application,offlineCustomWebViewRequestsRepository,offlineWebpageContentRepository,sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


