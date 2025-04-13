package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis


import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.BuildConfig
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.REQUESTS_LIMIT_MAX
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


data class AutomaticAnalysisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val outputText: String? = null,
    val inputText: String = "Look at the following prompt and image(s), and then give us a report about the data the " +
            "captive portal collects about the user and any information regarding the data privacy " +
            "and ToS of the captive portal if found " +
            "and how you rate them:",
    val clickedSessionId: String? = null,
    val sessionData: SessionData? = null,

    val selectedRequestIds: Set<Int> = emptySet(),
    val selectedScreenshotIds: Set<Int> = emptySet(),
    val selectedWebpageContentIds: Set<Int> = emptySet(),

    val totalRequestsCount: Int = 0,
    val totalScreenshotsCount: Int = 0, // Count of *potentially* includable screenshots
    val totalWebpageContentCount: Int = 0,
)


interface IAutomaticAnalysisViewModel {
    val automaticAnalysisUiState: StateFlow<AutomaticAnalysisUiState>
    fun analyzeWithAi()
    fun updatePromptEditText(text: String)
    fun loadSessionData()
    fun toggleRequestSelection(requestId: Int)
    fun toggleScreenshotSelection(screenshotId: Int)
    fun toggleWebpageContentSelection(contentId: Int)
    fun setAllRequestsSelected(select: Boolean)
    fun setAllScreenshotsSelected(select: Boolean)
    fun setAllWebpageContentSelected(select: Boolean)
    fun setClickedSessionId(clickedSessionId: String?)
}

/**
 * A [ViewModel] that generates text based on a given [SessionData] using a generative AI model.
 *
 * @param generativeModel the generative AI model to use for generating text
 * @param application the application context
 * @param repository the repository of network sessions
 * @param clickedSessionId the id of the session that was clicked
 */
