package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewAutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related.PreviewMainViewModel
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptPreviewScreen(
    navController: NavController,
    viewModel: IAutomaticAnalysisViewModel, // Shared ViewModel
    mainViewModel: IMainViewModel
) {
    val uiState by viewModel.automaticAnalysisUiState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()



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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Main content area
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Show loading indicator while generating
                if (uiState.isGeneratingPreview) {
                    Box(
                        modifier = Modifier.fillMaxSize(), // Fill available space
                        contentAlignment = Alignment.Center
                    ) {
                        // Use LoadingIndicator component or build a custom one
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.generating_prompt_preview), style = MaterialTheme.typography.bodyMedium)
                        }
                        // Or use your custom component:
                        // LoadingIndicator(message = stringResource(R.string.generating_prompt_preview))
                    }
                } else {
                    // Show generated prompt or error message in a scrollable column
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
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
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(64.dp)) // Add padding at the bottom for banner space
                    }
                }
            }

            // --- Internet Banner (Anchored to bottom of Box) ---
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

// --- Previews ---

@Preview(name = "Preview - Loading", showBackground = true)
@Composable
fun PromptPreviewScreenPreview_Loading() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            isGeneratingPreview = true, // Simulate loading state
            promptPreview = null
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

             - Relevant Screenshots: None selected.

             - Webpage Content: None selected.

             - PCAP Network Summary: Not included (PCAP conversion failed: Upload failed)
             --- End Analysis Context ---
             """.trimIndent()
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

@Preview(name = "Preview - Error/Empty", showBackground = true)
@Composable
fun PromptPreviewScreenPreview_ErrorOrEmpty() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            isGeneratingPreview = false, // Finished loading
            promptPreview = "[Error generating prompt preview: Session data not loaded]" // Example error or null
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

             - Network Requests (2 selected, truncated if necessary):
             url:https://example.com/resource1, type:GET, method:GET, body:N/A, headers:N/A

             - Relevant Screenshots: None selected.
             - Webpage Content: None selected.
             - PCAP Network Summary: Not selected.
             --- End Analysis Context ---
             """.trimIndent()
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
