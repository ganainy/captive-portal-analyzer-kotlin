package com.example.captive_portal_analyzer_kotlin.screens.session_list

import NetworkSessionRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Enum to represent the state of the session list.
 *
 * This enum is used by the [SessionListViewModel] to communicate the state of the session list
 * to the UI.
 *
 * The following states are possible:
 *
 * - [Loading] : The session list is currently being loaded.
 * - [Empty] : The session list is empty.
 * - [Success] : The session list was successfully loaded.
 * - [Error] : An error occurred while loading the session list.
 * The [Error] state contains a resource string ID that can be used to display the error
 * message to the user.
 */
sealed class SessionListUiState {
    object Loading : SessionListUiState()
    object Empty : SessionListUiState()
    object Success : SessionListUiState()
    data class Error(val messageStringResource: Int) : SessionListUiState()
}

/**
 * The [SessionListViewModel] will use the [repository] to load the complete session data list into
 *
 * the [SessionData] objects which contain the [NetworkSessionEntity], [WebpageContentEntity] and
 * [ScreenshotEntity] for each session.
 *
 * @param application The application context.
 * @param repository The [NetworkSessionRepository] used to load the sessions.
 */
class SessionListViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {

    // _sessionsFlow is a MutableStateFlow that stores the list of SessionData objects.
    private val _sessionsFlow = MutableStateFlow<List<SessionData>?>(null)

    // sessionDataList is a StateFlow that observes the _sessionsFlow.
    // It maps the list of SessionData objects to null if the list is empty.
    val sessionDataList = _sessionsFlow
        .map { sessions ->
            if (sessions.isNullOrEmpty()) null else sessions
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // _uiState is a MutableStateFlow that stores the state of the session list.
    private val _uiState = MutableStateFlow<SessionListUiState>(SessionListUiState.Loading)

    // uiState is a StateFlow that observes the _uiState.
    // It uses the stateIn operator to configure the StateFlow.
    // The SharingStarted.WhileSubscribed strategy is used to share the StateFlow
    // while it is subscribed. The initial value is set to SessionListUiState.Loading.
    val uiState = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionListUiState.Loading
        )
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collects the complete session data list from the repository
                repository.getCompleteSessionDataList().collect { sessions ->
                    // Updates the sessions flow with the retrieved sessions
                    _sessionsFlow.value = sessions
                    // Sets the UI state based on whether sessions are empty or not
                    _uiState.value = if (sessions.isNullOrEmpty()) {
                        SessionListUiState.Empty // Sets state to Empty if no sessions
                    } else {
                        SessionListUiState.Success // Sets state to Success if sessions are present
                    }
                }
            } catch (e: Exception) {
                // Handles exceptions by setting sessions flow to null
                _sessionsFlow.value = null
                // Sets UI state to Error with a specific error message resource
                _uiState.value = SessionListUiState.Error(R.string.error_retrieving_data)
            }
        }
    }
}


/**
 * A factory that creates [SessionListViewModel] instances.
 *
 * @param application The application context.
 * @param repository The [NetworkSessionRepository] used to load the sessions.
 */
class SessionListViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
) : ViewModelProvider.Factory {

    /**
     * Creates a new [SessionListViewModel] instance.
     *
     * @param modelClass The class of the ViewModel to be created.
     * @return A new instance of the requested ViewModel class.
     */
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