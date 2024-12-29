package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
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
import detectCaptivePortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed class AnalysisUiState {
    data class Loading(val messageStringResource: Int) : AnalysisUiState()
    data class CaptiveUrlDetected(val captiveUrl: String) : AnalysisUiState()
    data class Error(val messageStringResource: Int) : AnalysisUiState()
}

class AnalysisViewModel(
    application: Application,
    val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    val offlineWebpageContentRepository: OfflineWebpageContentRepository
)
    : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _portalUrl = MutableStateFlow("") // Default URL, changed to MutableStateFlow for consistency
    val portalUrl: StateFlow<String> get() = _portalUrl.asStateFlow() // Changed to StateFlow for consistency

    private var logcatProcess: Process? = null
    private var isReading = true

    private val _shouldShowNormalWebView = MutableStateFlow<Boolean>(false)
    val shouldShowNormalWebView = _shouldShowNormalWebView.asStateFlow()




    init {

        getCaptivePortalAddress()

       /* //testing
         _uiState.value = AnalysisUiState.CaptiveUrlDetected("http://captive.ganainy.online")
        updateUrl("http://captive.ganainy.online")
         viewModelScope.launch { showDbContent() }*/
    }




    private suspend fun showDbContent() {
        offlineCustomWebViewRequestsRepository.getAllItemsStream().
        collect { requests ->
            Log.i("DatabaseContent", requests.toString())
        }


    }

    private fun getCaptivePortalAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val portalUrl = detectCaptivePortal(context)
                if (portalUrl != null) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected(portalUrl)
                    updateUrl(portalUrl)
                } else {
                    _uiState.value = AnalysisUiState.Error(R.string.no_captive_portal_detected)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AnalysisUiState.Error(R.string.no_captive_portal_detected)
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
    private val offlineWebpageContentRepository: OfflineWebpageContentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(application,offlineCustomWebViewRequestsRepository,offlineWebpageContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


