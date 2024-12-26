package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acsbendi.requestinspectorwebview.WebViewRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.repository.DataRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.toCustomWebViewRequest
import detectCaptivePortal

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

    init {
        //getCaptivePortalAddress()
         viewModelScope.launch { showDbContent() }
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
        offlineCustomWebViewRequestsRepository.insertItem(request.toCustomWebViewRequest())
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


