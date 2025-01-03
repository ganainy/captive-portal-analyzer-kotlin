package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import SessionData
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SessionViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {


    private val _isUploading = MutableStateFlow<Boolean>(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()


    init {

    }

    fun uploadSession(
        sessionData: SessionData,
        showToast: (message: String, style: ToastStyle) -> Unit,
        updateClickedSession: (sessionData: SessionData) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.value = true
            repository.uploadSessionData(sessionData.session.sessionId)
                .onSuccess {
                    _isUploading.value = false
                    updateClickedSession(sessionData.copy(session = sessionData.session.copy(isUploadedToRemoteServer = true)))
                }
                .onFailure { apiResponse ->
                    _isUploading.value = false
                    withContext(Dispatchers.Main){
                        apiResponse.message?.let { showToast(it, ToastStyle.ERROR) }
                    }
                }
        }
    }


}

class SessionViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(
                application,
                repository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
