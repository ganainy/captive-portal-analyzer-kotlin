package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.PcapProcessingState
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

// ---  Composable for PCAP Selection Item ---
@Composable
fun PcapSelectionItem(
    uiState: AutomaticAnalysisUiState,
    onToggle: (Boolean) -> Unit,
    onRetry: () -> Unit
) {
    val pcapState = uiState.pcapProcessingState
    // Checkbox enabled state:
    // - Always allow toggling OFF.
    // - Allow toggling ON only if not currently converting.
    val checkboxEnabled = !uiState.isPcapConverting || !uiState.isPcapSelected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = uiState.isPcapSelected,
            onCheckedChange = onToggle,
            enabled = checkboxEnabled // Use calculated enabled state
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) { // Column takes remaining space
            Text(
                text = stringResource(R.string.pcap_file_label),
                style = MaterialTheme.typography.bodyLarge
            )
            // --- Status Display ---
            AnimatedVisibility(visible = uiState.isPcapSelected || pcapState is PcapProcessingState.Error) { // Show status if selected or if there's an error
                when (pcapState) {
                    is PcapProcessingState.Idle -> {
                        if (uiState.isPcapSelected) {
                            Text("Ready to include", style = MaterialTheme.typography.bodySmall)
                        }
                        // If not selected & Idle, show nothing extra here
                    }
                    is PcapProcessingState.Uploading -> {
                        Column { // Progress bar and text
                            LinearProgressIndicator(modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .padding(top = 4.dp))
                            Text("Uploading...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is PcapProcessingState.Processing -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Processing on server...", style = MaterialTheme.typography.bodySmall)
                    }
                    is PcapProcessingState.Polling -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Checking status (Attempt ${pcapState.attempt})...", style = MaterialTheme.typography.bodySmall)
                    }
                    is PcapProcessingState.DownloadingJson -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Downloading result...", style = MaterialTheme.typography.bodySmall)
                    }
                    is PcapProcessingState.Success -> Text(
                        "Conversion successful, ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    is PcapProcessingState.Error -> Text(
                        "Error: ${pcapState.message.take(60)}${if (pcapState.message.length > 60) "..." else ""}", // Show truncated error
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            // Show hint if PCAP is not selected and not in error state
            if (!uiState.isPcapSelected && pcapState !is PcapProcessingState.Error) {
                Text(stringResource(R.string.app_will_convert_the_pcap_file_to_json_format_and_include_it_in_the_ai_prompt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            // --- End Status Display ---
        }

        // --- Retry Button for Error State ---
        // Show retry button only if there is an error
        AnimatedVisibility(visible = pcapState is PcapProcessingState.Error) {
            IconButton(onClick = onRetry, enabled = !uiState.isPcapConverting) { // Disable retry if a new conversion started
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.retry_pcap_conversion), // Add string resource
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        // --- End Retry Button ---
    }
}

// --- Previews for PcapSelectionItem ---

@Preview(name = "PCAP Item - Idle Not Selected", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_IdleNotSelected() {
    AppTheme { // Use your app's theme
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true, // Assume PCAP is available for the item to show
            isPcapSelected = false,
            pcapProcessingState = PcapProcessingState.Idle
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Idle Selected", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_IdleSelected() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Idle
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Uploading", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_Uploading() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Uploading(0.4f)
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Processing", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_Processing() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Processing("job-id-123")
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Polling", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_Polling() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Polling("job-id-456", 5)
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Downloading", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_Downloading() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.DownloadingJson("job-id-789", "http://example.com/result.json")
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Success", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_Success() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Success("job-id-abc", "[{...}]") // JSON content doesn't matter for preview appearance
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Error Selected", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_ErrorSelected() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true, // Selected when error occurred
            pcapProcessingState = PcapProcessingState.Error("Conversion timed out after 30 attempts. Please check server status.", "job-err-1")
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Error Not Selected", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_ErrorNotSelected() {
    AppTheme {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = false, // Not selected, but still showing the error state
            pcapProcessingState = PcapProcessingState.Error("Server returned 503 Service Unavailable.", "job-err-2")
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}


// --- Dark Mode Examples ---

@Preview(name = "PCAP Item - Idle Selected (Dark)", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PcapSelectionItemPreview_IdleSelected_Dark() {
    AppTheme(darkTheme = true) { // Ensure your AppTheme respects darkTheme parameter
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Idle
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}

@Preview(name = "PCAP Item - Error Selected (Dark)", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PcapSelectionItemPreview_ErrorSelected_Dark() {
    AppTheme(darkTheme = true) {
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true,
            isPcapSelected = true,
            pcapProcessingState = PcapProcessingState.Error("Conversion timed out.", "job-err-1")
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}