package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.promptPreview != null) {
                    // Use MarkdownText if your prompt contains markdown, otherwise use Text
                    MarkdownText(
                        markdown = uiState.promptPreview!!, // Display the generated preview
                        style = MaterialTheme.typography.bodyMedium // Use appropriate style
                    )
                    // Alternatively, for plain text:
                    // Text(
                    //    text = uiState.promptPreview!!,
                    //    style = MaterialTheme.typography.bodyMedium
                    // )
                } else {
                    Text(stringResource(R.string.prompt_not_generated))
                }
            }
            // --- Internet Banner ---
            AnimatedNoInternetBanner(isConnected = isConnected)
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun PromptPreviewScreenPreview() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(
            promptPreview = """
             You are a privacy and network security analyst... (rest of base prompt)

             --- Analysis Context ---
             Model Used: 'gemini-1.5-pro-preview-0409'

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

@Preview(showBackground = true)
@Composable
fun PromptPreviewScreenPreview_NotGenerated() {
    val mockNavController = rememberNavController()
    val mockMainViewModel = PreviewMainViewModel()
    val mockViewModel = PreviewAutomaticAnalysisViewModel(
        initialState = AutomaticAnalysisUiState(promptPreview = null) // Null preview
    )

    AppTheme {
        PromptPreviewScreen(
            navController = mockNavController,
            viewModel = mockViewModel,
            mainViewModel = mockMainViewModel
        )
    }
}