package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis


import NetworkSessionRepository
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionDataDTO
import com.example.captive_portal_analyzer_kotlin.secret.Secret
import com.example.captive_portal_analyzer_kotlin.secret.Secret.apiKey
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/**
 * A sealed hierarchy describing the state of the text generation.
 */
sealed interface AutomaticAnalysisUiState {
    /**
     * Text generation is loading
     */
    data object Loading : AutomaticAnalysisUiState

    /**
     * Text has been generated
     */
    data class Success(
        val outputText: String
    ) : AutomaticAnalysisUiState

    /**
     * There was an error generating text
     */
    data class Error(
        val errorMessage: String
    ) : AutomaticAnalysisUiState
}

/**
 * A [ViewModel] that generates text based on a given [SessionData] using a generative AI model.
 *
 * @param generativeModel the generative AI model to use for generating text
 * @param application the application context
 * @param repository the repository of network sessions
 */
class AutomaticAnalysisViewModel(
    private val generativeModel: GenerativeModel,
    application: Application,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {


    private val _uiState: MutableStateFlow<AutomaticAnalysisUiState> =
        MutableStateFlow(AutomaticAnalysisUiState.Loading)
    val uiState: StateFlow<AutomaticAnalysisUiState> =
        _uiState.asStateFlow()

    /**
     * Analyzes the given session data using AI to generate a report about collected data, data privacy and ToS
     * of the captive portal. It updates the UI state based on the analysis result.
     *
     * @param sessionDataDTO the session data transfer object containing relevant information
     */
    fun analyzeWithAi(
        sessionDataDTO: SessionDataDTO
    ) {
        _uiState.value = AutomaticAnalysisUiState.Loading
        val prompt =
            "Look at the following prompt and image(s), and then give us a report about the data the" +
                    " captive portal collects about the user and any information regarding the dataprivacy" +
                    " and ToS of the captive portal if found" +
                    "and how you rate them: ${sessionDataDTO.prompt}"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputContent = content {
                    if (!sessionDataDTO.privacyOrTosRelatedScreenshots.isNullOrEmpty()) {
                        for (screenshot in sessionDataDTO.privacyOrTosRelatedScreenshots) {
                            val bitmap = pathToBitmap(screenshot.path)
                            image(bitmap)
                        }
                    }
                    text(prompt)
                }

                var outputContent = ""

                generativeModel.generateContentStream(inputContent)
                    .collect { response ->
                        outputContent += response.text
                        _uiState.value = AutomaticAnalysisUiState.Success(outputContent)
                    }
            } catch (e: Exception) {
                _uiState.value = AutomaticAnalysisUiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    /**
     * Loads the session data for the given [clickedSessionId] from the database.
     *
     * @param clickedSessionId the id of the session to load
     * @return the loaded session data or null if an exception occurred
     */
    fun loadSessionData(clickedSessionId: String?): SessionData? = runBlocking(Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        return@runBlocking try {
            if (clickedSessionId == null) {
                _uiState.value = AutomaticAnalysisUiState.Error(context.getString(R.string.session_id_not_found))
                return@runBlocking null
            }
            repository.getCompleteSessionData(clickedSessionId).first()
        } catch (e: Exception) {
            null
        }
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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutomaticAnalysisViewModel::class.java)) {
            // set up the generative model with the current API key
            val config = generationConfig {
                temperature = 0.7f
            }
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash-latest", // Specifies the name of the generative model to be used for analysis
                apiKey = Secret.apiKey,//apiKey to authenticate requests //todo replace with BuildConfig.apiKey
                generationConfig = config // Applies the configuration settings for generation
            )
            @Suppress("UNCHECKED_CAST")
            return AutomaticAnalysisViewModel(
                generativeModel,
                application,
                repository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}