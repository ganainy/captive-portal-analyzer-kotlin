package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import NetworkSessionRepository
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.ErrorIcon
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.dataclasses.toSessionDataDTO
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * Composable function for displaying the results of the automatic analysis of collected
 * captive portal network data using an AI model.
 *
 * @param sharedViewModel The shared view model containing the clicked session ID and
 * connectivity status.
 * @param repository The repository to access network session data.
 */
@Composable
fun AutomaticAnalysisScreen(
    sharedViewModel: SharedViewModel,
    repository: NetworkSessionRepository,
) {

    // Initialize the AutomaticAnalysisViewModel using a factory pattern
    val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
        factory = AutomaticAnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
        )
    )

    // Collect the clicked session ID from sharedViewModel
    val clickedSessionId by sharedViewModel.clickedSessionId.collectAsState()

    // Observe the UI state from automaticAnalysisViewModel
    val uiState by automaticAnalysisViewModel.uiState.collectAsState()

    // Load session data using the clicked session ID
    val sessionData =
        automaticAnalysisViewModel.loadSessionData(clickedSessionId = clickedSessionId)

    // Convert session data to DTO format to prepare it for transmission to AI server for analysis
    val sessionDataDTO = sessionData.toSessionDataDTO()
    // Trigger AI analysis with the session data DTO
    automaticAnalysisViewModel.analyzeWithAi(sessionDataDTO)

    // Observe network connectivity status to show/hide internet banner
    val isConnected by sharedViewModel.isConnected.collectAsState()

    Scaffold(

    ) { paddingValues ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AutomaticAnalysisContent(uiState = uiState,
                modifier = Modifier,
                onRetryClick = { automaticAnalysisViewModel.analyzeWithAi(sessionDataDTO) })
            AnimatedNoInternetBanner(isConnected = isConnected)
        }
    }
}

/**
 * A composable function that displays the results of the automatic analysis of
 * collected captive portal network data using an AI model.
 *
 * @param uiState The current state of the automatic analysis process, which can be
 * one of the following: [AutomaticAnalysisUiState.Loading], [AutomaticAnalysisUiState.Success],
 * or [AutomaticAnalysisUiState.Error].
 * @param modifier The modifier to apply to the root composable of this component.
 * @param onRetryClick A callback to call when the user clicks the retry button in
 * case of an error.
 */
@Composable
fun AutomaticAnalysisContent(
    uiState: AutomaticAnalysisUiState = AutomaticAnalysisUiState.Loading,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {}
) {
    // Handle different UI states based on the automatic analysis process
    when (uiState) {
        // show loading indicator while analyzing is in progress
        AutomaticAnalysisUiState.Loading -> {
            LoadingIndicator(message = stringResource(R.string.uploading_information_to_be_analyzed))
        }

        //show the result of the analysis when response is received from AI server
        is AutomaticAnalysisUiState.Success -> {
            AutomaticAnalysisResult(uiState)
        }

        //show error message when there is an error
        is AutomaticAnalysisUiState.Error -> {
            ErrorComponent(
                error = uiState.errorMessage,
                icon = ErrorIcon.ResourceIcon(R.drawable.robot),
                onRetryClick = onRetryClick
            )
        }
    }
}


/**
 * Composable that displays the result of the automatic analysis. The result is a
 * piece of markdown text that is received from the AI server.
 *
 * @param uiState the state of the automatic analysis. The result of the analysis
 * is stored in the [AutomaticAnalysisUiState.Success] object.
 */
@Composable
private fun AutomaticAnalysisResult(uiState: AutomaticAnalysisUiState.Success) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Fixed Header with Icon. Used for showing the result header.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // This icon is used for the Google Gemini icon.
            Icon(
                painter = painterResource(id = R.drawable.google_gemini),
                contentDescription = "Google Gemini Icon",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .requiredSize(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // This text is used for the result header text.
            Text(
                text = stringResource(id = R.string.here_is_the_result),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )


        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Card. Used for showing the result markdown text.
        Card(
            modifier = Modifier
                .weight(1f) // Take available space between header and hints
                .fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // This box is used for scrolling the markdown text.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // This markdown text is used for showing the result markdown text properly formatted.
                MarkdownText(
                    markdown = uiState.outputText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fixed Hints at Bottom. Used for showing the result hints.
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // This text is used for showing the first hint.
            HintTextWithIcon(hint = stringResource(R.string.ai_hint_1), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(4.dp))
            // This text is used for showing the second hint.
            HintTextWithIcon(hint = stringResource(R.string.ai_hint_2), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(4.dp))
            // This text is used for showing the third hint.
            HintTextWithIcon(
                hint = stringResource(R.string.hint_network), textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

/**
 * Preview for the AutomaticAnalysisResult composable function.
 * It shows the result of an automatic analysis with a sample success state.
 */
@Composable
@Preview(showBackground = true)
fun AutomaticAnalysisResultPreview() {
    AutomaticAnalysisResult(AutomaticAnalysisUiState.Success(stringResource(R.string.long_lorem_ipsum)))
}

/**
 * Preview for the AutomaticAnalysisContent composable function.
 * It displays the UI for automatic analysis content, showing different states.
 */
@Composable
@Preview(showSystemUi = true)
fun AutomaticAnalysisContentPreview() {
    AutomaticAnalysisContent()
}