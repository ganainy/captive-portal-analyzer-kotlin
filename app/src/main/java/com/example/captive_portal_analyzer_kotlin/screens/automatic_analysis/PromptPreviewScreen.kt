package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.IMainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.PcapProcessingState
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptPreviewScreen(
    navController: NavController,
    viewModel: IAutomaticAnalysisViewModel, // Shared ViewModel
    mainViewModel: IMainViewModel
) {
    val uiState by viewModel.automaticAnalysisUiState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()

    // State for managing scroll position
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // State to determine if the scroll-to-top button should be shown
    // Show button if scrolled down more than 200 dp
    val showScrollToTopButton by remember {
        derivedStateOf {
            scrollState.value > with(density) { 200.dp.toPx() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prompt_preview_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box( // Use Box to stack content and overlay the FAB and Banner
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding to the Box
        ) {
            // --- Main Scrollable Content ---
            Column(
                modifier = Modifier
                    .fillMaxSize() // Fill the Box
                    .verticalScroll(scrollState) // Make THIS column scrollable
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Apply inner padding for content
            ) {
                // --- Data Inclusion Status Cards ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between cards
                ) {
                    DataItemStatusCard(
                        label = stringResource(R.string.network_requests_label),
                        icon = ImageVector.vectorResource(R.drawable.http_24px),
                        isIncluded = uiState.selectedRequestIds.isNotEmpty(),
                        statusText = if (uiState.selectedRequestIds.isNotEmpty()) {
                            stringResource(
                                R.string.included_count,
                                uiState.selectedRequestIds.size
                            )
                        } else {
                            stringResource(R.string.not_included)
                        }
                    )
                    DataItemStatusCard(
                        label = stringResource(R.string.screenshots_label),
                        icon = ImageVector.vectorResource(R.drawable.image_24px),
                        isIncluded = uiState.selectedScreenshotIds.isNotEmpty(),
                        statusText = if (uiState.selectedScreenshotIds.isNotEmpty()) {
                            stringResource(
                                R.string.included_count,
                                uiState.selectedScreenshotIds.size
                            )
                        } else {
                            stringResource(R.string.not_included)
                        }
                    )
                    DataItemStatusCard(
                        label = stringResource(R.string.webpage_content_label),
                        icon = ImageVector.vectorResource(R.drawable.description_24px),
                        isIncluded = uiState.selectedWebpageContentIds.isNotEmpty(),
                        statusText = if (uiState.selectedWebpageContentIds.isNotEmpty()) {
                            stringResource(
                                R.string.included_count,
                                uiState.selectedWebpageContentIds.size
                            )
                        } else {
                            stringResource(R.string.not_included)
                        }
                    )
                    // PCAP card with more detailed status - assuming it fits here
                    PcapStatusCard(uiState = uiState)
                }

                Spacer(modifier = Modifier.height(16.dp)) // Space between cards and prompt preview

                // --- Prompt Preview Area ---
                // This Box is now just content within the scrollable column
                Box(
                    modifier = Modifier.fillMaxWidth(), // Removed weight(1f)
                    contentAlignment = Alignment.Center // For loading indicator
                ) {
                    if (uiState.isGeneratingPreview) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator() // Main loading indicator
                            Text(
                                stringResource(R.string.generating_prompt_preview),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        // Show generated prompt or error message
                        Column( // This inner Column no longer needs verticalScroll
                            modifier = Modifier.fillMaxSize() // Fill the remaining space *in this box*
                        ) {
                            if (!uiState.promptPreview.isNullOrBlank()) {
                                // Use MarkdownText for better formatting if prompt contains markdown
                                MarkdownText(
                                    markdown = uiState.promptPreview!!, // Display the generated preview
                                    style = MaterialTheme.typography.bodyMedium // Use appropriate style
                                )
                            } else {
                                // Show message if preview is empty or generation failed
                                Text(
                                    stringResource(R.string.prompt_preview_empty_or_error),
                                    style = MaterialTheme.typography.bodyLarge, // Make it slightly larger
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 32.dp) // Add some padding from the top
                                )
                            }
                        }
                    }
                }
                // No Spacer needed at the very end of the scrollable content
            }

            // --- Scroll to Top Button (Anchored to bottom-end of Box) ---
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn() + slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // Adjust padding to be above the potential internet banner
                    .padding(bottom = 80.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0) // Scroll back to the top (position 0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer, // Example color
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                  Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_up_24px),
                        contentDescription = stringResource(R.string.scroll_to_top) // Needs to be defined
                    )
                }
            }

            // --- Internet Banner (Anchored to bottom-center of Box) ---
            AnimatedNoInternetBanner(
                isConnected = isConnected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Trigger prompt generation when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.generatePromptForPreview()
    }
}

@Composable
fun DataItemStatusCard(
    label: String,
    icon: ImageVector,
    isIncluded: Boolean,
    statusText: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isIncluded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Push text to ends
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isIncluded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isIncluded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isIncluded) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}


