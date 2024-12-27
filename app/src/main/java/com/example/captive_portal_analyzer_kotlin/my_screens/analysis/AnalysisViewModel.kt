package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.toCustomWebViewRequest
import detectCaptivePortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


sealed class AnalysisUiState {
    data class Loading(val messageStringResource: Int) : AnalysisUiState()
    data class CaptiveUrlDetected(val captiveUrl: String) : AnalysisUiState()
    data class Error(val messageStringResource: Int) : AnalysisUiState()
}

class AnalysisViewModel(application: Application,val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository)
    : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _portalUrl = MutableStateFlow("") // Default URL, changed to MutableStateFlow for consistency
    val portalUrl: StateFlow<String> get() = _portalUrl.asStateFlow() // Changed to StateFlow for consistency

    private var logcatProcess: Process? = null
    private var isReading = true

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries = _logEntries.asStateFlow()


    data class LogEntry(
        val request: String
    )


    init {
        startLogCapture()
        getCaptivePortalAddress()
        // viewModelScope.launch { showDbContent() }
    }

    private fun startLogCapture() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear existing logs first
                Runtime.getRuntime().exec("logcat -c")

                // Start continuous log monitoring
                logcatProcess = Runtime.getRuntime().exec("logcat")
                val reader = BufferedReader(InputStreamReader(logcatProcess?.inputStream))

                val logPattern = ".*RequestInspectorWebView.*Sending request from WebView: (.*)".toRegex()

                while (isReading) {
                    val line = reader.readLine()
                    if (line != null) {
                        logPattern.find(line)?.let { matchResult ->
                            val request = matchResult.groupValues[1]
                            val newEntry = LogEntry(request)

                            val currentList = _logEntries.value.toMutableList()
                            currentList.add(newEntry)
                            _logEntries.value = currentList
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Error while reading logs", e)
            }
        }
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
        if (newUrl != _portalUrl.value) {
            _portalUrl.value = newUrl
        }
    }

    //save the request body to local db
    suspend fun saveRequest(request: WebViewRequest) {
        val bssid = getCurrentNetworkUniqueIdentifier()

        offlineCustomWebViewRequestsRepository.insertItem(request.toCustomWebViewRequest(bssid))
    }

    fun getCurrentNetworkUniqueIdentifier(): String {
        val wifiMan = context.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager
        val wifiInfo = wifiMan.connectionInfo
        val bssid = wifiInfo.bssid
        return bssid
    }
}


class AnalysisViewModelFactory(
    private val application: Application,
    private val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(application,offlineCustomWebViewRequestsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


