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
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.JobStatusResponse
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.PcapProcessingState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds


data class AutomaticAnalysisUiState(
    val isLoading: Boolean = false, // Tracks overall AI analysis loading
    val error: String? = null,
    val outputText: String? = null,
    val inputText: String = "You are a privacy and network security analyst tasked with evaluating a captive portal based on collected interaction data. Analyze the provided information, which may include:\n" +
            "\n" +
            "1.  **Network Requests:** HTTP/HTTPS requests made during the session (URLs, methods, potentially headers/body snippets).\n" +
            "2.  **Screenshots:** Images captured, potentially showing login pages, consent forms, or links to legal policies.\n" +
            "3.  **Webpage Content:** Snippets or summaries of HTML/JS content from relevant pages.\n" +
            "4.  **PCAP Network Summary (JSON):** A summary of network traffic, potentially showing protocols, IPs, and data flows (if provided and converted).\n" +
            "\n" +
            "Based on **all** the available data, please provide a structured report addressing the following:\n" +
            "\n" +
            "*   **Data Collection & Transmission:**\n" +
            "    *   Identify specific types of user data being collected (e.g., MAC address, email, name, device info, location hints, credentials).\n" +
            "    *   Analyze the destinations of network requests. Distinguish between requests likely essential for portal function versus requests to third-party domains (e.g., analytics, advertising, trackers).\n" +
            "    *   Note how data is transmitted (e.g., URL parameters, POST bodies).\n" +
            "*   **Privacy Policy & Consent:**\n" +
            "    *   Assess the visibility and accessibility of any Privacy Policy or Terms of Service based on screenshots and webpage content.\n" +
            "    *   If policy content is available or linked, briefly summarize any relevant points regarding data use, sharing, or retention (if possible from the provided data).\n" +
            "    *   Evaluate the presence and clarity of user consent mechanisms for data collection or terms acceptance.\n" +
            "*   **Security Considerations:**\n" +
            "    *   Based on network requests and the PCAP summary (if available), assess if sensitive data (like credentials) appears to be transmitted securely (e.g., over HTTPS).\n" +
            "    *   Highlight any obvious insecure practices observed (e.g., sensitive info in GET requests).\n" +
            "*   **Summary & Concerns:**\n" +
            "    *   Provide a concise summary highlighting the key privacy implications and potential security risks for a user connecting to this captive portal.\n" +
            "    *   Specifically mention any excessive data collection or concerning third-party communications identified.\n" +
            "\n" +
            "Structure your report clearly. Focus on actionable insights regarding user data privacy and security based *only* on the provided evidence.",
    val clickedSessionId: String? = null,
    val sessionData: SessionData? = null,

    val selectedRequestIds: Set<Int> = emptySet(),
    val selectedScreenshotIds: Set<Int> = emptySet(),
    val selectedWebpageContentIds: Set<Int> = emptySet(),

    val totalRequestsCount: Int = 0,
    val totalScreenshotsCount: Int = 0,
    val totalWebpageContentCount: Int = 0,
    val selectedModelName: String? = null,

    // --- PCAP Related State ---
    val isPcapIncludable: Boolean = false, // True if a PCAP file exists for this session
    val pcapFilePath: String? = null,      // Path to the local PCAP file
    val isPcapSelected: Boolean = false,   // User wants to include PCAP data
    val pcapProcessingState: PcapProcessingState = PcapProcessingState.Idle, // Tracks conversion status
    val pcapJsonContent: String? = null    // Stores the converted JSON (set on Success)
    // --- End PCAP Related State ---
) {
    // Helper property to determine if analysis can start
    val canStartAnalysis: Boolean
        get() {
            // Analysis cannot start if it's already loading OR if PCAP is selected but still converting
            return !isLoading && !(isPcapSelected && pcapProcessingState !is PcapProcessingState.Idle &&
                    pcapProcessingState !is PcapProcessingState.Success &&
                    pcapProcessingState !is PcapProcessingState.Error)
        }

    // Helper to check if PCAP conversion is actively running
    val isPcapConverting: Boolean
        get() = pcapProcessingState is PcapProcessingState.Uploading ||
                pcapProcessingState is PcapProcessingState.Processing ||
                pcapProcessingState is PcapProcessingState.Polling ||
                pcapProcessingState is PcapProcessingState.DownloadingJson
}

