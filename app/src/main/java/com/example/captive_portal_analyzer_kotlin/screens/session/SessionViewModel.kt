package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents the upload state of a session.
 */
sealed class SessionState {
    /**
     * Indicates that the session is being loaded from the repository to be viewed on the screen.
     */
    object Loading : SessionState()

    /**
     * Indicates that the session has never been uploaded to the remote server (firebase).
     */
    object NeverUploaded : SessionState()

    /**
     * Indicates that the session is currently being uploaded to the remote server (firebase).
     */
    object Uploading : SessionState()

    /**
     * Indicates that the session was previously already uploaded by user to the remote server (firebase).
     */
    object AlreadyUploaded : SessionState()

    /**
     * Indicates that the session was successfully uploaded to the remote server (firebase).
     */
    object Success : SessionState()

    /**
     * Represents an error state while loading session from DB.
     *
     * @property message A description of the error encountered during upload.
     */
    data class ErrorLoading(val message: String) : SessionState()

    /**
     * Represents an error state while uploading session to remote server.
     *
     * @property message A description of the error encountered during upload.
     */
    data class ErrorUploading(val message: String) : SessionState()
}

data class SessionUiData(
    val sessionData: SessionData? = null,
    val showFilteringBottomSheet: Boolean = false, // Show or hide the filtering bottom sheet
    val isBodyEmptyChecked: Boolean = false, // show or hide the requests with empty body
    val selectedMethods: List<Map<RequestMethod, Boolean>> = emptyList(), //show or hide requests with certain
    // methods GET,POST,PUT according to filters and boolean value to determine if filter is enabled or not
    val unfilteredRequests: List<CustomWebViewRequestEntity> = emptyList(), // contains all requests of the session to apply filtering to them
)

/**
 * ViewModel for managing the session data and upload state.
 *
 * @property application The application context.
 * @property repository The repository for network sessions.
 * @property clickedSessionId The ID of the session that was clicked.
 */
class SessionViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
    private val clickedSessionId: String?,
) : AndroidViewModel(application) {

    /**
     * Upload state of the session, can be NeverUploaded, AlreadyUploaded, or Error.
     */
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState = _sessionState.asStateFlow()

    /**
     * The session data that is currently being viewed on the screen.
     */
    private val _sessionUiData = MutableStateFlow(SessionUiData())
    val sessionUiData = _sessionUiData.asStateFlow()

    /**
     * This function is called when the ViewModel is constructed.
     */
    init {
        loadSessionData()
        viewModelScope.launch {
            applyFilters()
        }
    }

    /**
     * Applies filtering logic to the session requests based on current UI state.
     * Filters can be applied based on:
     * - Request body emptiness
     * - Selected HTTP methods
     *
     * The filtered results are stored in filteredRequests while preserving the original requests.
     *
     * Note: This should be called within a coroutine scope.
     */
    private suspend fun applyFilters() {
        _sessionUiData
            .map { sessionUiData ->
                sessionUiData.unfilteredRequests.filter { request ->
                    // Filter by body emptiness
                    val passesBodyFilter = if (sessionUiData.isBodyEmptyChecked) {
                        !request.body.isNullOrEmpty()
                    } else {
                        true
                    }

                    // Filter by selected methods
                    val passesMethodFilter = if (sessionUiData.selectedMethods.isEmpty()) {
                        true
                    } else {
                        sessionUiData.selectedMethods.any { methodMap ->
                            methodMap.entries.firstOrNull()?.let { (method, isEnabled) ->
                                isEnabled && request.method == method
                            } ?: false
                        }
                    }

                    passesBodyFilter && passesMethodFilter
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            .collect { filteredRequests ->
                _sessionUiData.update {
                    it.copy(
                        sessionData = it.sessionData?.copy(
                            requests = filteredRequests
                        )
                    )
                }
            }
    }

    /**
     * It starts loading the session data
     * from the repository and sets up the initial state of the uploadState and sessionData StateFlows.
     */
    private fun loadSessionData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (clickedSessionId == null) {
                    _sessionState.value = SessionState.ErrorLoading(
                        getApplication<Application>().getString(
                            R.string.error_loading_session
                        )
                    )
                    return@launch
                }
                // Load the session data from the repository using the clickedSessionId
                repository.getCompleteSessionData(clickedSessionId)
                    .collect { sessionData ->
                        //extract the methods available for this session to show them in the filter sheet
                        val availableMethods =
                            sessionData.requests?.map { it.method }?.toSet()?.toList()
                                ?: emptyList()

                        // Set the session data to the StateFlow
                        _sessionUiData.update {
                            it.copy(
                                sessionData = sessionData,
                                unfilteredRequests = sessionData.requests ?: emptyList(),
                                selectedMethods = availableMethods.map { method ->
                                    mapOf(method to true)
                                }
                            )
                        }
                        // Set the initial state of the uploadState StateFlow
                        // based on whether the session is already uploaded or not
                        if (sessionData.session.isUploadedToRemoteServer) {
                            _sessionState.value = SessionState.AlreadyUploaded
                        } else {
                            _sessionState.value = SessionState.NeverUploaded
                        }
                    }
            } catch (e: Exception) {
                // If an error occurs, set the uploadState to an Error State
                _sessionState.value =
                    SessionState.ErrorLoading(
                        getApplication<Application>().getString(
                            R.string.error_loading_session
                        )
                    )
            }
        }
    }

    /**
     * This function toggles the privacy or tos related state of a given [ScreenshotEntity]
     * in the repository to interact with the user clicking the image.
     *
     * @param screenshot the [ScreenshotEntity] to be updated
     */
    fun toggleScreenshotPrivacyOrToSrelated(screenshot: ScreenshotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScreenshot(
                screenshot.copy(isPrivacyOrTosRelated = !screenshot.isPrivacyOrTosRelated)
            )
        }
    }


    /**
     * This function uploads the session data to the remote server.
     * It takes a [SessionData] and a function to show a toast as parameters.
     * It updates the [_sessionState] with the result of the upload.
     *
     * @param showToast a function used to display a toast message if an error occurs during upload
     */
    fun uploadSession(
        sessionData: SessionData?,
        showToast: (message: String, style: ToastStyle) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            if (sessionData == null) {
                _sessionState.value = SessionState.ErrorUploading(
                    getApplication<Application>().getString(
                        R.string.error_uploading_session
                    )
                )
                return@launch
            }

            _sessionState.value = SessionState.Uploading

            repository.uploadSessionData(sessionData.session.networkId)
                .onSuccess {
                    try {
                        repository.updateSession(
                            sessionData.session.copy(
                                isUploadedToRemoteServer = true
                            )
                        )
                        _sessionState.value = SessionState.Success
                    } catch (e: Exception) {
                        _sessionState.value = SessionState.ErrorUploading(
                            e.localizedMessage
                                ?: getApplication<Application>().getString(R.string.error_uploading_session)
                        )
                        repository.updateSession(
                            sessionData.session.copy(
                                isUploadedToRemoteServer = false
                            )
                        )
                        return@launch
                    }
                }
                .onFailure { apiResponse ->
                    _sessionState.value = SessionState.ErrorUploading(
                        apiResponse.localizedMessage
                            ?: getApplication<Application>().getString(R.string.error_uploading_session)
                    )
                    withContext(Dispatchers.Main) {
                        repository.updateSession(
                            sessionData.session.copy(
                                isUploadedToRemoteServer = false
                            )
                        )
                        apiResponse.message?.let { showToast(it, ToastStyle.ERROR) }
                    }
                }
        }
    }

    /**
     * Toggles the visibility of the bottom sheet for filtering options.
     */
    fun toggleShowBottomSheet() {
        _sessionUiData.update {
            it.copy(showFilteringBottomSheet = !it.showFilteringBottomSheet)
        }
    }

    /**
     * Toggles the state of the body empty check.
     */
    fun toggleIsBodyEmpty() {
        _sessionUiData.update {
            it.copy(isBodyEmptyChecked = !it.isBodyEmptyChecked)
        }
    }

    /**
     * Modifies the selected methods by toggling the boolean value for the given [requestMethod].
     *
     * @param requestMethod the [RequestMethod] to toggle in the list of selected methods.
     */

    fun modifySelectedMethods(requestMethod: RequestMethod) {
        _sessionUiData.update {
            val updatedMethods = it.selectedMethods.map { methodMap ->
                methodMap.mapValues { (key, value) ->
                    if (key == requestMethod) !value else value
                }
            }
            it.copy(selectedMethods = updatedMethods)
        }
    }

    /**
     * Resets the filters to their default state.
     */
    fun resetFilters() {
        _sessionUiData.update { sessionUiData ->
            sessionUiData.copy(
                isBodyEmptyChecked = false,
                selectedMethods = sessionUiData.selectedMethods.map { methodMap ->
                    // Reset all methods to true to show all request methods of the session
                    methodMap.mapValues { (_, value) -> true }
                }
            )
        }
    }

}

/**
 * A factory for creating [SessionViewModel] instances.
 *
 * @param application the current application.
 * @param repository the [NetworkSessionRepository] to use.
 * @param clickedSessionId the id of the clicked session.
 */
class SessionViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
    private val clickedSessionId: String?,
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
