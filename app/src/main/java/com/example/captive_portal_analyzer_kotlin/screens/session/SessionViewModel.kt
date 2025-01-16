package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UploadState {
    object Loading : UploadState() //loading session from repository to view in the screen
    object NeverUploaded : UploadState() //session never uploaded to server
    object Uploading : UploadState() // session is uploading to server
    object AlreadyUploaded : UploadState() //session previously already uploaded to server
    object Success : UploadState() //session uploaded to server successfully
    data class Error(val message: String) : UploadState()
}

class SessionViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
    private val clickedSessionId: String,
) : AndroidViewModel(application) {

    // State holder
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Loading)
    val uploadState = _uploadState.asStateFlow()

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
                        .collect { sessionData ->
                            _sessionFlow.value = sessionData
                            if (sessionData.session.isUploadedToRemoteServer) {
                                _uploadState.value = UploadState.AlreadyUploaded
                            }else{
                                _uploadState.value = UploadState.NeverUploaded
                            }
                        }
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(getApplication<Application>().getString( R.string.error_loading_session))

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
            _uploadState.value = UploadState.Uploading

            repository.uploadSessionData(sessionData.session.networkId)
                .onSuccess {
                    try {
                        repository.updateSession(sessionData.session.copy(isUploadedToRemoteServer = true))
                        _uploadState.value = UploadState.Success
                    } catch (e: Exception) {
                        _uploadState.value = UploadState.Error(e.localizedMessage ?:
                        getApplication<Application>().getString(R.string.error_uploading_session))
                        repository.updateSession(sessionData.session.copy(isUploadedToRemoteServer = false))
                        return@launch
                    }
                }
                .onFailure { apiResponse ->
                    _uploadState.value = UploadState.Error(apiResponse.localizedMessage ?:
                    getApplication<Application>().getString(R.string.error_uploading_session))
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
