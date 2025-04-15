package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

// import androidx.compose.material.icons.filled.Close // Removed unused import
// Import the HidableAnalysisHints composable

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components.CollapsibleAnalysisHints
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputError
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputLoading
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputStreaming
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateOutputSuccess
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomaticAnalysisOutputScreen(
    navController: NavController,
    viewModel: IAutomaticAnalysisViewModel,
    mainViewModel: IMainViewModel
) {
    val uiState by viewModel.automaticAnalysisUiState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()

    // --- State for Hints Section ---
    var areHintsVisible by rememberSaveable { mutableStateOf(true) }
    // Make Hints COLLAPSED by default
    var isHintsExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.automatic_analysis_result)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
            ) {

                // --- Collapsible Header: Selected Files & Prompt ---
                AnalysisInfoHeader(uiState = uiState)
                Divider(modifier = Modifier.padding(bottom = 8.dp)) // Padding below divider

                // --- Analysis Content Area (Takes most space) ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()) // Only this part scrolls
                ) {
                    when {
                        // Loading indicator when analysis is running, output empty
                        uiState.isLoading && uiState.outputText.isNullOrEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(top = 30.dp), // Centered vertically
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(message = stringResource(R.string.analyzing_data))
                            }
                        }
                        // Error state
                        uiState.error != null && !uiState.isLoading -> {
                            ErrorComponent(
                                error = uiState.error!!,
                                icon = ErrorIcon.ResourceIcon(R.drawable.robot),
                                onRetryClick = { viewModel.analyzeWithAi() }
                            )
                        }
                        // Success state (finished analysis)
                        !uiState.outputText.isNullOrEmpty() && !uiState.isLoading -> {
                            AutomaticAnalysisResultContent(outputText = uiState.outputText)
                        }
                        // Streaming state (loading but partial output exists)
                        uiState.isLoading && !uiState.outputText.isNullOrEmpty() -> {
                            AutomaticAnalysisResultContent(outputText = uiState.outputText)
                            // Subtle loading indicator at the end during streaming
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.receiving_results),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        // Initial/Idle state (before analysis starts)
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(top = 30.dp), // Centered vertically
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.analysis_pending))
                            }
                        }
                    }
                } // End Scrollable Content Column (weight=1f)

                // --- Collapsible/Hidable Hints at Bottom ---
                // Only show hints section *structure* if analysis is complete and not loading
                if (!uiState.outputText.isNullOrEmpty() && !uiState.isLoading) {
                    // Divider only shown when hints are potentially visible
                    AnimatedVisibility(visible = areHintsVisible) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    CollapsibleAnalysisHints(
                        isVisible = areHintsVisible,
                        isExpanded = isHintsExpanded,
                        onToggleExpand = { isHintsExpanded = !isHintsExpanded },
                        onDismiss = { areHintsVisible = false }
                    )
                    // Add padding below hints only if they are visible (takes space)
                    if (areHintsVisible) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

            } // End Main Column

            // --- Internet Banner (Anchored to bottom of Box) ---
            AnimatedNoInternetBanner(
                isConnected = isConnected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

        } // End Box
    } // End Scaffold
}

