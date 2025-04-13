package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components.DataSelectionCategory
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateInputError
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateInputLoaded
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateInputLoadedEmpty
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateInputLoading
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
    var isDataSelectionExpanded by remember { mutableStateOf(true) } // State for collapsing data section

    // Observe initial loading state (for session data)
    val initialLoading = uiState.isLoading && uiState.sessionData == null && uiState.outputText == null
    val initialError = uiState.error != null && uiState.sessionData == null // Error during initial load
    val isAnalyzing = uiState.isLoading && uiState.outputText != null // Loading *during* analysis stream

    // Determine if any data is selectable (used for enabling buttons)
    val isAnyDataSelectable = uiState.totalRequestsCount > 0 || uiState.totalScreenshotsCount > 0 || uiState.totalWebpageContentCount > 0
    // Determine if any data is actually *selected* (could be used for stricter button enabling)
    val isAnyDataSelected = uiState.selectedRequestIds.isNotEmpty() || uiState.selectedScreenshotIds.isNotEmpty() || uiState.selectedWebpageContentIds.isNotEmpty()

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
                    maxLines = 5,
                    enabled = !initialLoading && !isAnalyzing // Disable if initially loading or currently analyzing
                )

                Spacer(Modifier.height(16.dp))

                // --- Toggle Data Selection Visibility ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !initialLoading && !isAnalyzing) { isDataSelectionExpanded = !isDataSelectionExpanded }
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
                    visible = isDataSelectionExpanded && !initialLoading && !initialError, // Hide if loading/error
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
                                "${request.method ?: "UNK"} $urlPart"
                            },
                            onToggle = { viewModel.toggleRequestSelection(it) },
                            onSetAllSelected = { viewModel.setAllRequestsSelected(it) },
                            enabled = !initialLoading && !isAnalyzing && uiState.totalRequestsCount > 0 // Disable during analysis too
                        )
                        // Screenshots Category
                        DataSelectionCategory(
                            title = stringResource(
                                R.string.screenshots_selected,
                                uiState.selectedScreenshotIds.size,
                                uiState.totalScreenshotsCount // Total is only privacy/tos related ones
                            ),
                            items = uiState.sessionData?.screenshots?.filter { it.isPrivacyOrTosRelated } ?: emptyList(), // Show only relevant ones
                            selectedIds = uiState.selectedScreenshotIds,
                            idProvider = { screenshot: ScreenshotEntity -> screenshot.screenshotId },
                            contentDescProvider = { screenshot: ScreenshotEntity ->
                                "Screenshot: ${screenshot.path.substringAfterLast('/')}"
                            },
                            onToggle = { viewModel.toggleScreenshotSelection(it) },
                            onSetAllSelected = { viewModel.setAllScreenshotsSelected(it) },
                            enabled = !initialLoading && !isAnalyzing && uiState.totalScreenshotsCount > 0 // Disable during analysis too
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
                                val path = content.htmlContentPath ?: content.jsContentPath ?: "Unknown content"
                                "Content: ${path.substringAfterLast('/')}"
                            },
                            onToggle = { viewModel.toggleWebpageContentSelection(it) },
                            onSetAllSelected = { viewModel.setAllWebpageContentSelected(it) },
                            enabled = !initialLoading && !isAnalyzing && uiState.totalWebpageContentCount > 0 // Disable during analysis too
                        )
                    }
                } // End AnimatedVisibility for Data Selection

                Spacer(Modifier.height(24.dp)) // Increased spacing before buttons

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
                        ){
                            val buttonEnabled = !isAnalyzing && isAnyDataSelectable // Enable if not analyzing AND there's something to potentially select

                            // Button for Gemini Pro
                            RoundCornerButton(
                                onClick = {
                                    viewModel.analyzeWithAi("gemini-2.5-pro-preview-03-25") // Use specific model name for pro
                                    navController.navigate(AutomaticAnalysisOutputRoute) // Navigate to output screen
                                },
                                buttonText = stringResource(R.string.analyze_pro),
                                enabled = buttonEnabled,
                                isLoading = false, // Loading state is handled on the output screen for analysis
                                fillWidth = true,
                            )

                            Spacer(Modifier.width(8.dp).height(16.dp)) // Space between buttons

                            // Button for Gemini Flash
                            GhostButton(
                                onClick = {
                                    viewModel.analyzeWithAi("gemini-2.0-flash") // Use specific model name for flash
                                    navController.navigate(AutomaticAnalysisOutputRoute) // Navigate to output screen
                                },
                                buttonText = stringResource(R.string.analyze_fast),
                                enabled = buttonEnabled,
                                isLoading = false, // Loading state is handled on the output screen for analysis
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



//Previews


@Preview(name = "Input - Loading State", showBackground = true)
// No uiMode specified, defaults to light

@Composable
fun AutomaticAnalysisInputScreenPreview_Loading_Light() { // Renamed for clarity
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoading)

    // Use AppTheme, explicitly set darkTheme = false for light mode preview
    AppTheme(darkTheme = false) {
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Input - Loading State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisInputScreenPreview_Loading_Dark() { // Renamed for clarity
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoading)

    // Use AppTheme, explicitly set darkTheme = true for dark mode preview
    AppTheme(darkTheme = true) {
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

// --- Apply the same pattern to other previews ---

@Preview(name = "Input - Error State", showBackground = true)
@Composable
fun AutomaticAnalysisInputScreenPreview_Error_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputError)

    AppTheme(darkTheme = false) { // Wrap with AppTheme (Light)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Input - Error State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisInputScreenPreview_Error_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputError)

    AppTheme(darkTheme = true) { // Wrap with AppTheme (Dark)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}


@Preview(name = "Input - Loaded State", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisInputScreenPreview_Loaded_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded)

    AppTheme(darkTheme = false) { // Wrap with AppTheme (Light)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Input - Loaded State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisInputScreenPreview_Loaded_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded)

    AppTheme(darkTheme = true) { // Wrap with AppTheme (Dark)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

// Example for Tablet Preview (assuming light mode default)
@Preview(name = "Input - Loaded State (Tablet)", showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun AutomaticAnalysisInputScreenPreview_Loaded_Tablet() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded)

    AppTheme(darkTheme = false) { // Wrap with AppTheme (Light)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
    // If you want a dark tablet preview, add another @Preview with uiMode and darkTheme=true
}


@Preview(name = "Input - Loaded Empty State", showBackground = true)
@Composable
fun AutomaticAnalysisInputScreenPreview_LoadedEmpty_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoadedEmpty)

    AppTheme(darkTheme = false) { // Wrap with AppTheme (Light)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

// Add a dark version if needed
@Preview(name = "Input - Loaded Empty State (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutomaticAnalysisInputScreenPreview_LoadedEmpty_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoadedEmpty)

    AppTheme(darkTheme = true) { // Wrap with AppTheme (Dark)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}