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
                    .verticalScroll(rememberScrollState())
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
                    enabled = !initialLoading // Disable if initially loading
                )

                Spacer(Modifier.height(16.dp))

                // --- Toggle Data Selection Visibility ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !initialLoading) { isDataSelectionExpanded = !isDataSelectionExpanded }
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
                                request.url?.length?.let { "${request.method} ${request.url.take(60)}${if (it > 60) "..." else ""}" }
                                    .toString()
                            },
                            onToggle = { viewModel.toggleRequestSelection(it) },
                            onSetAllSelected = { viewModel.setAllRequestsSelected(it) },
                            enabled = !initialLoading && uiState.totalRequestsCount > 0
                        )
                        // Screenshots Category
                        DataSelectionCategory(
                            title = stringResource(
                                R.string.screenshots_selected,
                                uiState.selectedScreenshotIds.size,
                                uiState.totalScreenshotsCount
                            ),
                            items = uiState.sessionData?.screenshots?.filter { it.isPrivacyOrTosRelated } ?: emptyList(),
                            selectedIds = uiState.selectedScreenshotIds,
                            idProvider = { screenshot: ScreenshotEntity -> screenshot.screenshotId },
                            contentDescProvider = { screenshot: ScreenshotEntity ->
                                "Screenshot: ${screenshot.path.substringAfterLast('/')}"
                            },
                            onToggle = { viewModel.toggleScreenshotSelection(it) },
                            onSetAllSelected = { viewModel.setAllScreenshotsSelected(it) },
                            enabled = !initialLoading && uiState.totalScreenshotsCount > 0
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
                            enabled = !initialLoading && uiState.totalWebpageContentCount > 0
                        )
                    }
                } // End AnimatedVisibility for Data Selection

                Spacer(Modifier.height(16.dp))

                // --- Action Button ---
                // Show initial loading or error if applicable
                when {
                    initialLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center){
                            LoadingIndicator(message = stringResource(R.string.loading_session_data)) // New string
                        }
                    }
                    initialError -> {
                        ErrorComponent(
                            error = uiState.error ?: stringResource(id = R.string.unknown_error), // Provide default
                            onRetryClick = { viewModel.loadSessionData() } // Retry loading data
                        )
                    }
                    else -> {
                        // Show Analyze button only when data is loaded successfully
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ){
                            RoundCornerButton(
                                onClick = {
                                    viewModel.analyzeWithAi() // Start analysis
                                    navController.navigate(AutomaticAnalysisOutputRoute) // Navigate to output screen
                                },
                                buttonText = stringResource(R.string.analyze_captive_portal_with_ai),
                                // Enable button only if not loading/analyzing and some data is potentially selectable
                                enabled = !uiState.isLoading && (uiState.totalRequestsCount > 0 || uiState.totalScreenshotsCount > 0 || uiState.totalWebpageContentCount > 0),
                                isLoading = false, // Loading state is handled on the output screen for analysis
                                fillWidth = false,
                            )
                        }
                    }
                }


                Spacer(Modifier.height(16.dp)) // Bottom padding
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