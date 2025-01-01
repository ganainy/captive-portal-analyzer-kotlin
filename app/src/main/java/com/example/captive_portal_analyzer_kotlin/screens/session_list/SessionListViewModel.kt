package com.example.captive_portal_analyzer_kotlin.screens.session_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.room.network_session.OfflineNetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.OfflineScreenshotRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.WebpageContentEntity
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


sealed class SessionListUiState {
    object Loading : SessionListUiState()
    object Empty : SessionListUiState()
    object Success : SessionListUiState()
    data class Error(val messageStringResource: Int) : SessionListUiState()
}


class SessionListViewModel(
    application: Application,
    private val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    private val offlineWebpageContentRepository: OfflineWebpageContentRepository,
    private val offlineScreenshotRepository: OfflineScreenshotRepository,
    private val offlineNetworkSessionRepository: NetworkSessionRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SessionListUiState>(SessionListUiState.Loading)
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()


    private val _sessions = MutableStateFlow<List<NetworkSessionEntity>?>(null)
    val sessions: StateFlow<List<NetworkSessionEntity>?> get() = _sessions.asStateFlow()

    private val _sessionCustomWebViewRequests = MutableStateFlow<List<CustomWebViewRequestEntity>>(
        emptyList()
    )
    val sessionCustomWebViewRequests: StateFlow<List<CustomWebViewRequestEntity>> get() = _sessionCustomWebViewRequests.asStateFlow()


    private val _sessionWebpageContent = MutableStateFlow<List<WebpageContentEntity>>(
        emptyList()
    )
    val sessionWebpageContent: StateFlow<List<WebpageContentEntity>> get() = _sessionWebpageContent.asStateFlow()


    private val _sessionScreenshot = MutableStateFlow<List<ScreenshotEntity>>(
        emptyList()
    )
    val sessionScreenshot: StateFlow<List<ScreenshotEntity>> get() = _sessionScreenshot.asStateFlow()


    init {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = offlineNetworkSessionRepository.getAllSessions()
            if (sessions.isNullOrEmpty()) {
                _uiState.value = SessionListUiState.Empty
            } else {
                _sessions.value = sessions
                _uiState.value = SessionListUiState.Success
                sessions.forEach { session ->

                    val sessionCustomWebViewRequests =
                        offlineCustomWebViewRequestsRepository.getSessionCustomWebViewRequest(
                            session.sessionId
                        )
                    _sessionCustomWebViewRequests.value = sessionCustomWebViewRequests.first()

                    val sessionWebpageContent =
                        offlineWebpageContentRepository.getAllContentForSessionId(session.sessionId)
                    _sessionWebpageContent.value = sessionWebpageContent.first()

                    val sessionScreenshot =
                        offlineScreenshotRepository.getAllScreenshotsForSession(session.sessionId)
                    _sessionScreenshot.value = sessionScreenshot.first()
                }


            }
        }

    }
}


class SessionListViewModelFactory(
    private val application: Application,
    private val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    private val offlineWebpageContentRepository: OfflineWebpageContentRepository,
    private val offlineScreenshotRepository: OfflineScreenshotRepository,
    private val offlineNetworkSessionRepository: OfflineNetworkSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionListViewModel(
                application,
                offlineCustomWebViewRequestsRepository,
                offlineWebpageContentRepository,
                offlineScreenshotRepository,
                offlineNetworkSessionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}