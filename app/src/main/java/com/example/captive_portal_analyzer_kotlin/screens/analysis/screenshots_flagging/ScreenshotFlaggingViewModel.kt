package com.example.captive_portal_analyzer_kotlin.screens.analysis.screenshots_flagging

import NetworkSessionRepository
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing screenshots and flagging them as privacy/ToS related
 * for a specific session during the analysis flow.
 */
class ScreenshotFlaggingViewModel(
    application: Application,
    private val repository: NetworkSessionRepository,
    private val sessionManager: NetworkSessionManager,
    private val showToast: (message: String, style: ToastStyle) -> Unit // Function to show toasts
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ScreenshotFlaggingVM"
    }

    // State flow to hold the screenshots for the current analysis session
    private val _screenshots = MutableStateFlow<List<ScreenshotEntity>>(emptyList())
    val screenshots: StateFlow<List<ScreenshotEntity>> get() = _screenshots.asStateFlow()

    // State flow to indicate loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private var currentSessionId: String? = null

    init {
        loadScreenshotsForCurrentSession()
    }

    /**
     * Loads screenshots associated with the current analysis session ID.
     * This is called when the ViewModel is created.
     */
    private fun loadScreenshotsForCurrentSession() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true // Set loading true at the start
            try {
                currentSessionId = sessionManager.getCurrentSessionId()
                if (currentSessionId != null) {
                    // Fetch all screenshots for this session ID
                    repository.getSessionScreenshots(currentSessionId!!)
                        // Use onEach to process emissions and manage loading state
                        .onEach { sessionScreenshots ->
                            // Update the state flow immediately
                            withContext(Dispatchers.Main) {
                                _screenshots.value = sessionScreenshots
                                // Set isLoading to false *after* the first successful emission
                                // Only set it false if it's currently true, to avoid setting it back to true
                                // on subsequent emissions.
                                if (_isLoading.value) {
                                    _isLoading.value = false
                                    Log.d(TAG, "Finished initial load & set isLoading=false for session $currentSessionId")
                                }
                            }
                            Log.d(TAG, "Collected ${sessionScreenshots.size} screenshots update for session $currentSessionId")
                        }
                        .collect{} // Terminal operator to start collection - no need to do anything in the block here
                    // Note: Code will not naturally reach here if the Flow never completes
                } else {
                    // Handle null session ID case
                    Log.e(TAG, "Current session ID is null, cannot load screenshots.")
                    withContext(Dispatchers.Main) {
                        showToast("Error: Cannot load screenshots for session.", ToastStyle.ERROR)
                    }
                    // If session ID is null, loading *is* finished (with an error/empty state)
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false // Set loading false even on error/null session
                    }
                }
            } catch (e: Exception) {
                // Handle general exception during loading
                Log.e(TAG, "Error loading screenshots: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error loading screenshots: ${e.localizedMessage}", ToastStyle.ERROR)
                    _isLoading.value = false // Set loading false on general exception
                }
            }
            // The 'finally' block is removed or no longer needed for setting isLoading=false
            // as we handle it within the try/catch or onEach.
            // This coroutine launched by viewModelScope.launch will only finish if the Flow completes
            // or the coroutine is cancelled.
        }
    }


    /**
     * Toggles the `isPrivacyOrTosRelated` flag for a given screenshot in the database
     * and updates the corresponding item in the `_screenshots` StateFlow.
     *
     * @param screenshot The [ScreenshotEntity] to update.
     */
    fun toggleScreenshotAnalysisFlag(screenshot: ScreenshotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedScreenshot = screenshot.copy(isPrivacyOrTosRelated = !screenshot.isPrivacyOrTosRelated)
            try {
                // Update in the database
                repository.updateScreenshot(updatedScreenshot)

                // Update in the ViewModel's state flow (on Main thread)
                withContext(Dispatchers.Main) {
                    _screenshots.update { currentList ->
                        currentList.map {
                            if (it.screenshotId == updatedScreenshot.screenshotId) {
                                updatedScreenshot
                            } else {
                                it
                            }
                        }
                    }
                    Log.d(TAG, "Toggled screenshot ${screenshot.screenshotId} to isPrivacyOrTosRelated = ${updatedScreenshot.isPrivacyOrTosRelated}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling screenshot flag in DB", e)
                withContext(Dispatchers.Main) {
                    showToast("Error updating screenshot state: ${e.localizedMessage}", ToastStyle.ERROR)
                }
            }
        }
    }

    /**
     * Called when the user finishes the flagging process.
     * This can trigger any final processing steps before navigating away.
     * For now, it just logs and prepares for navigation.
     */
    fun finishFlagging() {
        Log.i(TAG, "Screenshot flagging finished for session $currentSessionId")
        // Any final actions related to flagged screenshots can go here if needed later
        // e.g., summary, final save actions if different from toggling
    }

}

/**
 * A factory for creating instances of [ScreenshotFlaggingViewModel].
 * This is needed to pass dependencies to the ViewModel.
 */
class ScreenshotFlaggingViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
    private val sessionManager: NetworkSessionManager,
    private val showToast: (message: String, style: ToastStyle) -> Unit
) : ViewModelProvider.Factory {
    /**
     * Creates an instance of a ViewModel.
     *
     * @param modelClass The class of the ViewModel to create.
     * @return An instance of the ViewModel.
     * @throws IllegalArgumentException If the modelClass is not a subclass of [ScreenshotFlaggingViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScreenshotFlaggingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScreenshotFlaggingViewModel(
                application,
                repository,
                sessionManager,
                showToast
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}