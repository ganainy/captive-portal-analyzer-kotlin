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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.captive_portal_analyzer_kotlin.navigation.PcapInclusionRoute
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components.DataSelectionCategory
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

    // Determine if any non-PCAP data is *selectable*
    val isAnyBaseDataSelectable = uiState.totalRequestsCount > 0 ||
            uiState.totalScreenshotsCount > 0 ||
            uiState.totalWebpageContentCount > 0

    // Determine if at least one piece of data is *selected*
    val isAnyDataSelected = uiState.selectedRequestIds.isNotEmpty() ||
            uiState.selectedScreenshotIds.isNotEmpty() ||
            uiState.selectedWebpageContentIds.isNotEmpty()

    // Determine if "Proceed" button should be enabled
    // Needs data loaded, no initial error, and at least one item selected
    val isProceedButtonEnabled = !initialLoading && !initialError && isAnyDataSelected

    // Determine if the prompt clear button should be enabled
    val isPromptClearEnabled = !uiState.inputText.isEmpty() && !initialLoading && !initialError

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.automatic_analysis_input_screen_title)) })
        }
    ) { paddingValues ->
        // Use a Box to layer the NoInternetBanner over the content + button
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply scaffold padding here
        ) {
            // Main column for content distribution (scrollable part + fixed button part)
            Column(modifier = Modifier.fillMaxSize()) {

                // --- Scrollable Content Area ---
                Column(
                    modifier = Modifier
                        .weight(1f) // Takes up available space, pushing button area down
                        .padding(horizontal = 16.dp) // Horizontal padding for content
                        .verticalScroll(rememberScrollState()) // Make THIS column scrollable
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
                        enabled = !initialLoading,
                        trailingIcon = {
                            if (isPromptClearEnabled) {
                                IconButton(
                                    onClick = { viewModel.updatePromptEditText("") },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear_prompt)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // --- Toggle Data Selection Visibility ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        visible = isDataSelectionExpanded && !initialLoading && !initialError,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            // Network Requests Category
                            DataSelectionCategory(
                                title = stringResource(R.string.network_requests_selected, uiState.selectedRequestIds.size, uiState.totalRequestsCount),
                                items = uiState.sessionData?.requests ?: emptyList(),
                                selectedIds = uiState.selectedRequestIds,
                                idProvider = { it.customWebViewRequestId },
                                contentDescProvider = {
                                    val urlPart = it.url?.take(60)?.let { url -> if (it.url.length > 60) "$url..." else url } ?: "N/A"
                                    "${it.method} $urlPart"
                                },
                                onToggle = { viewModel.toggleRequestSelection(it) },
                                onSetAllSelected = { viewModel.setAllRequestsSelected(it) },
                                enabled = !initialLoading && uiState.totalRequestsCount > 0
                            )
                            // Screenshots Category
                            DataSelectionCategory(
                                title = stringResource(R.string.screenshots_selected, uiState.selectedScreenshotIds.size, uiState.totalScreenshotsCount),
                                items = uiState.sessionData?.screenshots?.filter { it.isPrivacyOrTosRelated } ?: emptyList(),
                                selectedIds = uiState.selectedScreenshotIds,
                                idProvider = { it.screenshotId },
                                contentDescProvider = { "Screenshot: ${it.path.substringAfterLast('/')}" },
                                onToggle = { viewModel.toggleScreenshotSelection(it) },
                                onSetAllSelected = { viewModel.setAllScreenshotsSelected(it) },
                                enabled = !initialLoading && uiState.totalScreenshotsCount > 0
                            )
                            // Webpage Content Category
                            DataSelectionCategory(
                                title = stringResource(R.string.webpage_content_selected, uiState.selectedWebpageContentIds.size, uiState.totalWebpageContentCount),
                                items = uiState.sessionData?.webpageContent ?: emptyList(),
                                selectedIds = uiState.selectedWebpageContentIds,
                                idProvider = { it.contentId },
                                contentDescProvider = { "Content: ${it.htmlContentPath.substringAfterLast('/')}" },
                                onToggle = { viewModel.toggleWebpageContentSelection(it) },
                                onSetAllSelected = { viewModel.setAllWebpageContentSelected(it) },
                                enabled = !initialLoading && uiState.totalWebpageContentCount > 0
                            )
                        }
                    } // End AnimatedVisibility for Data Selection

                    Spacer(Modifier.height(16.dp))

                } // --- End Scrollable Content Area ---


                // --- Fixed Bottom Action Area ---
                Surface( // Add a surface for potential background color/elevation
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp), // Padding for the button area
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            initialLoading -> {
                                LoadingIndicator(message = stringResource(R.string.loading_session_data))
                            }
                            initialError -> {
                                ErrorComponent(
                                    error = uiState.error ?: stringResource(id = R.string.unknown_error),
                                    onRetryClick = { viewModel.loadSessionData() }
                                )
                            }
                            else -> {
                                // Proceed Button
                                RoundCornerButton(
                                    onClick = {
                                        navController.navigate(PcapInclusionRoute)
                                    },
                                    buttonText = stringResource(R.string.proceed_to_pcap_step),
                                    enabled = isProceedButtonEnabled,
                                    isLoading = false,
                                    fillWidth = true,
                                )
                            }
                        }
                    }
                } // --- End Fixed Bottom Action Area ---

            } // End Main Column for distribution

            // --- Internet Banner (Aligns to bottom of the Box) ---
            AnimatedNoInternetBanner(
                isConnected = isConnected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } // End Box
    } // End Scaffold
}


//Previews

@Preview(name = "Input - Loaded State", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AutomaticAnalysisInputScreenPreview_Loaded_Light() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    // Mock state without PCAP elements for this screen's preview
    val mockViewModel = PreviewAutomaticAnalysisViewModel(mockUiStateInputLoaded)

    AppTheme(darkTheme = false) { // Wrap with AppTheme (Light)
        AutomaticAnalysisInputScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}