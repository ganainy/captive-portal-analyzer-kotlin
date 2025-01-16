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
import com.example.captive_portal_analyzer_kotlin.components.HintText
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.dataclasses.toSessionDataDTO
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun AutomaticAnalysisScreen(
    sharedViewModel: SharedViewModel,
    repository: NetworkSessionRepository,
) {

    val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
        factory = AutomaticAnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
        )
    )


    val clickedSessionId by sharedViewModel.clickedSessionId.collectAsState()
    val uiState by automaticAnalysisViewModel.uiState.collectAsState()

    val sessionData =
        automaticAnalysisViewModel.loadSessionData(clickedSessionId = clickedSessionId!!)
    val sessionDataDTO = sessionData!!.toSessionDataDTO()
    automaticAnalysisViewModel.analyzeWithAi(sessionDataDTO)

    val isConnected by sharedViewModel.isConnected.collectAsState()

    Scaffold(

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        AutomaticAnalysisContent(uiState = uiState, modifier = Modifier.padding(paddingValues), onRetryClick = { automaticAnalysisViewModel.analyzeWithAi(sessionDataDTO) })
        AnimatedNoInternetBanner(isConnected = isConnected)
    }
    }
}

@Composable
fun AutomaticAnalysisContent(
    uiState: AutomaticAnalysisUiState = AutomaticAnalysisUiState.Loading,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {}
) {

    when (uiState) {
        AutomaticAnalysisUiState.Initial -> {
            // Nothing is shown
        }

        AutomaticAnalysisUiState.Loading -> {
            LoadingIndicator(message = stringResource(R.string.uploading_information_to_be_analyzed))
        }

        is AutomaticAnalysisUiState.Success -> {
            AutomaticAnalysisResult(uiState)
        }

        is AutomaticAnalysisUiState.Error -> {
            ErrorComponent(
                error = uiState.errorMessage,
                icon = ErrorIcon.ResourceIcon(R.drawable.robot),
                onRetryClick = onRetryClick
            )
        }
    }
}


@Composable
private fun AutomaticAnalysisResult(uiState: AutomaticAnalysisUiState.Success) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Fixed Header with Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_gemini),
                contentDescription = "Google Gemini Icon",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .requiredSize(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(id = R.string.here_is_the_result),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )


        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Card
        Card(
            modifier = Modifier
                .weight(1f) // Take available space between header and hints
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                MarkdownText(
                    markdown  = uiState.outputText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fixed Hints at Bottom
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            HintText(hint = stringResource(R.string.ai_hint_1), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(4.dp))
            HintText(hint = stringResource(R.string.ai_hint_2), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(4.dp))
            HintText(hint = stringResource(R.string.hint_network), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Composable
@Preview(showBackground = true)
fun AutomaticAnalysisResultPreview() {
    AutomaticAnalysisResult(AutomaticAnalysisUiState.Success(stringResource(R.string.long_lorem_ipsum)))
}


@Composable
@Preview(showSystemUi = true)
fun AutomaticAnalysisContentPreview() {
    AutomaticAnalysisContent()
}
