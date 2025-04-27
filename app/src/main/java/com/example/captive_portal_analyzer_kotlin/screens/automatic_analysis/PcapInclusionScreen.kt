package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.IMainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.navigation.AutomaticAnalysisOutputRoute
import com.example.captive_portal_analyzer_kotlin.navigation.AutomaticAnalysisPromptPreviewRoute
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components.PcapSelectionItem
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.PcapProcessingState
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.mockUiStateInputLoaded
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme


private const val LATEST_GEMINI_PRO_MODEL = "gemini-2.5-pro-preview-03-25"
private const val LATEST_GEMINI_FLASH_MODEL = "gemini-2.0-flash"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcapInclusionScreen(
    navController: NavController,
    viewModel: IAutomaticAnalysisViewModel,
    mainViewModel: IMainViewModel
) {
    val uiState by viewModel.automaticAnalysisUiState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()

    // Determine if Analyze buttons should be enabled
    // Analysis can start if AI isn't already loading AND (PCAP not selected OR PCAP is idle/success/error)
    val isAnalyzeButtonEnabled = uiState.canStartAnalysis

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pcap_inclusion_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) { // Navigate back to Input screen
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // --- PCAP Inclusion Section ---
                if (uiState.isPcapIncludable) {
                    Text(
                        text = stringResource(R.string.include_pcap_question),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(modifier = Modifier.padding(bottom = 8.dp))
                    PcapSelectionItem(
                        uiState = uiState,
                        onToggle = { viewModel.togglePcapSelection(it) },
                        onRetry = { viewModel.retryPcapConversion() }
                    )
                    Spacer(Modifier.height(24.dp)) // Space before Analyze buttons
                } else {
                    // Case where PCAP file doesn't exist for the session
                    Text(
                        text = stringResource(R.string.pcap_not_available),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp)) // Space before Analyze buttons
                }
                // --- End PCAP Inclusion Section ---


                // --- Action Buttons ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Loading overlay specifically for ongoing AI analysis or PCAP conversion
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
                                    uiState.isPcapConverting -> stringResource(R.string.converting_pcap)
                                    uiState.isLoading -> stringResource(R.string.analyzing_with_ai)
                                    else -> stringResource(R.string.loading) // Generic fallback
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Show Full Prompt Button ---
                    AnimatedVisibility(visible = isAnalyzeButtonEnabled) {

                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.you_can_check_the_prompt_that_will_be_sent_to_the_model_by_clicking))
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(stringResource(R.string.here))
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                navController.navigate(AutomaticAnalysisPromptPreviewRoute) // Navigate to the preview screen
                            },
                    )
                    }

                    // --- End Show Full Prompt Button ---

                    Spacer(Modifier.height(32.dp))

                    // --- Analyze Buttons ---
                    // Button for Gemini Pro
                    RoundCornerButton(
                        onClick = {
                            viewModel.analyzeWithAi(LATEST_GEMINI_PRO_MODEL)
                            navController.navigate(AutomaticAnalysisOutputRoute) {
                                // Optional: Pop up to the route before input screen if desired
                                // popUpTo(Graph.AUTOMATIC_ANALYSIS) { inclusive = true }
                            }
                        },
                        buttonText = stringResource(R.string.analyze_pro),
                        enabled = isAnalyzeButtonEnabled, // Enable based on overall state
                        isLoading = false, // Button itself doesn't show loading, handled by overlay
                        fillWidth = true,
                    )

                    Spacer(Modifier.height(16.dp)) // Space between buttons

                    // Button for Gemini Flash
                    GhostButton(
                        onClick = {
                            viewModel.analyzeWithAi(LATEST_GEMINI_FLASH_MODEL)
                            navController.navigate(AutomaticAnalysisOutputRoute) {
                                // Optional: Pop up to the route before input screen if desired
                                // popUpTo(Graph.AUTOMATIC_ANALYSIS) { inclusive = true }
                            }
                        },
                        buttonText = stringResource(R.string.analyze_fast),
                        enabled = isAnalyzeButtonEnabled, // Enable based on overall state
                        isLoading = false, // Button itself doesn't show loading, handled by overlay
                        fillWidth = true,
                    )
                    // --- End Analyze Buttons ---
                }


                Spacer(Modifier.height(32.dp)) // Bottom padding
            } // End Main Column

            // --- Internet Banner ---
            AnimatedNoInternetBanner(
                isConnected = isConnected,
                modifier = Modifier.align(Alignment.BottomCenter) // Anchor to bottom
            )
        } // End Box
    } // End Scaffold
}


// --- Previews for PcapInclusionScreen ---

@Preview(name = "PCAP Screen - Available, Idle", showBackground = true, widthDp = 380)
@Composable
fun PcapInclusionScreenPreview_AvailableIdle() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        mockUiStateInputLoaded.copy(
            isPcapIncludable = true,
            pcapFilePath = "/fake/path.pcap",
            isPcapSelected = false,
            pcapProcessingState = PcapProcessingState.Idle
        )
    )
    AppTheme {
        PcapInclusionScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(
    name = "PCAP Screen - Available, Selected, Converting",
    showBackground = true,
    widthDp = 380
)
@Composable
fun PcapInclusionScreenPreview_Converting() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        mockUiStateInputLoaded.copy(
            isPcapIncludable = true,
            pcapFilePath = "/fake/path.pcap",
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Processing("job123")
        )
    )
    AppTheme {
        PcapInclusionScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(
    name = "PCAP Screen - Available, Selected, Error",
    showBackground = true,
    widthDp = 380
)
@Composable
fun PcapInclusionScreenPreview_Error() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        mockUiStateInputLoaded.copy(
            isPcapIncludable = true,
            pcapFilePath = "/fake/path.pcap",
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Error("Upload failed")
        )
    )
    AppTheme {
        PcapInclusionScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "PCAP Screen - Not Available", showBackground = true, widthDp = 380)
@Composable
fun PcapInclusionScreenPreview_NotAvailable() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        mockUiStateInputLoaded.copy(
            isPcapIncludable = false, // Set PCAP as not available
            pcapFilePath = null
        )
    )
    AppTheme {
        PcapInclusionScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}


@Preview(
    name = "PCAP Screen - Available, Success (Dark)",
    showBackground = true,
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PcapInclusionScreenPreview_SuccessDark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        mockUiStateInputLoaded.copy(
            isPcapIncludable = true,
            pcapFilePath = "/fake/path.pcap",
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Success("jobabc", "{}",true)
        )
    )
    AppTheme(darkTheme = true) {
        PcapInclusionScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}