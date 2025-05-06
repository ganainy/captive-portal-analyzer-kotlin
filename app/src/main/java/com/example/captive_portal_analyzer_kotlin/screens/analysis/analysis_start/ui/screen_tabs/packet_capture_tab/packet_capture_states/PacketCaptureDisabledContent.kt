package com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * Composable content displayed when packet capture mode is explicitly disabled.
 */
@Composable
 fun PacketCaptureDisabledContent(
    webViewActions: WebViewActions,
    updateSelectedTabIndex: (Int) -> Unit,
    markAnalysisAsComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Icon
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Disabled status",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = stringResource(R.string.packet_capture_disabled),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Description
        Text(
            text = stringResource(R.string.you_choose_to_continue_without_packet_capture_end_the_analysis_to_proceed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RoundCornerButton(
            modifier = Modifier
                .padding(end = 8.dp),
            onClick = {
                markAnalysisAsComplete()
            },
            buttonText = stringResource(R.string.end_analysis),
        )
    }
}




// --- PacketCaptureDisabledContent Previews ---

/*
* only previews for no internet and loading because if there is internet a navigation will trigger to next screen
* */

@Preview(
    showBackground = true,
    name = "Disabled - No Internet (Phone)",
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PacketCaptureDisabledContentPreview() {
    // Need to mock EndAnalysis_PacketCaptureDisabled or provide its code
    // Assuming EndAnalysis_PacketCaptureDisabled is available and handles button states
    AppTheme {
        PacketCaptureDisabledContent(
            webViewActions = mockWebViewActions,
            updateSelectedTabIndex = mockIntLambda,
            markAnalysisAsComplete = mockUnitLambda
        )
    }
}


