package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models.PcapProcessingState
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

@Composable
fun PcapSelectionItem(
    uiState: AutomaticAnalysisUiState,
    onToggle: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier // Added modifier parameter
) {
    val pcapState = uiState.pcapProcessingState
    // Checkbox enabled state: Always enabled unless actively uploading/processing/polling/downloading
    val checkboxEnabled = !uiState.isPcapConverting

    Row(
        modifier = modifier // Apply the modifier to the root Row
            .fillMaxWidth()
            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = uiState.isPcapSelected,
            onCheckedChange = onToggle,
            enabled = checkboxEnabled // Use calculated enabled state
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) { // Column takes remaining space
            Text(
                text = stringResource(R.string.include_pcap_in_prompt),
                style = MaterialTheme.typography.bodyLarge
            )
            // --- Status Display ---
            // Show status if selected OR if there's an error (even if not selected)
            AnimatedVisibility(visible = uiState.isPcapSelected || pcapState is PcapProcessingState.Error) {
                // Wrap the when content in a Column to allow multiple lines within a state branch
                Column(modifier = Modifier.padding(top = 4.dp)) { // Add some top padding for the status section
                    when (pcapState) {
                        is PcapProcessingState.Idle -> {
                            // Show hint if selected and Idle (means conversion ready to start or finished previously and user deselected/reselected)
                            if (uiState.isPcapSelected) {
                                Text(
                                    stringResource(R.string.pcap_conversion_starts_on_selection),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            // If not selected & Idle, show nothing extra here
                        }

                        is PcapProcessingState.Uploading -> {
                            Column { // Progress bar and text
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f) // Takes 60% of the column width
                                        .height(4.dp) // Make progress bar thinner
                                )
                                Text(
                                    stringResource(R.string.pcap_status_uploading),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        is PcapProcessingState.Processing -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.pcap_status_processing_server), // Potentially more descriptive string
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        is PcapProcessingState.Polling -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    R.string.pcap_status_checking_attempt, pcapState.attempt
                                ), // String with placeholder
                                // Example string: <string name="pcap_status_checking_attempt">Checking status (Attempt %1$d)...</string>
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        is PcapProcessingState.DownloadingJson -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.pcap_status_downloading),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        is PcapProcessingState.Success -> {
                            // Use a Column here to stack success message and potential warning
                            Column {
                                Text(
                                    stringResource(R.string.pcap_status_success_ready), // E.g., "Conversion successful, ready"
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary // Use primary color for success
                                )
                                // *** ADDED: Show truncation warning if needed ***
                                if (pcapState.wasTruncated) {
                                    Spacer(Modifier.height(4.dp)) // Space before warning
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Info,
                                            contentDescription = stringResource(R.string.warning), // Content description for accessibility
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant, // Use a less prominent color
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.pcap_json_truncated_warning),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic, // Italicize the note
                                            color = MaterialTheme.colorScheme.onSurfaceVariant // Use a less prominent color
                                        )
                                    }
                                }
                                // *** END ADDED ***
                            }
                        }

                        is PcapProcessingState.Error -> Column { // Use Column for error + explanation
                            Text(
                                stringResource(
                                    R.string.error_prefix,
                                    pcapState.message.take(60) + if (pcapState.message.length > 60) "..." else ""
                                ), // Show truncated error with "Error: " prefix
                                // Example string: <string name="error_prefix">Error: %1$s</string>
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            // Add explanation text below the error
                            Text(
                                stringResource(R.string.pcap_error_not_included_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    } // End Column wrapping the when statement's content
                } // End AnimatedVisibility for status
            } // End Column for text/status

            // --- Retry Button for Error State ---
            // Show retry button only if there is an error AND it's not currently trying again
            AnimatedVisibility(
                visible = pcapState is PcapProcessingState.Error && !uiState.isPcapConverting,
                modifier = Modifier.padding(start = 8.dp) // Add padding to separate from text column
            ) {
                IconButton(onClick = onRetry, enabled = true) { // Enable button when visible
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.retry_pcap_conversion),
                        tint = MaterialTheme.colorScheme.primary // Use primary color for retry action
                    )
                }
            }
            // --- End Retry Button ---
        } // End Row
    } // End PcapSelectionItem Composable
}

// --- Previews for PcapSelectionItem ---

@Preview(name = "PCAP Item - Idle Not Selected", showBackground = true, widthDp = 380)
@Composable
fun PcapSelectionItemPreview_IdleNotSelected() {
    AppTheme { // Use your app's theme
        val mockUiState = AutomaticAnalysisUiState(
            isPcapIncludable = true, // Assume PCAP is available for the item to show
            isPcapSelected = false, pcapProcessingState = PcapProcessingState.Idle
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
            pcapProcessingState = PcapProcessingState.DownloadingJson(
                "job-id-789", "http://example.com/result.json"
            )
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
            pcapProcessingState = PcapProcessingState.Success(
                "job-id-abc", "[{...}]", true
            ) // JSON content doesn't matter for preview appearance
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
            isPcapIncludable = true, isPcapSelected = true, // Selected when error occurred
            pcapProcessingState = PcapProcessingState.Error(
                "Conversion timed out after 30 attempts. Please check server status.", "job-err-1"
            )
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
            pcapProcessingState = PcapProcessingState.Error(
                "Server returned 503 Service Unavailable.", "job-err-2"
            )
        )
        Column(Modifier.padding(8.dp)) {
            PcapSelectionItem(uiState = mockUiState, onToggle = {}, onRetry = {})
        }
    }
}


// --- Dark Mode Examples ---

@Preview(
    name = "PCAP Item - Idle Selected (Dark)",
    showBackground = true,
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
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

@Preview(
    name = "PCAP Item - Error Selected (Dark)",
    showBackground = true,
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
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

