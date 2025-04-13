package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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

    // --- State for Hints Section ---
    // Use rememberSaveable to keep state across configuration changes/process death
    var areHintsVisible by rememberSaveable { mutableStateOf(true) }
    var isHintsExpanded by rememberSaveable { mutableStateOf(true) } // Start expanded

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

                // --- Collapsible/Hidable Hints at Bottom ---
                // Only show if output is generated, not loading, and hints haven't been dismissed
                if (!uiState.outputText.isNullOrEmpty() && !uiState.isLoading) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    CollapsibleAnalysisHints(
                        isVisible = areHintsVisible,
                        isExpanded = isHintsExpanded,
                        onToggleExpand = { isHintsExpanded = !isHintsExpanded },
                        onDismiss = { areHintsVisible = false }
                    )
                    // Add padding below hints if they are visible
                    if (areHintsVisible) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

            } // End Main Column

            // --- Internet Banner ---
            AnimatedNoInternetBanner(isConnected = isConnected)

        } // End Box
    } // End Scaffold
}

// --- Helper composable for the *collapsible* info header ---
@Composable
fun AnalysisInfoHeader(uiState: AutomaticAnalysisUiState) {
    val sessionData = uiState.sessionData ?: return // Don't show if no data
    var isExpanded by rememberSaveable { mutableStateOf(true) } // State for expansion, default true

    // Data retrieval (keep as is)
    val selectedRequests = sessionData.requests?.filter { uiState.selectedRequestIds.contains(it.customWebViewRequestId) } ?: emptyList()
    val selectedScreenshots = sessionData.screenshots?.filter { uiState.selectedScreenshotIds.contains(it.screenshotId) } ?: emptyList()
    val selectedWebpageContent = sessionData.webpageContent?.filter { uiState.selectedWebpageContentIds.contains(it.contentId) } ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Header Row (Clickable Title + Icon) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded } // Toggle expansion on row click
                .padding(vertical = 8.dp), // Padding for the clickable header row
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.analysis_based_on),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f) // Title takes available space
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse_analysis_details) else stringResource(R.string.expand_analysis_details),
                modifier = Modifier.size(24.dp)
            )
        }

        // --- Collapsible Content (Prompt & Data Summary) ---
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) { // Indent content slightly

                // Display Prompt
                Text(
                    text = stringResource(R.string.prompt_used, uiState.inputText), // Use formatted string
                    style = MaterialTheme.typography.bodyMedium,
                    // Keep maxLines/overflow or remove if it should show fully when expanded
                    maxLines = 10, // Allow more lines when expanded
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = FontStyle.Italic
                )
                Spacer(Modifier.height(8.dp))

                // Display Selected Files Summary
                if (selectedRequests.isNotEmpty() || selectedScreenshots.isNotEmpty() || selectedWebpageContent.isNotEmpty()){
                    Text(text = stringResource(R.string.selected_data_summary), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    if(selectedRequests.isNotEmpty()){
                        Text(text = "- ${selectedRequests.size} Network Requests", style = MaterialTheme.typography.bodySmall)
                    }
                    if(selectedScreenshots.isNotEmpty()){
                        Text(text = "- ${selectedScreenshots.size} Screenshots: ${selectedScreenshots.joinToString { it.path.substringAfterLast('/') }}", style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis) // Allow more lines
                    }
                    if(selectedWebpageContent.isNotEmpty()){
                        Text(text = "- ${selectedWebpageContent.size} Webpage Content: ${selectedWebpageContent.joinToString { (it.htmlContentPath ?: it.jsContentPath ?: "").substringAfterLast('/') }}", style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis) // Allow more lines
                    }
                } else {
                    Text(text= stringResource(R.string.no_data_selected_for_analysis), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }
            } // End Collapsible Content Column
        } // End AnimatedVisibility
    } // End Main Header Column
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

// Collapsible and Hidable Hints Section ---
@Composable
fun CollapsibleAnalysisHints(
    isVisible: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    // Only render the hint section if it's set to be visible
    AnimatedVisibility(visible = isVisible) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Header Row for Hints ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand) // Make the whole row clickable to toggle
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = stringResource(R.string.analysis_hints_title), // Use a dedicated string resource
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f) // Take available space
                )

                // Expand/Collapse Icon Button
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse_hints) else stringResource(R.string.expand_hints),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(8.dp)) // Space before dismiss icon

                // Dismiss ('X') Icon Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp) // Make dismiss icon clickable area smaller
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.dismiss_hints)
                    )
                }
            } // End Header Row

            // --- Animated Content for Hints ---
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) { // Indent hints slightly
                    HintTextWithIcon(hint = stringResource(R.string.ai_hint_1), textAlign = TextAlign.Start)
                    Spacer(modifier = Modifier.height(4.dp))
                    HintTextWithIcon(hint = stringResource(R.string.ai_hint_2), textAlign = TextAlign.Start)
                    Spacer(modifier = Modifier.height(4.dp))
                    HintTextWithIcon(
                        hint = stringResource(R.string.hint_network), textAlign = TextAlign.Start
                    )
                }
            } // End Animated Content
        } // End Main Column for Hints
    } // End AnimatedVisibility for overall visibility
}


// --- Previews ---

// (Keep existing previews as they are, they will now reflect the collapsible hints)

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

