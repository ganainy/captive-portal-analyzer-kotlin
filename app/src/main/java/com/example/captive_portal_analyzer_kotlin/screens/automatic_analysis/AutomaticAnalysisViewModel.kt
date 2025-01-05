package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis


import NetworkSessionRepository
import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionDataDTO
import com.example.captive_portal_analyzer_kotlin.secret.Secret
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
     * Empty state when the screen is first shown
     */
    data object Initial : AutomaticAnalysisUiState

    /**
     * Still loading
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


class AutomaticAnalysisViewModel(
    private val generativeModel: GenerativeModel,
    application: Application,
    private val repository: NetworkSessionRepository,
) : AndroidViewModel(application) {



    init {
    }

    private val _uiState: MutableStateFlow<AutomaticAnalysisUiState> =
        MutableStateFlow(AutomaticAnalysisUiState.Initial)
    val uiState: StateFlow<AutomaticAnalysisUiState> =
        _uiState.asStateFlow()

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
                    for (screenshot in sessionDataDTO.privacyOrTosRelatedScreenshots) {
                        val bitmap = pathToBitmap(screenshot.path)
                        image(bitmap)
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

    fun loadSessionData(clickedSessionId: String): SessionData? = runBlocking(Dispatchers.IO) {
        return@runBlocking try {
            clickedSessionId.let { sessionId ->
                repository.getCompleteSessionData(sessionId).first()
            }
        } catch (e: Exception) {
            null
        }
    }


}

    private fun pathToBitmap(path: String): Bitmap {
        val bitmap = BitmapFactory.decodeFile(path)
        return bitmap
    }

class AutomaticAnalysisViewModelFactory(
    private val application: Application,
    private val repository: NetworkSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutomaticAnalysisViewModel::class.java)) {
            val config = generationConfig {
                temperature = 0.7f
            }
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash-latest",
                apiKey = Secret.apiKey, //todo replace with BuildConfig.apiKey
                generationConfig = config
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
