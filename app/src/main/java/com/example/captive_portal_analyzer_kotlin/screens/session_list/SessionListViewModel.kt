// FILE: SessionListViewModel.kt
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
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Enum to represent the state of the session list.
 *
 * This enum is used by the [SessionListViewModel] to communicate the state of the session list
 * to the UI.
 */
sealed class SessionListUiState {
    object Loading : SessionListUiState()
    object Empty : SessionListUiState() // Represents overall empty state (no sessions at all)
    object Success : SessionListUiState() // Represents that data loading succeeded (list might be empty for a filter)
    data class Error(val messageStringResource: Int) : SessionListUiState()
}

/**
 * Enum to represent the filtering options for the session list.
 */
enum class SessionFilterType {
    CAPTIVE, NORMAL
}

/**
 * The [SessionListViewModel] loads session data and manages filtering logic.
 *
 * It fetches all [SessionData] objects and provides a filtered list based on the
 * selected [SessionFilterType]. It also handles session deletion.
 *
 * @param application The application context.
 * @param repository The [NetworkSessionRepository] used to load and delete sessions.
 */
class SessionListViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {

    // Raw data flow holding all sessions fetched from the repository.
    private val _sessionsFlow = MutableStateFlow<List<SessionData>?>(null)

    // State flow holding the currently selected filter type. Defaults to CAPTIVE.
    private val _selectedFilter = MutableStateFlow(SessionFilterType.CAPTIVE)
    val selectedFilter: StateFlow<SessionFilterType> = _selectedFilter.asStateFlow()

    // UI state reflecting the overall data loading process (Loading, Success, Empty, Error).
    private val _uiState = MutableStateFlow<SessionListUiState>(SessionListUiState.Loading)
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    // Combines the raw sessions and the selected filter to produce the filtered list for the UI.
    val filteredSessionDataList: StateFlow<List<SessionData>?> =
        combine(_sessionsFlow, _selectedFilter) { sessions, filter ->
            sessions?.let { // Only filter if sessions are loaded
                when (filter) {
                    SessionFilterType.CAPTIVE -> it.filter { session -> session.isCaptivePortal() }
                    SessionFilterType.NORMAL -> it.filter { session -> !session.isCaptivePortal() }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Start with null until data is loaded and filtered
        )

    init {
        // Launch a coroutine to fetch all session data when the ViewModel is created.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getCompleteSessionDataList().collect { sessions ->
                    _sessionsFlow.value = sessions // Update the raw sessions flow
                    // Update the overall UI state based on the *total* number of sessions fetched.
                    _uiState.value = when {
                        sessions == null -> SessionListUiState.Loading // Should ideally not happen here, but safety check
                        sessions.isEmpty() -> SessionListUiState.Empty
                        else -> SessionListUiState.Success
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionListViewModel", "Error fetching session data", e)
                _sessionsFlow.value = null // Ensure flow reflects error state
                _uiState.value = SessionListUiState.Error(R.string.error_retrieving_data)
            }
        }
    }

    /**
     * Updates the selected filter type. The `filteredSessionDataList` flow will automatically
     * update based on this change.
     *
     * @param filterType The new filter to apply (CAPTIVE or NORMAL).
     */
    fun selectFilter(filterType: SessionFilterType) {
        _selectedFilter.value = filterType
    }

    /**
     * Deletes a session and all its related data (database entries and files).
     * Expected to be called after user confirmation.
     *
     * @param networkId The networkId (used as sessionId) of the session to delete.
     */
    fun deleteSession(networkId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch session details to get file paths before deleting DB entry
                val sessionToDelete = repository.getSessionByNetworkId(networkId)
                if (sessionToDelete != null) {
                    deleteAssociatedFiles(sessionToDelete) // Delete files first
                } else {
                    Log.w("DeleteSession", "Session $networkId not found for file deletion, proceeding with DB deletion.")
                }

                // Delete database records
                repository.deleteSessionAndRelatedData(networkId)
                Log.i("SessionListViewModel", "Delete operation initiated for session ID: $networkId")
                // The _sessionsFlow will automatically update due to the Flow collection in init,
                // which in turn updates the filtered list.
            } catch (e: Exception) {
                Log.e("SessionListViewModel", "Error deleting session ID: $networkId", e)
                // Optional: Consider emitting a temporary error state/event to the UI
                _uiState.value = SessionListUiState.Error(R.string.error_deleting_session)
            }
        }
    }

    /**
     * Helper function to determine if a session represents a captive portal interaction.
     * A session is considered captive if it has associated requests, webpage content, or screenshots.
     */
    private fun SessionData.isCaptivePortal(): Boolean {
        return this.requestsCount > 0 || this.webpageContentCount > 0 || this.screenshotsCount > 0
    }


    /**
     * Helper function to delete files associated with a session (PCAP, Screenshots, Web Content).
     */
    private fun deleteAssociatedFiles(session: NetworkSessionEntity) {
        // Context is available via getApplication() in AndroidViewModel
        Log.d("DeleteSession", "Attempting to delete associated files for session: ${session.networkId}")

        // Delete PCAP file
        session.pcapFilePath?.let { path ->
            deleteFile(path, "PCAP")
        } ?: Log.d("DeleteSession", "No PCAP file path for session ${session.networkId}")

        // Delete Screenshots and Web Content Files - Requires separate coroutine scope
        // as it involves further repository calls which should be off the main thread.
        viewModelScope.launch(Dispatchers.IO) {
            // Delete Screenshot files
            try {
                val screenshots = repository.getSessionScreenshots(session.networkId).first()
                screenshots.forEach { screenshot ->
                    deleteFile(screenshot.path, "Screenshot")
                }
            } catch (e: Exception) {
                Log.e("DeleteSession", "Error fetching/deleting screenshots for session ${session.networkId}", e)
            }

            // Delete Webpage Content files
            try {
                val webContents = repository.getSessionWebpageContent(session.networkId).first()
                webContents.forEach { content ->
                    deleteFile(content.htmlContentPath, "HTML")
                    deleteFile(content.jsContentPath, "JS")
                }
            } catch (e: Exception) {
                Log.e("DeleteSession", "Error fetching/deleting webpage content for session ${session.networkId}", e)
            }
        }
    }

    /**
     * Safely deletes a file at the given path and logs the outcome.
     * @param path The absolute path of the file to delete.
     * @param fileType A descriptive type string for logging (e.g., "PCAP", "Screenshot").
     */
    private fun deleteFile(path: String?, fileType: String) {
        if (path.isNullOrBlank()) {
            Log.w("DeleteSession", "$fileType file path is null or empty, cannot delete.")
            return
        }
        try {
            val file = File(path)
            if (file.exists()) {
                if (file.delete()) {
                    Log.i("DeleteSession", "Deleted $fileType file: $path")
                } else {
                    Log.w("DeleteSession", "Failed to delete $fileType file: $path")
                }
            } else {
                Log.w("DeleteSession", "$fileType file not found for deletion: $path")
            }
        } catch (e: SecurityException) {
            Log.e("DeleteSession", "Security error deleting $fileType file: $path", e)
        } catch (e: Exception) {
            Log.e("DeleteSession", "Error deleting $fileType file: $path", e)
        }
    }
}

/**
 * Factory for creating [SessionListViewModel] instances with dependencies.
 */
class SessionListViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionListViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}