// --- Constants for PCAP Server ---
private const val SERVER_UPLOAD_URL = "https://13.61.79.152:3100/upload"
private const val SERVER_STATUS_URL_BASE = "https://13.61.79.152:3100/status/"
private const val PCAP_POLL_INITIAL_DELAY_MS = 3000L
private const val PCAP_POLL_INTERVAL_SECONDS = 5L
private const val PCAP_POLL_MAX_ATTEMPTS = 30
private const val PCAP_JSON_MAX_LENGTH = 100000 //  limit for including in prompt

interface IAutomaticAnalysisViewModel {
    val automaticAnalysisUiState: StateFlow<AutomaticAnalysisUiState>
    fun analyzeWithAi(modelName: String? = null)
    fun updatePromptEditText(text: String)
    fun loadSessionData()
    fun toggleRequestSelection(requestId: Int)
    fun toggleScreenshotSelection(screenshotId: Int)
    fun toggleWebpageContentSelection(contentId: Int)
    fun setAllRequestsSelected(select: Boolean)
    fun setAllScreenshotsSelected(select: Boolean)
    fun setAllWebpageContentSelected(select: Boolean)
    fun setClickedSessionId(clickedSessionId: String?)
    fun togglePcapSelection(isSelected: Boolean)
    fun retryPcapConversion()
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
    application: Application,
    private val repository: NetworkSessionRepository,
    clickedSessionId: String?,
) : AndroidViewModel(application), IAutomaticAnalysisViewModel {


    private val context: Context
        get() = getApplication<Application>().applicationContext

    //state related to workoutExerciseListScreen
    private val _automaticAnalysisUiState = MutableStateFlow(AutomaticAnalysisUiState())
    override val automaticAnalysisUiState = _automaticAnalysisUiState.asStateFlow()

    // --- OkHttp Client for PCAP ---
    private val pcapHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        // Add logging for debugging network calls
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // --- JSON Parser ---
    private val jsonParser = Json { ignoreUnknownKeys = true }

    // --- PCAP Conversion Job Management ---
    private var pcapConversionJob: Job? = null
    private var pcapPollingJob: Job? = null

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
        val allIds =
            _automaticAnalysisUiState.value.sessionData?.requests?.map { it.customWebViewRequestId }
                ?.toSet() ?: emptySet()
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedRequestIds = if (select) allIds else emptySet()
        )
    }

    override fun setAllScreenshotsSelected(select: Boolean) {
        val relevantScreenshots = _automaticAnalysisUiState.value.sessionData?.screenshots
            ?.filter { it.isPrivacyOrTosRelated } ?: emptyList()
        val allIds = relevantScreenshots.mapNotNull { it.screenshotId }.toSet()
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedScreenshotIds = if (select) allIds else emptySet()
        )
    }

    override fun setAllWebpageContentSelected(select: Boolean) {
        val allIds = _automaticAnalysisUiState.value.sessionData?.webpageContent
            ?.mapNotNull { it.contentId }?.toSet() ?: emptySet() // Assuming contentId is Int
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            selectedWebpageContentIds = if (select) allIds else emptySet()
        )
    }

    /**
     * Analyzes the selected session data using AI.
     * @param modelName The name of the Gemini model to use (e.g., "gemini-2.0-flash").
     */
    override fun analyzeWithAi(modelName: String?) {
        val currentState = _automaticAnalysisUiState.value
        // Use provided model name, fallback to last used, error if none available
        val effectiveModelName = modelName ?: currentState.selectedModelName
        if (effectiveModelName == null) {
            Log.e(
                "AnalyzeAI",
                "Analysis skipped: No AI model name provided or previously selected."
            )
            // Update state with error only if not already loading/converting
            if (!currentState.isLoading && !currentState.isPcapConverting) {
                _automaticAnalysisUiState.value = currentState.copy(error = "No AI model selected.")
            }
            return
        }

        if (currentState.sessionData == null) {
            Log.e("AnalyzeAI", "Analysis skipped: Session data is null.")
            if (!currentState.isLoading && !currentState.isPcapConverting) {
                _automaticAnalysisUiState.value =
                    currentState.copy(error = context.getString(R.string.error_session_data_not_loaded))
            }
            return
        }

        // Prevent starting analysis if already loading or PCAP is converting
        if (currentState.isLoading || currentState.isPcapConverting) {
            Log.w(
                "AnalyzeAI",
                "Analysis skipped: Already loading (isLoading=${currentState.isLoading}) or PCAP converting (isPcapConverting=${currentState.isPcapConverting})."
            )
            return
        }

        _automaticAnalysisUiState.value = currentState.copy(
            isLoading = true, // Set main loading flag for AI call
            error = null,
            outputText = null,
            selectedModelName = effectiveModelName // Store the model being used
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var pcapJsonForPrompt: String? = null
                var pcapErrorForPrompt: String? = null

                // --- Handle PCAP Conversion if Selected ---
                if (currentState.isPcapSelected && currentState.isPcapIncludable && currentState.pcapFilePath != null) {
                    // Check the current state of PCAP processing
                    when (val pcapState =
                        _automaticAnalysisUiState.value.pcapProcessingState) { // Re-read state in case it changed
                        is PcapProcessingState.Success -> {
                            Log.i(
                                "AnalyzeAI",
                                "PCAP already converted successfully. Using cached JSON."
                            )
                            pcapJsonForPrompt =
                                pcapState.jsonContent // Use existing successful result
                        }

                        is PcapProcessingState.Idle, is PcapProcessingState.Error -> {
                            // --- Trigger Conversion ---
                            Log.i("AnalyzeAI", "PCAP selected, starting conversion process...")
                            try {
                                // Call the suspend function that handles upload, polling, download
                                val convertedJson =
                                    convertPcapToJsonAndWait(currentState.pcapFilePath)
                                pcapJsonForPrompt = convertedJson // Store the successful result
                                Log.i("AnalyzeAI", "PCAP conversion successful.")
                                // State should be updated to Success inside convertPcapToJsonAndWait
                            } catch (pcapException: Exception) {
                                if (pcapException is CancellationException) {
                                    Log.i("AnalyzeAI", "PCAP conversion was cancelled.")
                                    // Do not proceed with AI analysis if PCAP was essential and cancelled
                                    withContext(Dispatchers.Main + NonCancellable) {
                                        _automaticAnalysisUiState.value =
                                            _automaticAnalysisUiState.value.copy(isLoading = false) // Clear AI loading flag
                                    }
                                    throw pcapException // Re-throw to stop the outer launch block
                                }
                                Log.e(
                                    "AnalyzeAI",
                                    "PCAP conversion failed during analysis trigger.",
                                    pcapException
                                )
                                pcapErrorForPrompt =
                                    "PCAP conversion failed: ${pcapException.message?.take(100)}" // Store error for prompt
                                // State should be updated to Error inside convertPcapToJsonAndWait's catch block
                            }
                        }
                        // If PCAP is already converting (Uploading, Processing, Polling, DownloadingJson)
                        // this function shouldn't have been called due to the initial check.
                        // Handle defensively just in case.
                        else -> {
                            Log.w(
                                "AnalyzeAI",
                                "PCAP is selected but is unexpectedly in state: $pcapState. Skipping PCAP inclusion."
                            )
                            pcapErrorForPrompt =
                                "PCAP data skipped (unexpected state during analysis start)."
                        }
                    }
                }
                // --- End PCAP Handling ---

                // --- Proceed with building prompt and calling AI ---

                // Re-read state in case PCAP conversion updated it (e.g., set an error)
                val currentStateAfterPcap = _automaticAnalysisUiState.value

                // Filter data based on selection (use currentStateAfterPcap for consistency)
                val selectedRequests = currentStateAfterPcap.sessionData?.requests?.filter {
                    currentStateAfterPcap.selectedRequestIds.contains(it.customWebViewRequestId)
                } ?: emptyList()

                val selectedScreenshots = currentStateAfterPcap.sessionData?.screenshots?.filter {
                    it.isPrivacyOrTosRelated && currentStateAfterPcap.selectedScreenshotIds.contains(
                        it.screenshotId
                    )
                } ?: emptyList()

                val selectedWebpageContent =
                    currentStateAfterPcap.sessionData?.webpageContent?.filter {
                        currentStateAfterPcap.selectedWebpageContentIds.contains(it.contentId)
                    } ?: emptyList()

                // Build Prompt Dynamically
                val basePrompt = currentStateAfterPcap.inputText
                var dynamicPromptPart =
                    "\n\n--- Analysis Context ---\nModel Used: '$effectiveModelName'"

                // Requests
                if (selectedRequests.isNotEmpty()) {
                    val requestsDTOString = buildRequestsString(selectedRequests)
                    dynamicPromptPart += "\n\n- Network Requests (${selectedRequests.size} selected, truncated if necessary):\n$requestsDTOString"
                } else {
                    dynamicPromptPart += "\n\n- Network Requests: None selected."
                }

                // Screenshots
                if (selectedScreenshots.isNotEmpty()) {
                    dynamicPromptPart += "\n\n- Relevant Screenshots (${selectedScreenshots.size} selected): Included as images."
                } else {
                    dynamicPromptPart += "\n\n- Relevant Screenshots: None selected."
                }

                // Webpage Content
                if (selectedWebpageContent.isNotEmpty()) {
                    // Consider adding snippets or summary if feasible
                    dynamicPromptPart += "\n\n- Webpage Content (${selectedWebpageContent.size} selected): Content provided separately (if applicable)."
                } else {
                    dynamicPromptPart += "\n\n- Webpage Content: None selected."
                }

                // --- Include PCAP JSON or Error in Prompt ---
                when {
                    pcapJsonForPrompt != null -> {
                        val truncatedJson = pcapJsonForPrompt.take(PCAP_JSON_MAX_LENGTH)
                        dynamicPromptPart += "\n\n- PCAP Network Summary (JSON, truncated if necessary):\n$truncatedJson"
                        if (truncatedJson.length < pcapJsonForPrompt.length) {
                            dynamicPromptPart += "\n... (PCAP JSON truncated due to length limit)"
                        }
                    }

                    pcapErrorForPrompt != null -> {
                        dynamicPromptPart += "\n\n- PCAP Network Summary: Not included ($pcapErrorForPrompt)"
                    }
                    // Explicitly check if PCAP was selected but not included for other reasons
                    currentStateAfterPcap.isPcapSelected && pcapJsonForPrompt == null && pcapErrorForPrompt == null -> {
                        if (!currentStateAfterPcap.isPcapIncludable || currentStateAfterPcap.pcapFilePath == null) {
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (File not available for this session)."
                        } else {
                            // Should not happen if logic is correct, but catch potential edge cases
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (Unknown reason)."
                            Log.w(
                                "AnalyzeAI",
                                "PCAP selected but neither JSON nor error available, and file path exists."
                            )
                        }
                    }

                    else -> { // PCAP was not selected by the user
                        dynamicPromptPart += "\n\n- PCAP Network Summary: Not selected."
                    }
                }
                dynamicPromptPart += "\n--- End Analysis Context ---"
                // --- End Include PCAP JSON ---

                val finalPrompt = "$basePrompt $dynamicPromptPart"
                Log.d(
                    "AnalyzeAI",
                    "Final prompt being sent to model $effectiveModelName:\n$finalPrompt"
                ) // Log prompt for debugging

                // Build Content for Gemini
                val inputContent = content {
                    // Add selected screenshots as images
                    if (selectedScreenshots.isNotEmpty()) {
                        selectedScreenshots.forEach { screenshot ->
                            try {
                                val bitmap = pathToBitmap(screenshot.path)
                                image(bitmap)
                                Log.d("AnalyzeAI", "Added image: ${screenshot.path}")
                            } catch (e: Exception) {
                                Log.e(
                                    "AnalyzeAI",
                                    "Failed to load or add image to prompt: ${screenshot.path}",
                                    e
                                )
                                // Optionally add text indicating image load failure to the prompt itself
                                text("\n[System Note: Failed to load image at ${screenshot.path}]")
                            }
                        }
                    }

                    //todo
                    // Add selected webpage content text here if desired and feasible within token limits
                    // Example: Concatenate text content (be very careful with limits)
                    // if (selectedWebpageContent.isNotEmpty()) {
                    //    val combinedHtml = selectedWebpageContent.mapNotNull { loadFileContent(it.htmlContentPath) }.joinToString("\n\n")
                    //    if (combinedHtml.isNotBlank()) {
                    //        text("\n\n--- Selected Webpage HTML Content (Snippets) ---\n${combinedHtml.take(10000)}") // Strict limit
                    //    }
                    // }

                    // Add the main text prompt
                    text(finalPrompt)
                }

                // --- Instantiate the model dynamically ---
                val config = generationConfig { temperature = 0.7f } // Example config
                val currentGenerativeModel = GenerativeModel(
                    modelName = effectiveModelName,
                    apiKey = BuildConfig.API_KEY_RELEASE,
                    generationConfig = config
                )
                Log.i("AnalyzeAI", "Calling Gemini model '$effectiveModelName'...")

                // --- Generate Content Stream ---
                var outputContent = ""
                currentGenerativeModel.generateContentStream(inputContent)
                    .collect { response ->
                        response.text?.let { textChunk ->
                            outputContent += textChunk
                            // Update output text incrementally on the Main thread while streaming
                            withContext(Dispatchers.Main) {
                                _automaticAnalysisUiState.value =
                                    _automaticAnalysisUiState.value.copy(
                                        outputText = outputContent // Update streamed output
                                    )
                            }
                        }
                        // Optional: Log prompt feedback or safety ratings if needed
                        Log.d("AnalyzeAI", "Prompt Feedback: ${response.promptFeedback}")
                        Log.d(
                            "AnalyzeAI",
                            "Finish Reason: ${response.candidates.firstOrNull()?.finishReason}"
                        )
                    }
                Log.i(
                    "AnalyzeAI",
                    "Gemini streaming finished successfully for model '$effectiveModelName'."
                )

                // --- Final State Update after successful AI call ---
                withContext(Dispatchers.Main) {
                    _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                        isLoading = false, // AI analysis part is done
                        outputText = outputContent // Ensure final complete text is set
                    )
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i("AnalyzeAI", "AI Analysis or preceding PCAP conversion was cancelled.")
                    // Reset loading state and potentially clear partial output only if cancelled here
                    withContext(Dispatchers.Main + NonCancellable) { // Ensure state reset happens
                        if (_automaticAnalysisUiState.value.isLoading) { // Check if still loading
                            _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                                isLoading = false,
                                outputText = null
                            ) // Clear partial output
                        }
                    }
                    throw e // Re-throw cancellation
                }
                // Catch errors from AI call or prompt building
                Log.e(
                    "AnalyzeAI",
                    "Error during AI analysis phase with model $effectiveModelName",
                    e
                )
                withContext(Dispatchers.Main + NonCancellable) { // Ensure state update happens
                    _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                        isLoading = false, // Stop loading indicator
                        error = "AI Analysis failed: ${e.localizedMessage ?: context.getString(R.string.unknown_error)}"
                    )
                }
            } finally {
                // Ensure loading is always reset, even if unexpected exceptions occur
                withContext(Dispatchers.Main + NonCancellable) {
                    if (_automaticAnalysisUiState.value.isLoading) {
                        _automaticAnalysisUiState.value =
                            _automaticAnalysisUiState.value.copy(isLoading = false)
                        Log.d("AnalyzeAI", "Ensured isLoading is false in finally block.")
                    }
                }
            }
        } // End viewModelScope.launch
    }


    // --- Toggle PCAP Selection ---
    override fun togglePcapSelection(isSelected: Boolean) {
        val currentState = _automaticAnalysisUiState.value
        // Only allow selection if PCAP is includable and not currently converting
        if (currentState.isPcapIncludable) { // Removed check for !isPcapConverting here - allow toggling off during conversion
            _automaticAnalysisUiState.value = currentState.copy(isPcapSelected = isSelected)
            // If selecting and state is Error, reset state to Idle to allow retry
            if (isSelected && currentState.pcapProcessingState is PcapProcessingState.Error) {
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    pcapProcessingState = PcapProcessingState.Idle,
                    pcapJsonContent = null // Clear old content/error
                )
            }
            // If toggling *off* during conversion, cancel the ongoing job
            if (!isSelected && currentState.isPcapConverting) {
                cancelPcapConversion("User deselected PCAP")
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    pcapProcessingState = PcapProcessingState.Idle // Reset state
                )
            }
        }
    }

    // ---  Retry PCAP Conversion ---
    override fun retryPcapConversion() {
        val currentState = _automaticAnalysisUiState.value
        if (currentState.isPcapIncludable && currentState.pcapProcessingState is PcapProcessingState.Error) {
            _automaticAnalysisUiState.value = currentState.copy(
                pcapProcessingState = PcapProcessingState.Idle,
                isPcapSelected = true // Assume user wants to retry because they clicked retry
            )
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
                // ... (handle error) ...
                return@launch
            }

            _automaticAnalysisUiState.value =
                _automaticAnalysisUiState.value.copy(isLoading = true, error = null)
            try {
                val sessionData = repository.getCompleteSessionData(currentSessionId).first()

                // --- Check for PCAP file ---
                val pcapPath = sessionData.session.pcapFilePath
                val isPcapPresent = pcapPath != null

                // Reset PCAP state when loading new session data
                cancelPcapConversion("New session data loaded")

                // Default selections
                val allRequestIds =
                    sessionData.requests?.mapNotNull { it.customWebViewRequestId }?.toSet()
                        ?: emptySet()
                val allScreenshotIds = sessionData.screenshots?.filter { it.isPrivacyOrTosRelated }
                    ?.mapNotNull { it.screenshotId }?.toSet() ?: emptySet()
                val allWebpageContentIds =
                    sessionData.webpageContent?.mapNotNull { it.contentId }?.toSet() ?: emptySet()

                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false,
                    sessionData = sessionData,
                    // Default selections
                    selectedRequestIds = allRequestIds,
                    selectedScreenshotIds = allScreenshotIds,
                    selectedWebpageContentIds = allWebpageContentIds,
                    // Counts
                    totalRequestsCount = sessionData.requests?.size ?: 0,
                    totalScreenshotsCount = sessionData.screenshots?.count { it.isPrivacyOrTosRelated }
                        ?: 0,
                    totalWebpageContentCount = sessionData.webpageContent?.size ?: 0,
                    // --- PCAP State Update ---
                    isPcapIncludable = isPcapPresent,
                    pcapFilePath = if (isPcapPresent) pcapPath else null,
                    isPcapSelected = false, // Default PCAP to not selected
                    pcapProcessingState = PcapProcessingState.Idle,
                    pcapJsonContent = null
                    // --- End PCAP State Update ---
                )
            } catch (e: Exception) {
                Log.e("AutomaticAnalysisViewModel", "Error during loadSessionData", e)
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false,
                    error = "Failed to load session data: ${e.message}"
                )
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

        val combined =
            postRequestsDTO + if (nonPostRequestsDTO.isNotEmpty() && remainingChars > 0) "\n$nonPostRequestsDTO" else ""

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
     * Updates the edit text content with the given text. The input text is limited to 3000 characters.
     *
     * @param text the new edit text content
     */
    override fun updatePromptEditText(text: String) {
        val newText = text.take(3000)
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            inputText = newText,
        )
    }

    override fun setClickedSessionId(clickedSessionId: String?) {
        _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
            clickedSessionId = clickedSessionId,
        )
    }

    // ---  PCAP Conversion Function (Suspendable) ---
    private suspend fun convertPcapToJsonAndWait(pcapFilePath: String): String =
        withContext(Dispatchers.IO) {
            // Ensure only one conversion runs at a time for this ViewModel instance
            pcapConversionJob?.cancelAndJoin() // Cancel previous and wait for it to finish

            val fileToUpload = File(pcapFilePath)
            if (!fileToUpload.exists() || fileToUpload.length() == 0L) {
                updatePcapState(PcapProcessingState.Error("PCAP file not found or empty at: $pcapFilePath"))
                throw IOException("PCAP file not found or empty.")
            }

            val deferredResult = CompletableDeferred<String>() // To wait for the final result

            pcapConversionJob = launch { // Launch conversion in a child job
                try {
                    updatePcapState(PcapProcessingState.Uploading(0f)) // Initial state

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "pcap_file",
                            fileToUpload.name,
                            fileToUpload.asRequestBody(
                                "application/vnd.tcpdump.pcap".toMediaTypeOrNull()
                                    ?: "application/octet-stream".toMediaTypeOrNull()
                            )
                        )
                        .build()

                    val request = Request.Builder().url(SERVER_UPLOAD_URL).post(requestBody).build()

                    Log.d("PCAP_CONVERT", "Uploading PCAP: ${fileToUpload.name}")
                    pcapHttpClient.newCall(request).execute().use { response ->
                        val responseBodyString = response.body?.string() // Read body once

                        if (!response.isSuccessful) {
                            Log.e(
                                "PCAP_CONVERT",
                                "Upload failed: ${response.code} ${response.message} - Body: ${
                                    responseBodyString?.take(500)
                                }"
                            )
                            throw IOException("Upload failed: ${response.code} - ${response.message}")
                        }

                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val result =
                                jsonParser.decodeFromString<Map<String, String>>(responseBody)
                            val jobId = result["job_id"]
                            Log.i("PCAP_CONVERT", "Upload successful, Job ID: $jobId")
                            // --- Start Polling ---
                            val finalJson =
                                pollAndDownloadResult(jobId!!) // This will suspend until completion or error
                            deferredResult.complete(finalJson) // Complete the deferred with the JSON
                        } else {
                            throw IOException("Server returned empty response body after upload.")
                        }
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.i("PCAP_CONVERT", "PCAP Conversion cancelled.")
                        updatePcapState(PcapProcessingState.Idle) // Reset state on cancellation
                        deferredResult.cancel(CancellationException("PCAP conversion cancelled", e))
                    } else {
                        Log.e("PCAP_CONVERT", "PCAP conversion failed", e)
                        updatePcapState(PcapProcessingState.Error("Conversion failed: ${e.message}"))
                        deferredResult.completeExceptionally(e) // Complete with exception
                    }
                }
            }

            // Wait for the deferred result (either JSON string or exception)
            return@withContext deferredResult.await()
        }

    // --- Combined Polling and Downloading (Suspendable) ---
    private suspend fun pollAndDownloadResult(jobId: String): String = withContext(Dispatchers.IO) {
        pcapPollingJob?.cancelAndJoin() // Cancel previous polling

        var attempt = 0
        val deferredResult = CompletableDeferred<String>()

        updatePcapState(PcapProcessingState.Processing(jobId))
        Log.i("PCAP_POLL", "Starting polling for job: $jobId")

        pcapPollingJob = launch pollLoop@{ // Child job for polling
            delay(PCAP_POLL_INITIAL_DELAY_MS) // Initial delay

            while (isActive && attempt < PCAP_POLL_MAX_ATTEMPTS) {
                attempt++
                Log.d("PCAP_POLL", "Polling attempt $attempt for job $jobId")
                updatePcapState(PcapProcessingState.Polling(jobId, attempt))

                try {
                    val statusUrl = "$SERVER_STATUS_URL_BASE$jobId"
                    val request = Request.Builder().url(statusUrl).get().build()

                    pcapHttpClient.newCall(request).execute().use checkStatus@{ response ->
                        val responseBodyString = response.body?.string() // Read once

                        if (!response.isSuccessful) {
                            Log.w(
                                "PCAP_POLL",
                                "Polling $jobId: Status check failed code ${response.code}. Body: ${
                                    responseBodyString?.take(200)
                                }"
                            )
                            if (response.code == 404) throw IOException("Job ID '$jobId' not found on server.")
                            // For other errors, let's wait and retry
                            delay(PCAP_POLL_INTERVAL_SECONDS.seconds)
                            return@checkStatus // Continue to next loop iteration
                        }

                        if (responseBodyString != null) {
                            Log.d("PCAP_POLL", "Status Response $jobId: $responseBodyString")
                            val statusResponse =
                                jsonParser.decodeFromString<JobStatusResponse>(responseBodyString)

                            when (statusResponse.status) {
                                "completed" -> {
                                    statusResponse.url?.let { url ->
                                        Log.i(
                                            "PCAP_POLL",
                                            "Job $jobId completed. Downloading from $url"
                                        )
                                        updatePcapState(
                                            PcapProcessingState.DownloadingJson(
                                                jobId,
                                                url
                                            )
                                        )
                                        try {
                                            val jsonContent =
                                                downloadJsonResult(url) // Suspend download
                                            updatePcapState(
                                                PcapProcessingState.Success(
                                                    jobId,
                                                    jsonContent
                                                )
                                            )
                                            deferredResult.complete(jsonContent) // Success!
                                            this@pollLoop.cancel("Job finished") // Stop polling loop
                                        } catch (downloadError: Exception) {
                                            if (downloadError is CancellationException) throw downloadError
                                            Log.e(
                                                "PCAP_DOWNLOAD",
                                                "Failed to download JSON for $jobId",
                                                downloadError
                                            )
                                            updatePcapState(
                                                PcapProcessingState.Error(
                                                    "Download failed: ${downloadError.message}",
                                                    jobId
                                                )
                                            )
                                            deferredResult.completeExceptionally(downloadError) // Fail deferred
                                            this@pollLoop.cancel("Download error")
                                        }
                                    } ?: run {
                                        throw IOException("Job completed but result URL missing.")
                                    }
                                    return@checkStatus // Exit loop implicitly via cancellation
                                }

                                "failed" -> {
                                    val errorMsg =
                                        statusResponse.error ?: "Unknown server processing error"
                                    Log.e("PCAP_POLL", "Job $jobId failed on server: $errorMsg")
                                    throw IOException("Processing failed: $errorMsg") // Throw to catch block
                                }

                                "processing" -> {
                                    // Continue polling, state already updated to Polling
                                    Log.d("PCAP_POLL", "Job $jobId still processing...")
                                }

                                else -> {
                                    Log.w(
                                        "PCAP_POLL",
                                        "Job $jobId: Unknown status: ${statusResponse.status}"
                                    )
                                    // Treat unknown as still processing for now
                                }
                            }
                        } else {
                            Log.w(
                                "PCAP_POLL",
                                "Polling $jobId: Empty status response body. Retrying..."
                            )
                        }
                    } // End response.use

                    // Only delay if the job is still processing or had a transient error
                    if (isActive) { // Check if loop wasn't cancelled by success/failure
                        delay(PCAP_POLL_INTERVAL_SECONDS.seconds)
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.i("PCAP_POLL", "Polling for $jobId cancelled.")
                        // Don't update state here, let the cancellation reason dictate
                        deferredResult.cancel(CancellationException("Polling cancelled", e))
                        throw e // Re-throw
                    }
                    Log.e("PCAP_POLL", "Error during status check for $jobId", e)
                    updatePcapState(PcapProcessingState.Error("Polling error: ${e.message}", jobId))
                    deferredResult.completeExceptionally(e) // Fail deferred
                    this@pollLoop.cancel("Polling error") // Stop polling loop
                    return@pollLoop // Exit launch block
                }
            } // End while loop

            // Handle timeout if loop finished due to maxAttempts
            if (isActive && attempt >= PCAP_POLL_MAX_ATTEMPTS) {
                Log.w("PCAP_POLL", "Polling $jobId timed out ")
                val timeoutError = IOException("Processing timed out on server.")
                updatePcapState(PcapProcessingState.Error("Processing timed out.", jobId))
                deferredResult.completeExceptionally(timeoutError)
            }
        } // End polling launch

        // Wait for polling/downloading to complete or fail
        return@withContext deferredResult.await()
    }

    // --- Download JSON Function (Suspendable, returns String) ---
    private suspend fun downloadJsonResult(url: String): String = withContext(Dispatchers.IO) {
        Log.d("PCAP_DOWNLOAD", "Downloading JSON from: $url")
        val request = Request.Builder().url(url).get().build()
        pcapHttpClient.newCall(request).execute().use { response ->
            val responseBodyString = response.body?.string() // Read once
            if (!response.isSuccessful) {
                Log.e(
                    "PCAP_DOWNLOAD",
                    "Download failed: ${response.code} ${response.message}. Body: ${
                        responseBodyString?.take(200)
                    }"
                )
                throw IOException("Download failed: ${response.code} ${response.message}")
            }
            responseBodyString ?: throw IOException("Downloaded JSON content was null.")
        }
    }

    // ---  Helper to update PCAP state on Main thread ---
    private suspend fun updatePcapState(newState: PcapProcessingState) {
        withContext(Dispatchers.Main) {
            // Store JSON content separately when state is Success
            val jsonContent =
                if (newState is PcapProcessingState.Success) newState.jsonContent else _automaticAnalysisUiState.value.pcapJsonContent
            _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                pcapProcessingState = newState,
                pcapJsonContent = jsonContent // Keep existing JSON unless overwritten by new Success
            )
        }
    }

    // ---  Helper to cancel ongoing PCAP conversion/polling ---
    private fun cancelPcapConversion(reason: String) {
        if (pcapPollingJob?.isActive == true) {
            Log.i("PCAP_CANCEL", "Cancelling PCAP polling: $reason")
            pcapPollingJob?.cancel(reason)
        }
        if (pcapConversionJob?.isActive == true) {
            Log.i("PCAP_CANCEL", "Cancelling PCAP conversion job: $reason")
            pcapConversionJob?.cancel(reason)
        }
        pcapPollingJob = null
        pcapConversionJob = null
    }

    override fun onCleared() {
        pcapConversionJob?.cancel("ViewModel cleared")
        pcapPollingJob?.cancel("ViewModel cleared")
        super.onCleared()
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
            // Removed GenerativeModel creation from here
            @Suppress("UNCHECKED_CAST")
            return AutomaticAnalysisViewModel(
                application,
                repository,
                clickedSessionId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}