@Composable
fun PcapStatusCard(uiState: AutomaticAnalysisUiState) {
    val pcapStatusText = when {
        !uiState.isPcapIncludable -> stringResource(R.string.pcap_not_available)
        !uiState.isPcapSelected -> stringResource(R.string.not_selected)
        uiState.isPcapConverting -> stringResource(R.string.pcap_converting) // Show converting state
        uiState.pcapProcessingState is PcapProcessingState.Success -> {
            stringResource(R.string.included_ready) + if (uiState.pcapProcessingState.wasTruncated) stringResource(R.string.pcap_truncated_note) else ""
        }
        uiState.pcapProcessingState is PcapProcessingState.Error -> stringResource(R.string.pcap_conversion_failed)
        uiState.pcapProcessingState is PcapProcessingState.Idle -> stringResource(R.string.selected_not_converted) // Selected but not started/completed
        else -> stringResource(R.string.pcap_unknown_status) // Should not happen often
    }

    val isIncluded = uiState.isPcapSelected && uiState.pcapProcessingState is PcapProcessingState.Success
    val isConverting = uiState.isPcapConverting

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isIncluded -> MaterialTheme.colorScheme.primaryContainer
                isConverting -> MaterialTheme.colorScheme.secondaryContainer // Indicate active state
                uiState.isPcapSelected -> MaterialTheme.colorScheme.surfaceContainer // Selected but not included/error
                else -> MaterialTheme.colorScheme.surfaceVariant // Not selected or available
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Push text to ends
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector =  ImageVector.vectorResource(R.drawable.pcapdroid), // PCAP icon
                        contentDescription = stringResource(R.string.pcap_network_summary_label),
                        tint = when {
                            isIncluded -> MaterialTheme.colorScheme.onPrimaryContainer
                            isConverting -> MaterialTheme.colorScheme.onSecondaryContainer
                            uiState.isPcapSelected -> MaterialTheme.colorScheme.onSurfaceVariant // Or onSurfaceContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.pcap_network_summary_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isIncluded -> MaterialTheme.colorScheme.onPrimaryContainer
                            isConverting -> MaterialTheme.colorScheme.onSecondaryContainer
                            uiState.isPcapSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Text(
                    text = pcapStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isIncluded -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        isConverting -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        uiState.isPcapSelected -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                )
            }
            // Show a progress bar if PCAP is converting
            if (isConverting) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


// --- Previews ---

// Update previews to include the cards

@Preview(name = "Preview - Loading", showBackground = true)
@Composable
fun PromptPreviewScreenPreview_Loading() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            isGeneratingPreview = true, // Simulate loading state
            promptPreview = null,
            // Simulate selections for card preview
            selectedRequestIds = setOf(1, 2),
            selectedScreenshotIds = setOf(1),
            selectedWebpageContentIds = emptySet(),
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Polling("job1", 5) // Simulate PCAP converting
        )
    )

    AppTheme {
        PromptPreviewScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}


@Preview(name = "Preview - Success", showBackground = true)
@Composable
fun PromptPreviewScreenPreview_Success() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            isGeneratingPreview = false,
            promptPreview = """
             You are a privacy and network security analyst... (rest of base prompt)

             --- Analysis Context ---
             Model Used: 'xx'

             - Network Requests (2 selected, truncated if necessary):
             url:https://example.com/resource1, type:GET, method:GET, body:N/A, headers:N/A
             url:https://example.com/resource2?param=value, type:GET, method:GET, body:N/A, headers:N/A

             - Relevant Screenshots: Included as images.

             - Webpage Content: None selected.

             - PCAP Network Summary (JSON):
             {"summary":"...truncated json data..."}
             ... (PCAP JSON truncated due to length limit)
             --- End Analysis Context ---
             """.trimIndent(),
            // Simulate selections for card preview
            selectedRequestIds = setOf(1, 2),
            selectedScreenshotIds = setOf(1),
            selectedWebpageContentIds = emptySet(),
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Success("job1", "{\"summary\":\"...truncated json data...\"}", wasTruncated = true) // Simulate successful truncated PCAP
        )
    )

    AppTheme {
        PromptPreviewScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Preview - Error/Empty Prompt", showBackground = true)
@Composable
fun PromptPreviewScreenPreview_ErrorOrEmpty() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            isGeneratingPreview = false, // Finished loading
            promptPreview = "[Error generating prompt preview: Session data not loaded]", // Example error or null
            // Simulate selections for card preview (none selected, PCAP error)
            selectedRequestIds = emptySet(),
            selectedScreenshotIds = emptySet(),
            selectedWebpageContentIds = emptySet(),
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Error("Upload failed") // Simulate PCAP failure
        )
    )

    AppTheme {
        PromptPreviewScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}

@Preview(name = "Preview - Success Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PromptPreviewScreenPreview_Success_Dark() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            isGeneratingPreview = false,
            promptPreview = """
             You are a privacy and network security analyst... (rest of base prompt)

             --- Analysis Context ---
             Model Used: 'gemini-1.5-pro-preview-0409'

             - Network Requests (1 selected, truncated if necessary):
             url:https://example.com/resource1, type:GET, method:GET, body:N/A, headers:N/A

             - Relevant Screenshots: None selected.
             - Webpage Content: (5 selected): Included separately.
             - PCAP Network Summary: Not selected.
             --- End Analysis Context ---
             """.trimIndent(),
            // Simulate selections for card preview
            selectedRequestIds = setOf(1),
            selectedScreenshotIds = emptySet(),
            selectedWebpageContentIds = setOf(1,2,3,4,5),
            isPcapIncludable = true,
            isPcapSelected = false, // PCAP not selected
            pcapProcessingState = PcapProcessingState.Idle // PCAP idle state
        )
    )

    AppTheme(darkTheme = true) {
        PromptPreviewScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}