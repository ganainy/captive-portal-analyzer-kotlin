package com.example.captive_portal_analyzer_kotlin.screens.session_list

import NetworkSessionRepository
import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

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

    /**
     * Deletes a session and all its related data from the repository.
     * Expected to be called after user confirmation.
     * @param networkId The networkId (used as sessionId) of the session to delete.
     */
    fun deleteSession(networkId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First, potentially delete associated files (PCAP, Screenshots, Web Content)
                // This requires getting the full SessionData or specific file paths first
                val sessionToDelete = repository.getSessionByNetworkId(networkId)
                if (sessionToDelete != null) {
                    deleteAssociatedFiles(sessionToDelete) // Call helper to delete files
                } else {
                    Log.w("DeleteSession", "Session $networkId not found for file deletion, proceeding with DB deletion.")
                }

                // Then, delete from the database using the repository method
                repository.deleteSessionAndRelatedData(networkId)
                Log.i("SessionListViewModel", "Delete operation initiated for session ID: $networkId")
                // The list will automatically update due to the Flow collection in init block
            } catch (e: Exception) {
                Log.e("SessionListViewModel", "Error deleting session ID: $networkId", e)
                // Optionally update UI state to show a deletion error, maybe via a temporary event flow or toast
                // For now, just logging the error.
            }
        }
    }

    /**
     * Helper function to delete files associated with a session.
     * Needs access to Context for file operations.
     */
    private fun deleteAssociatedFiles(session: NetworkSessionEntity) {
        val context = getApplication<Application>().applicationContext
        Log.d("DeleteSession", "Attempting to delete associated files for session: ${session.networkId}")

        // 1. Delete PCAP file
        session.pcapFilePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    if (file.delete()) {
                        Log.i("DeleteSession", "Deleted PCAP file: $path")
                    } else {
                        Log.w("DeleteSession", "Failed to delete PCAP file: $path")
                    }
                } else {
                    Log.w("DeleteSession", "PCAP file not found for deletion: $path")
                }
            } catch (e: Exception) {
                Log.e("DeleteSession", "Error deleting PCAP file: $path", e)
            }
        } ?: Log.d("DeleteSession", "No PCAP file path found for session ${session.networkId}")

        // 2. Delete Screenshot files (Requires fetching ScreenshotEntities first)
        viewModelScope.launch(Dispatchers.IO) { // Launch separate IO scope for DB access
            try {
                val screenshots = repository.getSessionScreenshots(session.networkId).first() // Get list for this session
                screenshots.forEach { screenshot ->
                    try {
                        val file = File(screenshot.path)
                        if (file.exists()) {
                            if (file.delete()) {
                                Log.i("DeleteSession", "Deleted screenshot file: ${screenshot.path}")
                            } else {
                                Log.w("DeleteSession", "Failed to delete screenshot file: ${screenshot.path}")
                            }
                        } else {
                            Log.w("DeleteSession", "Screenshot file not found for deletion: ${screenshot.path}")
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteSession", "Error deleting screenshot file: ${screenshot.path}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("DeleteSession", "Error fetching screenshots for deletion for session ${session.networkId}", e)
            }

            // 3. Delete Webpage Content files (Requires fetching WebpageContentEntities first)
            try {
                val webContents = repository.getSessionWebpageContent(session.networkId).first()
                webContents.forEach { content ->
                    // Delete HTML file
                    try {
                        val htmlFile = File(content.htmlContentPath)
                        if (htmlFile.exists()) {
                            if (htmlFile.delete()) Log.i("DeleteSession", "Deleted HTML file: ${content.htmlContentPath}")
                            else Log.w("DeleteSession", "Failed to delete HTML file: ${content.htmlContentPath}")
                        } else {
                            Log.w("DeleteSession", "HTML file not found for deletion: ${content.htmlContentPath}")
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteSession", "Error deleting HTML file: ${content.htmlContentPath}", e)
                    }
                    // Delete JS file
                    try {
                        val jsFile = File(content.jsContentPath)
                        if (jsFile.exists()) {
                            if (jsFile.delete()) Log.i("DeleteSession", "Deleted JS file: ${content.jsContentPath}")
                            else Log.w("DeleteSession", "Failed to delete JS file: ${content.jsContentPath}")
                        } else {
                            Log.w("DeleteSession", "JS file not found for deletion: ${content.jsContentPath}")
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteSession", "Error deleting JS file: ${content.jsContentPath}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("DeleteSession", "Error fetching webpage content for deletion for session ${session.networkId}", e)
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