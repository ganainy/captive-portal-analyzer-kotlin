package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.example.captive_portal_analyzer_kotlin.R
import detectCaptivePortal

sealed class AnalysisUiState {
    data class Loading (val messageStringResource: Int) : AnalysisUiState()
    data class CaptiveUrlDetected(val captiveUrl: String) : AnalysisUiState()
    data class Error(val messageStringResource: Int) : AnalysisUiState()
}


class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _portalUrl = mutableStateOf("") // Default URL
    val portalUrl: State<String> get() = _portalUrl

    private val _isWebViewLoading = mutableStateOf(false)
    val isWebViewLoading: State<Boolean> get() = _isWebViewLoading

    init {
        getCaptivePortalAddress()
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

    //update the url which will be passed to ui to show in the webview
    fun updateUrl(newUrl: String) {
        _portalUrl.value = newUrl
    }

    fun setWebviewLoadingStatus(isLoading: Boolean) {
        if (isLoading){
            _isWebViewLoading.value = true
        }else{
            _isWebViewLoading.value = false
        }

    }



}



