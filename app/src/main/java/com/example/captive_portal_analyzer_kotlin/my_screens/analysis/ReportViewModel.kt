package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter.formatIpAddress
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.dataclasses.BypassVulnerability
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.dataclasses.Credential
import com.example.captive_portal_analyzer_kotlin.dataclasses.DefaultCredentialResult
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkInformation
import com.example.captive_portal_analyzer_kotlin.dataclasses.SecurityHeadersAnalysis
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionTokenAnalysis
import com.example.captive_portal_analyzer_kotlin.dataclasses.TLSAnalysis
import com.example.captive_portal_analyzer_kotlin.utils.CaptivePortalDetector.detectPortalURL
import com.example.captive_portal_analyzer_kotlin.utils.Utils
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.calculateChannel
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.getDnsServers
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.getSecurityType
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.isNetworkSecure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import java.util.Arrays
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.log2
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.update

sealed class ReportUiState {
    object Loading : ReportUiState()
    object Initial : ReportUiState()
    data class Error(val messageStringResource: Int) : ReportUiState()
}


class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Initial)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()


    init {

    }




}


/*
class FeedViewModelFactory(private val repository: IDataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LandingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LandingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}*/
