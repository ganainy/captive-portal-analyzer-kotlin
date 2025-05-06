package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis


// import kotlinx.coroutines.cancel // Removed unused import
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
import kotlinx.coroutines.flow.update
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
import okio.Buffer
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
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
    val pcapJsonContent: String? = null,    // Stores the converted JSON (set on Success)
    // --- End PCAP Related State ---

    // ---  State for Prompt Preview ---
    val promptPreview: String? = null, // Holds the generated prompt for preview
    val isGeneratingPreview: Boolean = false, // to show loading indicator while generating preview

) {
    // Helper property to determine if analysis can start
    // Analysis cannot start if it's already loading OR if PCAP is selected but still converting
    val canStartAnalysis: Boolean
        get() {
            val isPcapReadyOrNotSelected = !isPcapSelected ||
                    pcapProcessingState is PcapProcessingState.Idle ||
                    pcapProcessingState is PcapProcessingState.Success ||
                    pcapProcessingState is PcapProcessingState.Error // Allow starting even if PCAP failed, it just won't be included

            return !isLoading && isPcapReadyOrNotSelected
        }

    // Helper to check if PCAP conversion is actively running
    val isPcapConverting: Boolean
        get() = pcapProcessingState is PcapProcessingState.Uploading ||
                pcapProcessingState is PcapProcessingState.Processing ||
                pcapProcessingState is PcapProcessingState.Polling ||
                pcapProcessingState is PcapProcessingState.DownloadingJson
}

// --- Constants for PCAP Server ---
private const val SERVER_UPLOAD_URL = "http://13.61.79.152:3100/upload"
private const val SERVER_STATUS_URL_BASE = "http://13.61.79.152:3100/status/"
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
    fun generatePromptForPreview()
}