// --- Helper composable for the *collapsible* info header ---
@Composable
fun AnalysisInfoHeader(uiState: AutomaticAnalysisUiState) {
    val sessionData = uiState.sessionData ?: return // Don't show if no data
    // --- Start COLLAPSED by default ---
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // Data retrieval
    val selectedRequests = sessionData.requests?.filter { uiState.selectedRequestIds.contains(it.customWebViewRequestId) } ?: emptyList()
    val selectedScreenshots = sessionData.screenshots?.filter { uiState.selectedScreenshotIds.contains(it.screenshotId) } ?: emptyList()
    val selectedWebpageContent = sessionData.webpageContent?.filter { uiState.selectedWebpageContentIds.contains(it.contentId) } ?: emptyList()
    val pcapFilename = if (uiState.isPcapSelected && !uiState.pcapFilePath.isNullOrBlank()) {
        uiState.pcapFilePath.substringAfterLast('/')
    } else null
    val anyDataSelected = selectedRequests.isNotEmpty() || selectedScreenshots.isNotEmpty() || selectedWebpageContent.isNotEmpty() || pcapFilename != null

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Header Row (Always Visible, Clickable) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp), // Consistent padding for clickable area
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.analysis_based_on),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse_analysis_details) else stringResource(R.string.expand_analysis_details),
                modifier = Modifier.size(20.dp)
            )
        }

        // --- Collapsible Content (Prompt & Data Summary) ---
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) { // Padding when expanded

                // Display Prompt
                Text(
                    text = stringResource(R.string.prompt_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.inputText,
                    style = MaterialTheme.typography.bodySmall, // Smaller text for prompt
                    maxLines = 4, // Limit lines when expanded
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = FontStyle.Italic
                )
                Spacer(Modifier.height(6.dp))

                // Display Selected Files Summary
                if (anyDataSelected){
                    Text(
                        text = stringResource(R.string.selected_data_summary),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Network Requests
                    if(selectedRequests.isNotEmpty()){
                        Text(text = "- ${selectedRequests.size} Network Requests", style = MaterialTheme.typography.bodySmall)
                    }
                    // Screenshots
                    if(selectedScreenshots.isNotEmpty()){
                        Text(
                            text = "- ${selectedScreenshots.size} Screenshots",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, // Show only count when expanded
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Webpage Content
                    if(selectedWebpageContent.isNotEmpty()){
                        Text(
                            text = "- ${selectedWebpageContent.size} Webpage Content files",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, // Show only count when expanded
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // PCAP File
                    if (pcapFilename != null) {
                        Text(
                            text = "- PCAP File: $pcapFilename",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, // Show filename concisely
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (uiState.isPcapSelected) { // Added case: PCAP was selected but not included (e.g., error)
                        Text(
                            text = "- PCAP File: Not included (see error/status)",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                } else {
                    Text(
                        text= stringResource(R.string.no_data_selected_for_analysis),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
            } // End Collapsible Content Column
        } // End AnimatedVisibility
    } // End Main Header Column
}


// Helper composable for the main result content (Markdown) - Minor Padding Adjustment
@Composable
fun AutomaticAnalysisResultContent(outputText: String?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 8.dp), // Reduced top padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_gemini),
                contentDescription = "Google Gemini Icon",
                modifier = Modifier.size(28.dp), // Slightly smaller icon
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.ai_analysis),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // Smaller title
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Box for the markdown text
        Box(modifier = Modifier.fillMaxWidth()) {
            if (outputText != null) {
                MarkdownText(
                    markdown = outputText,
                    modifier = Modifier.padding(bottom = 8.dp), // Padding below markdown
                    style = MaterialTheme.typography.bodyLarge // Keep result text size large
                )
            }
        }
    }
}


// --- Previews ---

// Previews remain largely the same for the Output screen
@Preview(name = "Output - Loading Analysis", showBackground = true)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Loading_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputLoading)

    AppTheme(darkTheme = false) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Loading Analysis (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Loading_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputLoading)

    AppTheme(darkTheme = true) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Streaming Analysis", showBackground = true)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Streaming_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputStreaming)

    AppTheme(darkTheme = false) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Streaming Analysis (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Streaming_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputStreaming)

    AppTheme(darkTheme = true) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}


@Preview(name = "Output - Error State", showBackground = true)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Error_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputError)

    AppTheme(darkTheme = false) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Error State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisOutputScreenPreview_Error_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputError)

    AppTheme(darkTheme = true) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Success State", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisOutputScreenPreview_Success_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputSuccess)

    AppTheme(darkTheme = false) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Success State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisOutputScreenPreview_Success_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputSuccess)

    AppTheme(darkTheme = true) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Output - Success State (Tablet)", showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun AutomaticAnalysisOutputScreenPreview_Success_Tablet() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateOutputSuccess)

    AppTheme(darkTheme = false) {
        AutomaticAnalysisOutputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}
