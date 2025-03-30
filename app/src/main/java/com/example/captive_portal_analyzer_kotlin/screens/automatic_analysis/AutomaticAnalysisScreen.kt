package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import NetworkSessionRepository
import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.ErrorIcon
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * Composable function for displaying the results of the automatic analysis of collected
 * captive portal network data using an AI model.
 *
 * @param mainViewModel The shared view model containing the clicked session ID and
 * connectivity status.
 * @param repository The repository to access network session data.
 */
@Composable
fun AutomaticAnalysisScreen(
    mainViewModel: MainViewModel,
    repository: NetworkSessionRepository,
) {

    // Collect the clicked session ID from sharedViewModel
    val clickedSessionId by mainViewModel.clickedSessionId.collectAsState()

    // Initialize the AutomaticAnalysisViewModel using a factory pattern
    val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
        factory = AutomaticAnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            clickedSessionId= clickedSessionId,
        )
    )

    // Observe the UI state from automaticAnalysisViewModel
    val automaticAnalysisUiState by automaticAnalysisViewModel.automaticAnalysisUiState.collectAsState()


    // Observe network connectivity status to show/hide internet banner
    val isConnected by mainViewModel.isConnected.collectAsState()

    Scaffold(

    ) { paddingValues ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AutomaticAnalysisContent(
                automaticAnalysisUiState = automaticAnalysisUiState,
                onRetryClick = { automaticAnalysisViewModel.analyzeWithAi() },
                inputText = automaticAnalysisUiState.inputText,
                onUpdateInputText = { automaticAnalysisViewModel.updatePromptEditText(it) },
                analyzeWithAI = { automaticAnalysisViewModel.analyzeWithAi() },
                isLoading = automaticAnalysisUiState.isLoading
            )
            AnimatedNoInternetBanner(isConnected = isConnected)
        }
    }
}

/**
 * A composable function that displays the result of automatic analysis using an AI model.
 *
 * The composable function displays one of the following states:
 * - A loading indicator if the analysis is in progress
 * - An error component if the analysis failed
 * - The result of the analysis if the analysis was successful
 *
 * @param automaticAnalysisUiState the UI state of the automatic analysis
 * @param modifier the modifier to apply to the composable
 * @param onRetryClick a lambda to call when the retry button is clicked
 * @param onUpdatePromptInputText a lambda to call when the user updates the custom prompt
 * @param analyzeWithAI a lambda to call when the user press the analyze button
 * @param inputText the custom prompt that the user has entered.
 * @param onUpdateInputText a lambda that is called when the user updates the custom prompt.
 * @param isLoading true if the AI analysis is in progress, false otherwise
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomaticAnalysisContent(
    automaticAnalysisUiState: AutomaticAnalysisUiState,
    onRetryClick: () -> Unit = {},
    inputText: String,
    onUpdateInputText: (String) -> Unit,
    analyzeWithAI: () -> Unit,
    isLoading: Boolean
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            value = inputText,
            onValueChange = { onUpdateInputText(it) },
            label = { Text(stringResource(R.string.custom_prompt)) },
            singleLine = false,
            maxLines = 5
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Button to start analysis with AI
            RoundCornerButton(
                onClick = analyzeWithAI,
                buttonText = stringResource(R.string.analyze_captive_portal_with_ai),
                enabled = !isLoading,
                isLoading = isLoading,
                fillWidth = false,
            )
            GhostButton(
                onClick = { onUpdateInputText("") },
                text = stringResource(R.string.clear_prompt),
                enabled = inputText.isNotEmpty()
            )
        }

        if (automaticAnalysisUiState.isLoading) {
            // Display a loading indicator if the analysis is in progress
            LoadingIndicator(message = stringResource(R.string.uploading_information_to_be_analyzed))
        } else if (automaticAnalysisUiState.error != null) {
            // Display error component if there is an error
            ErrorComponent(
                error = automaticAnalysisUiState.error,
                icon = ErrorIcon.ResourceIcon(R.drawable.robot),
                onRetryClick = onRetryClick
            )
        } else if (automaticAnalysisUiState.outputText.isNullOrEmpty()) {
            // Display a hint text if no result is available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                HintTextWithIcon(
                    hint = stringResource(R.string.no_result_yet),
                    rowAllignment = Alignment.Center
                )
            }
        }
        else {
            // Display the result if there is a result
            AutomaticAnalysisResult(
                automaticAnalysisUiState.outputText,
            )
        }

    }

}

/**
 * Composable that displays the result of the automatic analysis. The result is a
 * piece of markdown text that is received from the AI server.
 *
 * @param outputText the AI generated response returned from the AI server.
 */
@Composable
private fun AutomaticAnalysisResult(
    outputText: String?,
) {

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
                if (outputText != null) {
                    MarkdownText(
                        markdown = outputText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
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
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun AutomaticAnalysisResultPreview() {
    AutomaticAnalysisResult(
        outputText = stringResource(R.string.long_lorem_ipsum),
    )
}

/**
 * Preview for the AutomaticAnalysisContent composable function.
 * It displays the UI for automatic analysis content, showing different states.
 */
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun AutomaticAnalysisContentPreview() {
    AutomaticAnalysisContent(
        automaticAnalysisUiState = AutomaticAnalysisUiState(),
        onRetryClick = { },
        onUpdateInputText = { },
        isLoading = false,
        inputText = "",
        analyzeWithAI = { },
    )
}