open class AutomaticAnalysisViewModel(
    private val generativeModel: GenerativeModel,
    application: Application,
    private val repository: NetworkSessionRepository,
    clickedSessionId: String?,
) : AndroidViewModel(application), IAutomaticAnalysisViewModel {


    private val context: Context
        get() = getApplication<Application>().applicationContext

    //state related to workoutExerciseListScreen
    private val _automaticAnalysisUiState = MutableStateFlow(AutomaticAnalysisUiState())
    override val automaticAnalysisUiState = _automaticAnalysisUiState.asStateFlow()


    init {
        // Set the clicked session ID in the AutomaticAnalysisUiState
        setClickedSessionId(clickedSessionId)
        // Load session data using the clicked session ID
        loadSessionData()
    }



    override fun toggleRequestSelection(requestId: Int) {
        val currentSelection = _automaticAnalysisUiState.value.selectedRequestIds
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedRequestIds = if (currentSelection.contains(requestId)) {
                currentSelection - requestId
            } else {
                currentSelection + requestId
            }
        )
    }

    override fun toggleScreenshotSelection(screenshotId: Int) {
        val currentSelection = _automaticAnalysisUiState.value.selectedScreenshotIds
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedScreenshotIds = if (currentSelection.contains(screenshotId)) {
                currentSelection - screenshotId
            } else {
                currentSelection + screenshotId
            }
        )
    }

    override fun toggleWebpageContentSelection(contentId: Int) {
        val currentSelection = _automaticAnalysisUiState.value.selectedWebpageContentIds
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedWebpageContentIds = if (currentSelection.contains(contentId)) {
                currentSelection - contentId
            } else {
                currentSelection + contentId
            }
        )
    }

    override fun setAllRequestsSelected(select: Boolean) {
        val allIds = _automaticAnalysisUiState.value.sessionData?.requests?.map { it.customWebViewRequestId }?.toSet() ?: emptySet()
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedRequestIds = if (select) allIds else emptySet()
        )
    }

    override fun setAllScreenshotsSelected(select: Boolean) {
        // Use the actual ID field from your Screenshot Entity
        // Filter for privacy related here if you only want to allow selecting those
        val relevantScreenshots = _automaticAnalysisUiState.value.sessionData?.screenshots
            ?.filter { it.isPrivacyOrTosRelated } ?: emptyList()
        val allIds = relevantScreenshots.mapNotNull { it.screenshotId }.toSet() // Assuming screenshotId is Int
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedScreenshotIds = if (select) allIds else emptySet()
        )
    }

    override fun setAllWebpageContentSelected(select: Boolean) {
        // Use the actual ID field from your WebpageContent Entity
        val allIds = _automaticAnalysisUiState.value.sessionData?.webpageContent
            ?.mapNotNull { it.contentId }?.toSet() ?: emptySet() // Assuming contentId is Int
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedWebpageContentIds = if (select) allIds else emptySet()
        )
    }

    /**
     * Analyzes the selected session data using AI.
     */
    override fun analyzeWithAi() {
        val currentState = _automaticAnalysisUiState.value
        val sessionData = currentState.sessionData

        if (sessionData == null) {
            _automaticAnalysisUiState.value = currentState.copy(
                isLoading = false,
                error = context.getString(R.string.error_session_data_not_loaded) // Add this string resource
            )
            return
        }


        _automaticAnalysisUiState.value = currentState.copy(isLoading = true, error = null, outputText = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // --- Filter data based on selection ---
                val selectedRequests = sessionData.requests?.filter {
                    currentState.selectedRequestIds.contains(it.customWebViewRequestId) // Use the correct ID field
                } ?: emptyList()

                val selectedScreenshots = sessionData.screenshots?.filter {
                    // Filter for privacy *AND* user selection
                    it.isPrivacyOrTosRelated && currentState.selectedScreenshotIds.contains(it.screenshotId)
                    // OR: If you allow selecting non-privacy related ones too:
                    // currentState.selectedScreenshotIds.contains(it.screenshotId)
                } ?: emptyList()

                val selectedWebpageContent = sessionData.webpageContent?.filter {
                    currentState.selectedWebpageContentIds.contains(it.contentId) // Use the correct ID field
                } ?: emptyList()
                // ------------------------------------

                // --- Build Prompt Dynamically ---
                val basePrompt = currentState.inputText
                var dynamicPromptPart = "\nAnalyzing the selected information:"

                if (selectedRequests.isNotEmpty()) {
                    val requestsDTOString = buildRequestsString(selectedRequests) // Pass filtered list
                    dynamicPromptPart += "\n\n- Network Requests (${selectedRequests.size} selected):\n$requestsDTOString"
                } else {
                    dynamicPromptPart += "\n\n- Network Requests: None selected."
                }

                if (selectedScreenshots.isNotEmpty()) {
                    dynamicPromptPart += "\n\n- Relevant Screenshots (${selectedScreenshots.size} selected): Included as images."
                } else {
                    dynamicPromptPart += "\n\n- Relevant Screenshots: None selected."
                }

                if (selectedWebpageContent.isNotEmpty()) {
                    // Still likely best to just mention count unless content is very small
                    dynamicPromptPart += "\n\n- Webpage Content (${selectedWebpageContent.size} selected): Included."
                } else {
                    dynamicPromptPart += "\n\n- Webpage Content: None selected."
                }

                val finalPrompt = "$basePrompt $dynamicPromptPart"
                // ----------------------------------

                // --- Build Content for Gemini ---
                val inputContent = content {
                    if (selectedScreenshots.isNotEmpty()) {
                        selectedScreenshots.forEach { screenshot ->
                            try {
                                val bitmap = pathToBitmap(screenshot.path)
                                image(bitmap)
                            } catch (e: Exception) {
                                Log.e("AnalyzeAI", "Failed to load or add image: ${screenshot.path}", e)
                            }
                        }
                    }
                    // Add selected webpage content text here if desired and feasible within limits
                    // For now, we only mention it in the text prompt.

                    text(finalPrompt)
                }
                // --- Generate Content ---
                var outputContent = ""
                generativeModel.generateContentStream(inputContent)
                    .collect { response ->
                        outputContent += response.text
                        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                            isLoading = true, // Still loading during stream
                            outputText = outputContent,
                        )
                    }
                // --- Final State Update ---
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false,
                    error = (e.localizedMessage ?: "An unknown error occurred")
                )
                Log.e("AnalyzeAI", "Error during AI analysis", e)
            }
        }
    }

    /**
     * Loads the session data for the given [clickedSessionId] from the database.
     *
     */
    override fun loadSessionData() {
        viewModelScope.launch(Dispatchers.IO) {

            val currentSessionId = _automaticAnalysisUiState.value.clickedSessionId
            if (currentSessionId == null) {
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false,
                    error = context.getString(R.string.error_session_id_missing) // Add string
                )
                return@launch
            }

            _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(isLoading = true, error = null)
            try {
                val sessionData = repository.getCompleteSessionData(_automaticAnalysisUiState.value.clickedSessionId!!).first()

                // Default to all items selected on load
                val allRequestIds = sessionData.requests?.mapNotNull { it.customWebViewRequestId }?.toSet() ?: emptySet() // Ensure IDs exist
                val allScreenshotIds = sessionData.screenshots?.filter { it.isPrivacyOrTosRelated }?.mapNotNull { it.screenshotId }?.toSet() ?: emptySet()
                // OR select all screenshots regardless of privacy flag initially:
                // val allScreenshotIds = sessionData.screenshots?.mapNotNull { it.screenshotId }?.toSet() ?: emptySet()
                val allWebpageContentIds = sessionData.webpageContent?.mapNotNull { it.contentId }?.toSet() ?: emptySet() // Ensure IDs exist

                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false,
                    sessionData = sessionData,
                    selectedRequestIds = allRequestIds,
                    selectedScreenshotIds = allScreenshotIds,
                    selectedWebpageContentIds = allWebpageContentIds,
                    // Also update total counts
                    totalRequestsCount = sessionData.requests?.size ?: 0,
                    totalScreenshotsCount = sessionData.screenshots?.count { it.isPrivacyOrTosRelated } ?: 0,
                    totalWebpageContentCount = sessionData.webpageContent?.size ?: 0
                )
            } catch (e: Exception) {
                Log.e("AutomaticAnalysisViewmodel", "Error during loadSessionData", e)
            }
        }
    }

    // Helper to build the requests string
    private fun buildRequestsString(requests: List<CustomWebViewRequestEntity>?): String {
        if (requests.isNullOrEmpty()) return "None"

        val postRequests = requests.filter { it.type == "POST" }
        val nonPostRequests = requests.filter { it.type != "POST" }

        val postRequestsDTO = postRequests.joinToString(separator = "\n") {
            "url:${it.url}, type:${it.type}, method:${it.method}, body:${it.body ?: "N/A"}, headers:${it.headers ?: "N/A"}" // Simplified format
        }

        val remainingChars = REQUESTS_LIMIT_MAX - postRequestsDTO.length

        val nonPostRequestsDTO = nonPostRequests.joinToString(separator = "\n") {
            "url:${it.url}, type:${it.type}, method:${it.method}, body:${it.body ?: "N/A"}, headers:${it.headers ?: "N/A"}" // Simplified format
        }.take(remainingChars.coerceAtLeast(0)) // Ensure non-negative take

        val combined = postRequestsDTO + if (nonPostRequestsDTO.isNotEmpty() && remainingChars > 0) "\n$nonPostRequestsDTO" else ""

        // Add truncation indicator if needed
        val totalLength = postRequestsDTO.length + nonPostRequestsDTO.length
        val originalTotalLength = requests.sumOf {
            ("url:${it.url}, type:${it.type}, method:${it.method}, body:${it.body ?: "N/A"}, headers:${it.headers ?: "N/A"}\n").length
        }

        return if (totalLength < originalTotalLength || totalLength >= REQUESTS_LIMIT_MAX) {
            "$combined\n... (Requests truncated due to length limit)"
        } else {
            combined
        }
    }


    /**
     * Updates the edit text content with the given text. The input text is limited to 300 characters.
     *
     * @param text the new edit text content
     */
    override fun updatePromptEditText(text: String) {
        val newText = text.take(300)
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            inputText = newText,
        )
    }

    override fun setClickedSessionId(clickedSessionId: String?) {
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            clickedSessionId = clickedSessionId,
        )
    }
}

/**
 * Decodes a file path into a Bitmap because the AI server expects a Bitmap.
 *
 * @param path the file path of the image
 * @return the decoded Bitmap
 */
private fun pathToBitmap(path: String): Bitmap {
    val bitmap = BitmapFactory.decodeFile(path)
    return bitmap
}


/**
 * A factory for creating [AutomaticAnalysisViewModel] instances.
 *
 * @param application the application context
 * @param repository the repository of network sessions
 */
class AutomaticAnalysisViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
    private val clickedSessionId: String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutomaticAnalysisViewModel::class.java)) {
            // set up the generative model with the current API key
            val config = generationConfig {
                temperature = 0.7f
            }
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash", // Specifies the name of the generative model to be used for analysis
                apiKey = BuildConfig.API_KEY_RELEASE,//apiKey to authenticate requests
                generationConfig = config // Applies the configuration settings for generation
            )
            @Suppress("UNCHECKED_CAST")
            return AutomaticAnalysisViewModel(
                generativeModel,
                application,
                repository,
                clickedSessionId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}