/**
 * A [ViewModel] that generates text based on a given [SessionData] using a generative AI model.
 *
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
            level = HttpLoggingInterceptor.Level.HEADERS
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
     * @param modelName The name of the Gemini model to use (e.g., "gemini-2.0-flash"). Defaults to last used or state's selected model.
     */
    /**
     * Analyzes the selected session data using AI.
     * Includes handling for PCAP conversion status and potential truncation.
     * @param modelName The name of the Gemini model to use (e.g., "gemini-2.0-flash"). Defaults to last used or state's selected model.
     */
    override fun analyzeWithAi(modelName: String?) {
        val currentState = _automaticAnalysisUiState.value
        val effectiveModelName = modelName ?: currentState.selectedModelName
        if (effectiveModelName == null) {
            Log.e("AnalyzeAI", "Analysis skipped: No AI model name provided or previously selected.")
            if (!currentState.isLoading && !currentState.isPcapConverting) {
                _automaticAnalysisUiState.update { it.copy(error = "No AI model selected.") }
            }
            return
        }

        if (currentState.sessionData == null) {
            Log.e("AnalyzeAI", "Analysis skipped: Session data is null.")
            if (!currentState.isLoading && !currentState.isPcapConverting) {
                _automaticAnalysisUiState.update { it.copy(error = context.getString(R.string.error_session_data_not_loaded)) }
            }
            return
        }

        // Check if PCAP is selected and still converting (more specific check)
        if (currentState.isPcapSelected && currentState.isPcapConverting) {
            Log.w("AnalyzeAI", "Analysis start delayed: PCAP conversion is still in progress.")
            // UI should prevent this via canStartAnalysis, but log defensively.
            return
        }

        // Prevent starting AI analysis if already loading
        if (currentState.isLoading) {
            Log.w("AnalyzeAI", "Analysis skipped: AI analysis is already in progress (isLoading=true).")
            return
        }

        _automaticAnalysisUiState.update {
            it.copy(
                isLoading = true, // Set main loading flag for AI call
                error = null,
                outputText = null, // Clear previous output
                selectedModelName = effectiveModelName // Store the model being used
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var pcapErrorForPrompt: String? = null // Tracks errors specifically for the prompt message

            try {
                // --- Handle PCAP Data Acquisition ---
                // Re-read state *inside* the coroutine for latest status
                val stateForAnalysis = _automaticAnalysisUiState.value
                var pcapStateForAnalysis = stateForAnalysis.pcapProcessingState

                if (stateForAnalysis.isPcapSelected && stateForAnalysis.isPcapIncludable && stateForAnalysis.pcapFilePath != null) {
                    // Check the current state of PCAP processing just before analysis
                    when (pcapStateForAnalysis) {
                        is PcapProcessingState.Success -> {
                            Log.i("AnalyzeAI", "PCAP already converted successfully. Using cached JSON. Truncated: ${pcapStateForAnalysis.wasTruncated}")
                            // JSON content is handled later when building the prompt using the state.
                        }
                        is PcapProcessingState.Error -> {
                            Log.w("AnalyzeAI", "PCAP conversion previously failed. Proceeding without PCAP data. Error: ${pcapStateForAnalysis.message}")
                            pcapErrorForPrompt = "PCAP conversion failed: ${pcapStateForAnalysis.message.take(100)}"
                        }
                        is PcapProcessingState.Idle -> {
                            // PCAP was selected, but conversion wasn't triggered or completed yet. Trigger it now.
                            Log.i("AnalyzeAI", "PCAP selected but conversion not started/completed. Starting conversion now...")
                            try {
                                // Call the suspend function that handles upload, polling, download
                                // convertPcapToJsonAndWait will update the state internally
                                val convertedJson = convertPcapToJsonAndWait(stateForAnalysis.pcapFilePath)
                                Log.i("AnalyzeAI", "PCAP conversion successful during analysis trigger.")
                                // Re-read the state after successful conversion
                                pcapStateForAnalysis = _automaticAnalysisUiState.value.pcapProcessingState
                                if (pcapStateForAnalysis !is PcapProcessingState.Success) {
                                    Log.w("AnalyzeAI", "PCAP conversion reported success, but state is not Success ($pcapStateForAnalysis). Proceeding without PCAP.")
                                    pcapErrorForPrompt = "PCAP conversion finished with unexpected state."
                                }
                            } catch (pcapException: Exception) {
                                if (pcapException is CancellationException) {
                                    Log.i("AnalyzeAI", "PCAP conversion was cancelled during analysis trigger.")
                                    withContext(Dispatchers.Main + NonCancellable) {
                                        _automaticAnalysisUiState.update { it.copy(isLoading = false, error = "PCAP conversion cancelled.") }
                                    }
                                    throw pcapException // Stop the outer launch block
                                }
                                Log.e("AnalyzeAI", "PCAP conversion failed during analysis trigger.", pcapException)
                                pcapErrorForPrompt = "PCAP conversion failed: ${pcapException.message?.take(100)}"
                                // State should be updated to Error inside convertPcapToJsonAndWait's catch block
                                pcapStateForAnalysis = _automaticAnalysisUiState.value.pcapProcessingState // Update local state var
                            }
                        }
                        // If PCAP is currently converting (Uploading, Processing, etc.)
                        // This case should ideally be prevented by the initial checks. Handle defensively.
                        else -> { // is PcapProcessingState.Uploading, Processing, Polling, DownloadingJson
                            Log.e("AnalyzeAI", "PCAP is selected but unexpectedly still converting at analysis start time. State: $pcapStateForAnalysis. Proceeding without PCAP data.")
                            pcapErrorForPrompt = "PCAP data skipped (conversion in progress)."
                        }
                    }
                }
                // --- End PCAP Handling ---

                // --- Proceed with building prompt and calling AI ---
                // Re-read state again after potential PCAP conversion
                val currentStateAfterPcap = _automaticAnalysisUiState.value
                pcapStateForAnalysis = currentStateAfterPcap.pcapProcessingState // Ensure we use the latest state

                // Ensure sessionData isn't null after potential delays
                val sessionDataForPrompt = currentStateAfterPcap.sessionData ?: run {
                    Log.e("AnalyzeAI", "Session data became null during PCAP processing.")
                    throw IllegalStateException("Session data lost during analysis preparation.")
                }

                // Filter data based on selection
                val selectedRequests = sessionDataForPrompt.requests?.filter {
                    currentStateAfterPcap.selectedRequestIds.contains(it.customWebViewRequestId)
                } ?: emptyList()

                val selectedScreenshots = sessionDataForPrompt.screenshots?.filter {
                    it.isPrivacyOrTosRelated && currentStateAfterPcap.selectedScreenshotIds.contains(it.screenshotId)
                } ?: emptyList()

                val selectedWebpageContent = sessionDataForPrompt.webpageContent?.filter {
                    currentStateAfterPcap.selectedWebpageContentIds.contains(it.contentId)
                } ?: emptyList()

                // Build Prompt Dynamically
                val basePrompt = currentStateAfterPcap.inputText
                var dynamicPromptPart = "\n\n--- Analysis Context ---\nModel Used: '$effectiveModelName'"

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
                    dynamicPromptPart += "\n\n- Webpage Content (${selectedWebpageContent.size} selected): Content provided separately (if applicable)." // Adjust wording as needed
                } else {
                    dynamicPromptPart += "\n\n- Webpage Content: None selected."
                }

                // --- Include PCAP JSON or Error/Status in Prompt ---
                when {
                    // Case 1: Success state - check the truncation flag
                    pcapStateForAnalysis is PcapProcessingState.Success -> {
                        dynamicPromptPart += "\n\n- PCAP Network Summary (JSON):\n${pcapStateForAnalysis.jsonContent}"
                        // Append truncation note if the flag is true
                        if (pcapStateForAnalysis.wasTruncated) {
                            dynamicPromptPart += "\n... (PCAP JSON truncated due to length limit)"
                        }
                    }

                    // Case 2: Use error captured earlier OR check Error state directly
                    pcapErrorForPrompt != null -> {
                        dynamicPromptPart += "\n\n- PCAP Network Summary: Not included ($pcapErrorForPrompt)"
                    }
                    pcapStateForAnalysis is PcapProcessingState.Error -> {
                        // Should be covered by pcapErrorForPrompt, but double-check
                        if (pcapErrorForPrompt == null) { // If error happened before analysis started
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (PCAP conversion failed: ${pcapStateForAnalysis.message.take(100)})"
                        }
                    }


                    // Case 3: PCAP selected but not Success/Error (File unavailable, Idle, In Progress)
                    currentStateAfterPcap.isPcapSelected -> {
                        if (!currentStateAfterPcap.isPcapIncludable || currentStateAfterPcap.pcapFilePath == null) {
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (File not available for this session)."
                        } else if (pcapStateForAnalysis is PcapProcessingState.Idle) {
                            // This means conversion was triggered but failed or was cancelled without setting Error state properly.
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (Conversion did not complete successfully)."
                        } else if (currentStateAfterPcap.isPcapConverting) { // Check the helper property
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (Conversion still in progress)."
                        } else {
                            // Fallback for unexpected states
                            dynamicPromptPart += "\n\n- PCAP Network Summary: Not included (Unexpected state: ${pcapStateForAnalysis::class.simpleName})."
                            Log.w("AnalyzeAI", "PCAP selected but final state wasn't Success/Error/Idle/Converting. State: $pcapStateForAnalysis")
                        }
                    }

                    // Case 4: PCAP was not selected by the user
                    else -> {
                        dynamicPromptPart += "\n\n- PCAP Network Summary: Not selected."
                    }
                }
                dynamicPromptPart += "\n--- End Analysis Context ---"
                // --- End Include PCAP JSON ---

                val finalPrompt = "$basePrompt $dynamicPromptPart"
                Log.d("AnalyzeAI", "Final prompt being sent to model $effectiveModelName:\n$finalPrompt") // Log prompt for debugging

                // Build Content for Gemini
                val inputContent = content {
                    if (selectedScreenshots.isNotEmpty()) {
                        selectedScreenshots.forEach { screenshot ->
                            try {
                                val bitmap = pathToBitmap(screenshot.path)
                                image(bitmap)
                                Log.d("AnalyzeAI", "Added image: ${screenshot.path}")
                            } catch (e: Exception) {
                                Log.e("AnalyzeAI", "Failed to load or add image to prompt: ${screenshot.path}", e)
                                text("\n[System Note: Failed to load image at ${screenshot.path}]")
                            }
                        }
                    }
                    text(finalPrompt)
                }

                // Instantiate the model dynamically
                val config = generationConfig { temperature = 0.7f }
                val currentGenerativeModel = GenerativeModel(
                    modelName = effectiveModelName,
                    apiKey = BuildConfig.API_KEY_RELEASE, // Use correct API key build config
                    generationConfig = config
                )
                Log.i("AnalyzeAI", "Calling Gemini model '$effectiveModelName'...")

                // Generate Content Stream
                var outputContent = ""
                currentGenerativeModel.generateContentStream(inputContent)
                    .collect { response ->
                        response.text?.let { textChunk ->
                            outputContent += textChunk
                            // Update output text incrementally on the Main thread
                            withContext(Dispatchers.Main) {
                                // Use update to avoid race conditions if updates are rapid
                                _automaticAnalysisUiState.update { it.copy(outputText = outputContent) }
                            }
                        }
                        Log.d("AnalyzeAI", "Prompt Feedback: ${response.promptFeedback}")
                        Log.d("AnalyzeAI", "Finish Reason: ${response.candidates.firstOrNull()?.finishReason}")
                    }
                Log.i("AnalyzeAI", "Gemini streaming finished successfully for model '$effectiveModelName'.")

                // Final State Update after successful AI call
                withContext(Dispatchers.Main) {
                    _automaticAnalysisUiState.update { it.copy(isLoading = false, outputText = outputContent) }
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i("AnalyzeAI", "AI Analysis or preceding PCAP conversion was cancelled.")
                    // Reset loading state only if it's still true
                    withContext(Dispatchers.Main + NonCancellable) {
                        _automaticAnalysisUiState.update {
                            if (it.isLoading) {
                                it.copy(
                                    isLoading = false,
                                    error = it.error ?: "Analysis cancelled.", // Keep existing error if set (e.g., from PCAP cancel)
                                    outputText = null // Clear partial output
                                )
                            } else {
                                it // No change needed if not loading
                            }
                        }
                    }
                } else {
                    Log.e("AnalyzeAI", "Error during AI analysis phase with model $effectiveModelName", e)
                    withContext(Dispatchers.Main + NonCancellable) {
                        _automaticAnalysisUiState.update {
                            it.copy(
                                isLoading = false,
                                error = "AI Analysis failed: ${e.localizedMessage ?: context.getString(R.string.unknown_error)}"
                            )
                        }
                    }
                }
            } finally {
                // Ensure loading is always reset, defensively
                withContext(Dispatchers.Main + NonCancellable) {
                    _automaticAnalysisUiState.update {
                        if (it.isLoading) {
                            Log.d("AnalyzeAI", "Ensured isLoading is false in finally block.")
                            it.copy(isLoading = false)
                        } else {
                            it
                        }
                    }
                }
            }
        } // End viewModelScope.launch
    }




    // --- Toggle PCAP Selection ---
    override fun togglePcapSelection(isSelected: Boolean) {
        val currentState = _automaticAnalysisUiState.value
        // Allow toggling only if PCAP is includable
        if (currentState.isPcapIncludable) {
            // If turning ON and state is Idle/Error, initiate conversion
            if (isSelected && (currentState.pcapProcessingState is PcapProcessingState.Idle || currentState.pcapProcessingState is PcapProcessingState.Error)) {
                if (currentState.pcapFilePath != null) {
                    _automaticAnalysisUiState.value = currentState.copy(
                        isPcapSelected = true,
                        pcapProcessingState = PcapProcessingState.Idle, // Ensure state is Idle before starting
                        pcapJsonContent = null // Clear old content/error
                    )
                    // Launch conversion asynchronously - don't wait here
                    viewModelScope.launch {
                        try {
                            convertPcapToJsonAndWait(currentState.pcapFilePath)
                            // Success state is updated within convertPcapToJsonAndWait
                        } catch (e: Exception) {
                            if (e is CancellationException) {
                                Log.i("PCAP_Toggle", "PCAP Conversion cancelled during toggle ON.")
                                // State should be reset by cancelPcapConversion
                            } else {
                                Log.e(
                                    "PCAP_Toggle",
                                    "Error starting PCAP conversion on toggle ON",
                                    e
                                )
                                // Error state is updated within convertPcapToJsonAndWait
                            }
                        }
                    }
                } else {
                    Log.e("PCAP_Toggle", "Cannot select PCAP, file path is null.")
                    // Optionally update state with an error? For now, just log.
                }
            }
            // If turning OFF
            else if (!isSelected) {
                cancelPcapConversion("User deselected PCAP") // Cancel any ongoing job
                _automaticAnalysisUiState.value = currentState.copy(
                    isPcapSelected = false,
                    pcapProcessingState = PcapProcessingState.Idle // Reset state
                )
            }
            // If turning ON but already processing/success, just update the selection flag
            else if (isSelected) {
                _automaticAnalysisUiState.value = currentState.copy(isPcapSelected = true)
            }
        }
    }


    // ---  Retry PCAP Conversion ---
    override fun retryPcapConversion() {
        val currentState = _automaticAnalysisUiState.value
        // Only allow retry if PCAP is includable, was in Error state, and a file path exists
        if (currentState.isPcapIncludable && currentState.pcapProcessingState is PcapProcessingState.Error && currentState.pcapFilePath != null) {
            _automaticAnalysisUiState.value = currentState.copy(
                pcapProcessingState = PcapProcessingState.Idle, // Reset state to allow restart
                isPcapSelected = true // Assume user wants to retry because they clicked retry
            )
            // Launch conversion asynchronously
            viewModelScope.launch {
                try {
                    convertPcapToJsonAndWait(currentState.pcapFilePath)
                    // Success/Error state handled within the function
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.i("PCAP_Retry", "PCAP Conversion cancelled during retry.")
                    } else {
                        Log.e("PCAP_Retry", "Error starting PCAP conversion on retry", e)
                    }
                }
            }
        } else {
            Log.w(
                "PCAP_Retry",
                "Retry ignored. State: ${currentState.pcapProcessingState}, Path: ${currentState.pcapFilePath}"
            )
        }
    }


    /**
     * Generates the full prompt string based on current selections and PCAP state,
     * updating the `promptPreview` state without calling the AI model.
     * Handles PCAP status including truncation.
     */
    override fun generatePromptForPreview() {
        val initialCheckState = _automaticAnalysisUiState.value
        if (initialCheckState.sessionData == null) {
            Log.e("PromptPreview", "Cannot generate preview: Session data is null.")
            _automaticAnalysisUiState.update {
                it.copy(
                    promptPreview = "[Error: Session data not loaded]",
                    isGeneratingPreview = false
                )
            }
            return
        }
        if (initialCheckState.isGeneratingPreview) {
            Log.d("PromptPreview", "Generation already in progress, ignoring request.")
            return
        }

        // Start loading indicator
        _automaticAnalysisUiState.update {
            it.copy(
                isGeneratingPreview = true,
                promptPreview = null // Clear previous preview
            )
        }

        // Launch background coroutine for prompt generation
        viewModelScope.launch(Dispatchers.IO) {
            // Read the state needed for processing *inside* the background thread
            val stateForProcessing = _automaticAnalysisUiState.value

            try {
                // Read necessary properties from stateForProcessing
                val finalPromptResult = stateForProcessing.inputText
                withContext(Dispatchers.Main) {
                    // Use update to safely modify the state based on the latest value
                    _automaticAnalysisUiState.update { latestState ->
                        latestState.copy(
                            promptPreview = finalPromptResult,
                            isGeneratingPreview = false // Stop loading
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("PromptPreview", "Error generating prompt preview", e)
                withContext(Dispatchers.Main) {
                    // Use update to safely modify the state based on the latest value
                    _automaticAnalysisUiState.update { latestState ->
                        latestState.copy(
                            promptPreview = "Error generating prompt preview",
                            isGeneratingPreview = false // Stop loading
                        )
                    }
                }
            }
        } // End viewModelScope.launch
    }

    /**
     * Loads the session data for the given [clickedSessionId] from the database.
     *
     */
    override fun loadSessionData() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSessionId = _automaticAnalysisUiState.value.clickedSessionId
            if (currentSessionId == null) {
                Log.e("LoadSession", "Cannot load data, clickedSessionId is null.")
                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false,
                    error = "Session ID not found."
                )
                return@launch
            }

            _automaticAnalysisUiState.value =
                _automaticAnalysisUiState.value.copy(
                    isLoading = true,
                    error = null,
                    outputText = null
                ) // Reset output text too
            try {
                val sessionData = repository.getCompleteSessionData(currentSessionId).first()

                // --- Check for PCAP file ---
                val pcapPath = sessionData.session.pcapFilePath
                val isPcapPresent =
                    pcapPath != null && File(pcapPath).exists() // Check existence too

                //for debugging
                val file = if (pcapPath != null) File(pcapPath) else null
                val fileExists = file?.exists() == true
                Log.d("PcapCheck", "pcapPath from sessionData: $pcapPath")
                Log.d("PcapCheck", "File object created: ${file?.absolutePath}")
                Log.d("PcapCheck", "File.exists() result: $fileExists")

                // Reset PCAP state when loading new session data
                cancelPcapConversion("New session data loaded")

                // Default selections (Select all initially)
                val allRequestIds =
                    sessionData.requests?.mapNotNull { it.customWebViewRequestId }?.toSet()
                        ?: emptySet()
                val allScreenshotIds =
                    sessionData.screenshots?.filter { it.isPrivacyOrTosRelated }
                        ?.mapNotNull { it.screenshotId }?.toSet() ?: emptySet()
                val allWebpageContentIds =
                    sessionData.webpageContent?.mapNotNull { it.contentId }?.toSet()
                        ?: emptySet()

                _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                    isLoading = false, // Data loading finished
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
                Log.e("LoadSession", "Error during loadSessionData for ID $currentSessionId", e)
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
                    // Make sure state reflects the start
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

                    val request =
                        Request.Builder().url(SERVER_UPLOAD_URL).post(requestBody).build()

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

                        if (responseBodyString != null) {
                            try {
                                val result =
                                    jsonParser.decodeFromString<Map<String, String>>(
                                        responseBodyString
                                    )
                                val jobId = result["job_id"]
                                if (jobId != null) {
                                    Log.i("PCAP_CONVERT", "Upload successful, Job ID: $jobId")
                                    // --- Start Polling ---
                                    val finalJson =
                                        pollAndDownloadResult(jobId) // This will suspend until completion or error
                                    deferredResult.complete(finalJson) // Complete the deferred with the JSON
                                } else {
                                    throw IOException("Server response missing 'job_id' after upload.")
                                }
                            } catch (e: kotlinx.serialization.SerializationException) {
                                Log.e(
                                    "PCAP_CONVERT",
                                    "Failed to parse server response after upload: $responseBodyString",
                                    e
                                )
                                throw IOException("Invalid server response after upload.")
                            }
                        } else {
                            throw IOException("Server returned empty response body after upload.")
                        }
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.i("PCAP_CONVERT", "PCAP Conversion cancelled.")
                        updatePcapState(PcapProcessingState.Idle) // Reset state on cancellation
                        deferredResult.cancel(
                            CancellationException(
                                "PCAP conversion cancelled",
                                e
                            )
                        )
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
    private suspend fun pollAndDownloadResult(jobId: String): String =
        withContext(Dispatchers.IO) {
            pcapPollingJob?.cancelAndJoin() // Cancel previous polling

            var attempt = 0
            val deferredResult = CompletableDeferred<String>()

            // Only update state if the current job is still relevant (i.e., not cancelled)
            if (pcapConversionJob?.isActive == true) {
                updatePcapState(PcapProcessingState.Processing(jobId))
                Log.i("PCAP_POLL", "Starting polling for job: $jobId")
            } else {
                Log.i(
                    "PCAP_POLL",
                    "Polling skipped for $jobId, parent conversion job is not active."
                )
                deferredResult.cancel(CancellationException("Parent conversion job not active"))
                return@withContext deferredResult.await() // Return cancelled deferred
            }


            pcapPollingJob = launch pollLoop@{ // Child job for polling
                delay(PCAP_POLL_INITIAL_DELAY_MS) // Initial delay

                while (isActive && attempt < PCAP_POLL_MAX_ATTEMPTS) {
                    attempt++
                    Log.d("PCAP_POLL", "Polling attempt $attempt for job $jobId")
                    // Update state only if the job is still active
                    if (isActive) updatePcapState(
                        PcapProcessingState.Polling(
                            jobId,
                            attempt
                        )
                    ) else break

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
                                Log.d(
                                    "PCAP_POLL",
                                    "Status Response $jobId: $responseBodyString"
                                )
                                val statusResponse: JobStatusResponse
                                try {
                                    statusResponse =
                                        jsonParser.decodeFromString<JobStatusResponse>(
                                            responseBodyString
                                        )
                                } catch (e: kotlinx.serialization.SerializationException) {
                                    Log.e(
                                        "PCAP_POLL",
                                        "Failed to parse status response for $jobId: $responseBodyString",
                                        e
                                    )
                                    throw IOException("Invalid status response from server.")
                                }


                                when (statusResponse.status) {
                                    "completed" -> {
                                        statusResponse.url?.let { url ->
                                            Log.i(
                                                "PCAP_POLL",
                                                "Job $jobId completed. Downloading from $url"
                                            )
                                            // Update state only if job is still active
                                            if (isActive) updatePcapState(
                                                PcapProcessingState.DownloadingJson(
                                                    jobId,
                                                    url
                                                )
                                            ) else return@checkStatus

                                            try {
                                                val (jsonContent, truncated) = downloadJsonResult(url)

                                                // Update state only if job is still active
                                                if (isActive) {
                                                    updatePcapState(
                                                        PcapProcessingState.Success(
                                                            jobId,
                                                            jsonContent,
                                                            truncated
                                                        )
                                                    )
                                                    deferredResult.complete(jsonContent) // Success!
                                                } else {
                                                    deferredResult.cancel(
                                                        CancellationException(
                                                            "Job cancelled before download completion"
                                                        )
                                                    )
                                                }

                                                this@pollLoop.cancel("Job finished successfully") // Stop polling loop

                                            } catch (downloadError: Exception) {
                                                if (downloadError is CancellationException) throw downloadError
                                                Log.e(
                                                    "PCAP_DOWNLOAD",
                                                    "Failed to download JSON for $jobId",
                                                    downloadError
                                                )
                                                // Update state only if job is still active
                                                if (isActive) {
                                                    updatePcapState(
                                                        PcapProcessingState.Error(
                                                            "Download failed: ${downloadError.message}",
                                                            jobId
                                                        )
                                                    )
                                                    deferredResult.completeExceptionally(
                                                        downloadError
                                                    ) // Fail deferred
                                                } else {
                                                    deferredResult.cancel(
                                                        CancellationException(
                                                            "Job cancelled during download error handling"
                                                        )
                                                    )
                                                }
                                                this@pollLoop.cancel("Download error")
                                            }
                                        } ?: run {
                                            throw IOException("Job completed but result URL missing.")
                                        }
                                        return@checkStatus // Exit loop implicitly via cancellation
                                    }

                                    "failed" -> {
                                        val errorMsg =
                                            statusResponse.error
                                                ?: "Unknown server processing error"
                                        Log.e(
                                            "PCAP_POLL",
                                            "Job $jobId failed on server: $errorMsg"
                                        )
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
                        // Update state only if job is still active
                        if (isActive) {
                            updatePcapState(
                                PcapProcessingState.Error(
                                    "Polling error: ${e.message}",
                                    jobId
                                )
                            )
                            deferredResult.completeExceptionally(e) // Fail deferred
                        } else {
                            deferredResult.cancel(CancellationException("Job cancelled during polling error handling"))
                        }
                        this@pollLoop.cancel("Polling error") // Stop polling loop
                        return@pollLoop // Exit launch block
                    }
                } // End while loop

                // Handle timeout if loop finished due to maxAttempts
                if (isActive && attempt >= PCAP_POLL_MAX_ATTEMPTS) {
                    Log.w("PCAP_POLL", "Polling $jobId timed out ")
                    val timeoutError = IOException("Processing timed out on server.")
                    // Update state only if job is still active
                    if (isActive) {
                        updatePcapState(
                            PcapProcessingState.Error(
                                "Processing timed out.",
                                jobId
                            )
                        )
                        deferredResult.completeExceptionally(timeoutError)
                    } else {
                        deferredResult.cancel(CancellationException("Job cancelled before timeout completion"))
                    }
                }
            } // End polling launch

            // Wait for polling/downloading to complete or fail
            return@withContext deferredResult.await()
        }



    // --- Download JSON Function (Suspendable, returns String and truncation status) ---
    private suspend fun downloadJsonResult(url: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        Log.d("PCAP_DOWNLOAD", "Downloading JSON from: $url")
        val request = Request.Builder().url(url).get().build()
        var wasTruncated = false // Flag to track truncation

        pcapHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = try { response.body?.string()?.take(500) } catch (e: Exception) { "[Failed to read error body]" }
                Log.e(
                    "PCAP_DOWNLOAD",
                    "Download failed: ${response.code} ${response.message}. Body: $errorBody"
                )
                throw IOException("Download failed: ${response.code} ${response.message}")
            }

            val body = response.body ?: throw IOException("Downloaded JSON content body was null.")
            val source = body.source()
            val buffer = Buffer() // okio.Buffer
            var totalBytesRead = 0L
            val bufferSegmentSize = 8192L // Okio's default segment size

            try {
                while (totalBytesRead < PCAP_JSON_MAX_LENGTH) {
                    val bytesToRead = min(bufferSegmentSize, PCAP_JSON_MAX_LENGTH - totalBytesRead)
                    // Check if source is exhausted *before* attempting to read
                    if (source.exhausted()) {
                        break // Nothing more to read
                    }
                    val bytesRead = source.read(buffer, bytesToRead)
                    if (bytesRead == -1L) {
                        break // End of stream reached definitively
                    }
                    totalBytesRead += bytesRead
                }

                // After reading up to the limit, check if there's still more data in the source
                if (!source.exhausted()) {
                    wasTruncated = true
                    Log.w("PCAP_DOWNLOAD", "JSON content will be truncated to $PCAP_JSON_MAX_LENGTH bytes.")
                    // We don't need to read the rest, just know it exists.
                }

            } catch (e: IOException) {
                Log.e("PCAP_DOWNLOAD", "Error reading response stream", e)
                throw IOException("Error reading download stream: ${e.message}", e)
            }
            // No finally needed for source.close() as response.use handles it.

            // Use StandardCharsets for clarity and robustness
            val jsonString = buffer.readString(StandardCharsets.UTF_8)

            if (jsonString.isEmpty() && totalBytesRead == 0L && body.contentLength() != 0L) {
                // Check if known length wasn't 0 but we read nothing (potentially error or empty file)
                Log.w("PCAP_DOWNLOAD", "Downloaded JSON content is empty, but Content-Length was ${body.contentLength()}.")
                // Decide if this should be an error or allowed empty string
                // throw IOException("Downloaded JSON content was empty despite non-zero content length.")
            } else {
                Log.d("PCAP_DOWNLOAD", "Successfully read ${jsonString.length} chars / $totalBytesRead bytes. Truncated: $wasTruncated")
            }

            // Return the string and the truncation status
            return@withContext Pair(jsonString, wasTruncated)
        }
    }

    // ---  Helper to update PCAP state on Main thread ---
    private suspend fun updatePcapState(newState: PcapProcessingState) {
        // Check if the job is active (or if it's a final state like Idle/Error)
        if (pcapConversionJob?.isActive == true || newState is PcapProcessingState.Idle || newState is PcapProcessingState.Error || newState is PcapProcessingState.Success) {
            withContext(Dispatchers.Main) {
                // Determine the JSON content to store in the UI state's separate field
                val jsonContentForUiState = when (newState) {
                    is PcapProcessingState.Success -> newState.jsonContent // Get content from new Success state
                    is PcapProcessingState.Error, is PcapProcessingState.Idle -> null // Clear on Error or Idle
                    else -> _automaticAnalysisUiState.value.pcapJsonContent // Keep existing otherwise
                }

                // Check if the state or the relevant content has actually changed
                val currentState = _automaticAnalysisUiState.value
                if (currentState.pcapProcessingState != newState || currentState.pcapJsonContent != jsonContentForUiState) {
                    _automaticAnalysisUiState.value = currentState.copy(
                        pcapProcessingState = newState, // Store the full state including the truncation flag if Success
                        pcapJsonContent = jsonContentForUiState // Update the separate content field
                    )
                    Log.d("PcapStateUpdate", "Updated PCAP state to: $newState")
                }
            }
        } else {
            Log.d(
                "PcapStateUpdate",
                "Skipped PCAP state update to $newState because job is not active."
            )
        }
    }


    // ---  Helper to cancel ongoing PCAP conversion/polling ---
    private fun cancelPcapConversion(reason: String) {
        val wasPolling = pcapPollingJob?.isActive == true
        val wasConverting = pcapConversionJob?.isActive == true

        if (wasPolling) {
            Log.i("PCAP_CANCEL", "Cancelling PCAP polling: $reason")
            pcapPollingJob?.cancel(CancellationException(reason))
        }
        if (wasConverting) {
            Log.i("PCAP_CANCEL", "Cancelling PCAP conversion job: $reason")
            pcapConversionJob?.cancel(CancellationException(reason))
        }
        // Reset jobs only if they were active, avoid resetting if already null
        if (wasPolling) pcapPollingJob = null
        if (wasConverting) pcapConversionJob = null

        // Reset state to Idle only if it was actually converting/polling
        if (wasConverting || wasPolling) {
            // Use launch on Main thread to ensure UI state update happens correctly
            viewModelScope.launch(Dispatchers.Main) {
                if (_automaticAnalysisUiState.value.pcapProcessingState !is PcapProcessingState.Idle) {
                    _automaticAnalysisUiState.value = _automaticAnalysisUiState.value.copy(
                        pcapProcessingState = PcapProcessingState.Idle
                    )
                    Log.d("PCAP_CANCEL", "Reset PCAP state to Idle after cancellation.")
                }
            }
        }
    }


    override fun onCleared() {
        cancelPcapConversion("ViewModel cleared")
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
    val options = BitmapFactory.Options()
    val bitmap = BitmapFactory.decodeFile(path, options)
    if (bitmap == null) {
        throw IOException("Failed to decode bitmap from path: $path")
    }
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