package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineWebpageContentRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


sealed class ReportUiState {
    object Loading : ReportUiState()
    data class Error(val messageStringResource: Int) : ReportUiState()
}


class ReportViewModel(
    application: Application,
    val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    val offlineWebpageContentRepository: OfflineWebpageContentRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()


    init {

    }




}


class ReportViewModelFactory(
    private val application: Application,
    private val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    private val offlineWebpageContentRepository: OfflineWebpageContentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(application,offlineCustomWebViewRequestsRepository,offlineWebpageContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}