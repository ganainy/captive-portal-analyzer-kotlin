package com.example.captive_portal_analyzer_kotlin.screens.session_list

import NetworkSessionRepository
import SessionData
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed class SessionListUiState {
    object Loading : SessionListUiState()
    object Empty : SessionListUiState()
    object Success : SessionListUiState()
    data class Error(val messageStringResource: Int) : SessionListUiState()
}


class SessionListViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {

    private val _sessionsFlow = MutableStateFlow<List<SessionData>?>(null)
    val sessionDataList = _sessionsFlow
        .map { sessions ->
            if (sessions.isNullOrEmpty()) null else sessions
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow<SessionListUiState>(SessionListUiState.Loading)
    val uiState = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionListUiState.Loading
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getCompleteSessionDataList().collect { sessions ->
                    _sessionsFlow.value = sessions
                    _uiState.value = if (sessions.isNullOrEmpty()) {
                        SessionListUiState.Empty
                    } else {
                        SessionListUiState.Success
                    }
                }
            } catch (e: Exception) {
                _sessionsFlow.value = null
                _uiState.value = SessionListUiState.Error(R.string.error_retrieving_data)
            }
        }
    }
}



class SessionListViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionListViewModel(
                application,
                repository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}