package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SessionViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
    private val clickedSessionId: String,
) : AndroidViewModel(application) {

    //is the session being uploaded to the server
    private val _isUploading = MutableStateFlow<Boolean>(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    //keep getting any new changes to the session to view in the screen
    private val _sessionFlow = MutableStateFlow<SessionData?>(null)
    val sessionData = _sessionFlow.asStateFlow()  // Use asStateFlow() for public exposure

    init {
        loadSessionData()
    }


    private fun loadSessionData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clickedSessionId.let { sessionId ->
                    // Just collect the Flow from repository
                    repository.getCompleteSessionData(sessionId)
                        .collect { session ->
                            _sessionFlow.value = session
                        }
                }
            } catch (e: Exception) {
                _sessionFlow.value = null
            }
        }
    }

    fun toggleScreenshotPrivacyOrToSrelated(screenshot: ScreenshotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScreenshot(
                screenshot.copy(isPrivacyOrTosRelated = !screenshot.isPrivacyOrTosRelated)
            )
        }
    }


    fun uploadSession(
        sessionData: SessionData,
        showToast: (message: String, style: ToastStyle) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.value = true
            repository.updateSession(sessionData.session.copy(isUploadedToRemoteServer = true))
            repository.uploadSessionData(sessionData.session.sessionId)
                .onSuccess {
                    _isUploading.value = false
                }
                .onFailure { apiResponse ->
                    _isUploading.value = false
                    withContext(Dispatchers.Main) {
                        repository.updateSession(sessionData.session.copy(isUploadedToRemoteServer = false))
                        apiResponse.message?.let { showToast(it, ToastStyle.ERROR) }
                    }
                }
        }
    }


}

class SessionViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
    private val clickedSessionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(
                application,
                repository,
                clickedSessionId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
