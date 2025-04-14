package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.IMainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.navigation.AutomaticAnalysisOutputRoute
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components.DataSelectionCategory
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components.PcapSelectionItem
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.PcapProcessingState
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateInputLoaded
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutomaticAnalysisInputScreen(
    navController: NavController,
    viewModel: IAutomaticAnalysisViewModel,
    mainViewModel: IMainViewModel
) {
    val uiState by viewModel.automaticAnalysisUiState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()
    var isDataSelectionExpanded by remember { mutableStateOf(true) }

    // Determine initial loading state (only for fetching session data)
    val initialLoading = uiState.isLoading && uiState.sessionData == null && uiState.outputText == null && !uiState.isPcapConverting
    // Determine initial error state (error during session data fetch)
    val initialError = uiState.error != null && uiState.sessionData == null && !uiState.isLoading && !uiState.isPcapConverting

    // Determine if any data is *selectable* (used for enabling sections and buttons)
    val isAnyDataSelectable = uiState.totalRequestsCount > 0 ||
            uiState.totalScreenshotsCount > 0 ||
            uiState.totalWebpageContentCount > 0 ||
            uiState.isPcapIncludable // PCAP is selectable if includable

    // Determine if Analyze button should be enabled
    val isAnalyzeButtonEnabled = uiState.canStartAnalysis && isAnyDataSelectable
// Determine if the prompt clear button should be enabled
    val isPromptClearEnabled = !uiState.inputText.isEmpty() && !initialLoading && uiState.canStartAnalysis

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.automatic_analysis_input_screen_title)) })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()) // Ensure scrolling works
            ) {
                Spacer(Modifier.height(16.dp))

                // --- Prompt Input ---
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.inputText,
                    onValueChange = { viewModel.updatePromptEditText(it) },
                    label = { Text(stringResource(R.string.custom_prompt)) },
                    singleLine = false,
                    maxLines = 4,
                    // Disable if initially loading OR if PCAP is converting OR if AI analysis is running
                    enabled = !initialLoading && uiState.canStartAnalysis,
                    // --- Add trailing icon ---
                    trailingIcon = {
                        // Show icon only if text is not empty and field is enabled
                        if (isPromptClearEnabled) {
                            IconButton(
                                onClick = { viewModel.updatePromptEditText("") }, // Clear text on click
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear_prompt)
                                )
                            }
                        }
                    }
                    // --- End trailing icon ---
                )

                Spacer(Modifier.height(16.dp))

                // --- Toggle Data Selection Visibility ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Allow toggling even if converting, unless it's initial load/error
                        .clickable(enabled = !initialLoading && !initialError) { isDataSelectionExpanded = !isDataSelectionExpanded }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.include_data_in_analysis),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = if (isDataSelectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isDataSelectionExpanded) stringResource(R.string.collapse_data_selection) else stringResource(R.string.expand_data_selection)
                    )
                }

                // --- Collapsible Data Selection Section ---
                AnimatedVisibility(
                    visible = isDataSelectionExpanded && !initialLoading && !initialError, // Hide if initial loading/error
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        // Network Requests Category
                        DataSelectionCategory(
                            title = stringResource(
                                R.string.network_requests_selected,
                                uiState.selectedRequestIds.size,
                                uiState.totalRequestsCount
                            ),
                            items = uiState.sessionData?.requests ?: emptyList(),
                            selectedIds = uiState.selectedRequestIds,
                            idProvider = { request: CustomWebViewRequestEntity -> request.customWebViewRequestId },
                            contentDescProvider = { request: CustomWebViewRequestEntity ->
                                val urlPart = request.url?.take(60)?.let { if (request.url.length > 60) "$it..." else it } ?: "N/A"
                                "${request.method} $urlPart"
                            },
                            onToggle = { viewModel.toggleRequestSelection(it) },
                            onSetAllSelected = { viewModel.setAllRequestsSelected(it) },
                            // Disable selection controls if PCAP/AI is running
                            enabled = uiState.canStartAnalysis && uiState.totalRequestsCount > 0
                        )
                        // Screenshots Category
                        DataSelectionCategory(
                            title = stringResource(
                                R.string.screenshots_selected,
                                uiState.selectedScreenshotIds.size,
                                uiState.totalScreenshotsCount // Total is only privacy/tos related ones
                            ),
                            items = uiState.sessionData?.screenshots?.filter { it.isPrivacyOrTosRelated } ?: emptyList(), // Show only PrivacyOrTos relevant ones
                            selectedIds = uiState.selectedScreenshotIds,
                            idProvider = { screenshot: ScreenshotEntity -> screenshot.screenshotId },
                            contentDescProvider = { screenshot: ScreenshotEntity ->
                                "Screenshot: ${screenshot.path.substringAfterLast('/')}"
                            },
                            onToggle = { viewModel.toggleScreenshotSelection(it) },
                            onSetAllSelected = { viewModel.setAllScreenshotsSelected(it) },
                            // Disable selection controls if PCAP/AI is running
                            enabled = uiState.canStartAnalysis && uiState.totalScreenshotsCount > 0
                        )
                        // Webpage Content Category
                        DataSelectionCategory(
                            title = stringResource(
                                R.string.webpage_content_selected,
                                uiState.selectedWebpageContentIds.size,
                                uiState.totalWebpageContentCount
                            ),
                            items = uiState.sessionData?.webpageContent ?: emptyList(),
                            selectedIds = uiState.selectedWebpageContentIds,
                            idProvider = { content: WebpageContentEntity -> content.contentId },
                            contentDescProvider = { content: WebpageContentEntity ->
                                val path = content.htmlContentPath
                                "Content: ${path.substringAfterLast('/')}"
                            },
                            onToggle = { viewModel.toggleWebpageContentSelection(it) },
                            onSetAllSelected = { viewModel.setAllWebpageContentSelected(it) },
                            // Disable selection controls if PCAP/AI is running
                            enabled = uiState.canStartAnalysis && uiState.totalWebpageContentCount > 0
                        )

                        // --- NEW: PCAP File Selection ---
                        if (uiState.isPcapIncludable) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            PcapSelectionItem(
                                uiState = uiState, // Pass the whole state
                                onToggle = { viewModel.togglePcapSelection(it) },
                                onRetry = { viewModel.retryPcapConversion() }
                            )
                        }
                        // --- End PCAP File Selection ---

                    }
                } // End AnimatedVisibility for Data Selection

                Spacer(Modifier.height(24.dp))

                // --- Action Buttons ---
                when {
                    initialLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center){
                            LoadingIndicator(message = stringResource(R.string.loading_session_data))
                        }
                    }
                    initialError -> {
                        ErrorComponent(
                            error = uiState.error ?: stringResource(id = R.string.unknown_error),
                            onRetryClick = { viewModel.loadSessionData() } // Retry loading data
                        )
                    }
                    else -> {
                        // Show Analyze buttons only when data is loaded successfully
                        Column (
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally // Center loading indicator
                        ){
                            // Loading overlay specifically for ongoing AI analysis or PCAP conversion
                            // Show this *above* the buttons when active
                            AnimatedVisibility(visible = uiState.isLoading || uiState.isPcapConverting) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            uiState.isPcapConverting -> "Converting PCAP..."
                                            uiState.isLoading -> "Analyzing with AI..."
                                            else -> "Loading..." // Generic fallback (shouldn't be reached here)
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // Button for Gemini Pro
                            RoundCornerButton(
                                onClick = {
                                    // Specify the exact model name you want to use
                                    viewModel.analyzeWithAi("gemini-2.5-pro-preview-03-25")
                                    navController.navigate(AutomaticAnalysisOutputRoute) // Navigate to output screen
                                },
                                buttonText = stringResource(R.string.analyze_pro),
                                enabled = isAnalyzeButtonEnabled,
                                isLoading = false,
                                fillWidth = true,
                            )

                            Spacer(Modifier.height(16.dp)) // Space between buttons

                            // Button for Gemini Flash
                            GhostButton(
                                onClick = {
                                    // Specify the exact model name you want to use
                                    viewModel.analyzeWithAi("gemini-2.0-flash")
                                    navController.navigate(AutomaticAnalysisOutputRoute) // Navigate to output screen
                                },
                                buttonText = stringResource(R.string.analyze_fast),
                                enabled = isAnalyzeButtonEnabled,
                                isLoading = false,
                                fillWidth = true,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp)) // More bottom padding
            } // End Main Column

            // --- Internet Banner ---
            AnimatedNoInternetBanner(isConnected = isConnected)
        } // End Box
    } // End Scaffold
}


//Previews - Remember to update these with PCAP states

@Preview(name = "Input - Loaded State", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisInputScreenPreview_Loaded_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    // Add a mock state that includes PCAP being available but idle
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded.copy(
        isPcapIncludable = true,
        pcapFilePath = "/fake/path.pcap",
        pcapProcessingState = PcapProcessingState.Idle
    ))

    AppTheme(darkTheme = false) { // Wrap with AppTheme (Light)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Input - PCAP Converting", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisInputScreenPreview_PcapConverting_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded.copy(
        isPcapIncludable = true,
        isPcapSelected = true, // Must be selected to be converting
        pcapFilePath = "/fake/path.pcap",
        pcapProcessingState = PcapProcessingState.Processing("job123") // Example converting state
    ))

    AppTheme(darkTheme = false) {
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Input - PCAP Error", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisInputScreenPreview_PcapError_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded.copy(
        isPcapIncludable = true,
        isPcapSelected = true, // Can be selected even if error occurred
        pcapFilePath = "/fake/path.pcap",
        pcapProcessingState = PcapProcessingState.Error("Upload failed: 400 Bad Request") // Example error state
    ))

    AppTheme(darkTheme = false) {
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

