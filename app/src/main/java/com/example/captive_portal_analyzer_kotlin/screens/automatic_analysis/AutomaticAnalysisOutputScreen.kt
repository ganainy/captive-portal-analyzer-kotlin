package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.IMainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.ErrorIcon
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputError
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputLoading
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputStreaming
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputSuccess
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

// Define Route for Navigation
const val AutomaticAnalysisOutputRoute = "automatic_analysis_output"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomaticAnalysisOutputScreen(
    navController: NavController,
    viewModel: IAutomaticAnalysisViewModel,
    mainViewModel: IMainViewModel
) {
    val uiState by viewModel.automaticAnalysisUiState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.automatic_analysis_result)) }, // Title
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) { // Back button
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                // Make only the result scrollable, keep header fixed
            ) {

                // --- Fixed Header: Selected Files & Prompt ---
                AnalysisInfoHeader(uiState = uiState)
                Divider(modifier = Modifier.padding(vertical = 8.dp))


                // --- Analysis Content Area ---
                // This Column will contain Loading, Error, or Result
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when {
                        // Show loading indicator *only* when analysis is running
                        uiState.isLoading && uiState.outputText.isNullOrEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(top = 50.dp), // Add padding to center better
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(message = stringResource(R.string.analyzing_data))
                            }
                        }
                        // Show error if analysis failed (error is not null and not loading)
                        uiState.error != null && !uiState.isLoading -> {
                            ErrorComponent(
                                error = uiState.error!!,
                                icon = ErrorIcon.ResourceIcon(R.drawable.robot),
                                onRetryClick = { viewModel.analyzeWithAi() } // Retry analysis
                            )
                        }
                        // Show the result once analysis is finished (not loading and output exists)
                        !uiState.outputText.isNullOrEmpty() && !uiState.isLoading -> {
                            AutomaticAnalysisResultContent(outputText = uiState.outputText)
                        }
                        // Handle streaming output (loading is true but output is partially available)
                        uiState.isLoading && !uiState.outputText.isNullOrEmpty() -> {
                            AutomaticAnalysisResultContent(outputText = uiState.outputText)
                            // Optionally add a subtle loading indicator at the bottom during streaming
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical=8.dp), horizontalArrangement = Arrangement.Center){
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.receiving_results), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        // Initial state before clicking Analyze (shouldn't usually be seen here, but handle defensively)
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .wrapContentSize(Alignment.Center)
                            ) {
                                Text(stringResource(R.string.analysis_pending)) // Placeholder text
                            }
                        }
                    }
                } // End Scrollable Content Column

                // --- Fixed Hints at Bottom --- (Only show if output is generated)
                if (!uiState.outputText.isNullOrEmpty() && !uiState.isLoading) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AnalysisHints()
                }

            } // End Main Column

            // --- Internet Banner ---
            AnimatedNoInternetBanner(isConnected = isConnected)

        } // End Box
    } // End Scaffold
}

// Helper composable for the fixed info header
@Composable
fun AnalysisInfoHeader(uiState: AutomaticAnalysisUiState) {
    val sessionData = uiState.sessionData ?: return // Don't show if no data

    val selectedRequests = sessionData.requests?.filter { uiState.selectedRequestIds.contains(it.customWebViewRequestId) } ?: emptyList()
    val selectedScreenshots = sessionData.screenshots?.filter { uiState.selectedScreenshotIds.contains(it.screenshotId) } ?: emptyList()
    val selectedWebpageContent = sessionData.webpageContent?.filter { uiState.selectedWebpageContentIds.contains(it.contentId) } ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.analysis_based_on),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))

        // Display Prompt
        Text(
            text = stringResource(R.string.prompt_used, uiState.inputText), // Use formatted string
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            fontStyle = FontStyle.Italic
        )
        Spacer(Modifier.height(8.dp))

        // Display Selected Files Summary (Optional: Make this expandable if needed)
        if (selectedRequests.isNotEmpty() || selectedScreenshots.isNotEmpty() || selectedWebpageContent.isNotEmpty()){
            Text(text = stringResource(R.string.selected_data_summary), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            if(selectedRequests.isNotEmpty()){
                Text(text = "- ${selectedRequests.size} Network Requests", style = MaterialTheme.typography.bodySmall)
            }
            if(selectedScreenshots.isNotEmpty()){
                Text(text = "- ${selectedScreenshots.size} Screenshots: ${selectedScreenshots.joinToString { it.path.substringAfterLast('/') }}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if(selectedWebpageContent.isNotEmpty()){
                Text(text = "- ${selectedWebpageContent.size} Webpage Content: ${selectedWebpageContent.joinToString { (it.htmlContentPath ?: it.jsContentPath ?: "").substringAfterLast('/') }}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(text= stringResource(R.string.no_data_selected_for_analysis), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        }


    }
}

// Helper composable for the main result content (Markdown)
@Composable
fun AutomaticAnalysisResultContent(outputText: String?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp), // Adjust padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_gemini),
                contentDescription = "Google Gemini Icon",
                modifier = Modifier.requiredSize(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.here_is_the_result),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Use Box for the markdown text only, allowing the column above to scroll
        Box(modifier = Modifier.fillMaxWidth()) {
            if (outputText != null) {
                MarkdownText(
                    markdown = outputText,
                    modifier = Modifier.padding(bottom = 16.dp), // Add padding below markdown
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Helper composable for the hints at the bottom
@Composable
fun AnalysisHints() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp) // Add padding below hints
    ) {
        HintTextWithIcon(hint = stringResource(R.string.ai_hint_1), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(4.dp))
        HintTextWithIcon(hint = stringResource(R.string.ai_hint_2), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(4.dp))
        HintTextWithIcon(
            hint = stringResource(R.string.hint_network), textAlign = TextAlign.Start
        )
    }
}


// --- Previews ---

@Preview(name = "Output - Loading Analysis", showBackground = true)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Loading_Light() { // Split light/dark
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputLoading)

    AppTheme(darkTheme = false) { // Use AppTheme (Light)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Loading Analysis (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Loading_Dark() { // Split light/dark
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputLoading)

    AppTheme(darkTheme = true) { // Use AppTheme (Dark)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Streaming Analysis", showBackground = true)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Streaming_Light() { // Split light/dark
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputStreaming)

    AppTheme(darkTheme = false) { // Use AppTheme (Light)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Streaming Analysis (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Streaming_Dark() { // Split light/dark
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputStreaming)

    AppTheme(darkTheme = true) { // Use AppTheme (Dark)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}


@Preview(name = "Output - Error State", showBackground = true)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Error_Light() { // Split light/dark
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputError)

    AppTheme(darkTheme = false) { // Use AppTheme (Light)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Error State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Error_Dark() { // Split light/dark
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputError)

    AppTheme(darkTheme = true) { // Use AppTheme (Dark)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Success State", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisOutputScreenPreview_Success_Light() { // Split light/dark/tablet
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputSuccess)

    AppTheme(darkTheme = false) { // Use AppTheme (Light)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Success State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisOutputScreenPreview_Success_Dark() { // Split light/dark/tablet
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputSuccess)

    AppTheme(darkTheme = true) { // Use AppTheme (Dark)
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Success State (Tablet)", showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun AutomaticAnalysisOutputScreenPreview_Success_Tablet() { // Split light/dark/tablet
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputSuccess)

    AppTheme(darkTheme = false) { // Use AppTheme (Light) - Assuming light tablet preview
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}