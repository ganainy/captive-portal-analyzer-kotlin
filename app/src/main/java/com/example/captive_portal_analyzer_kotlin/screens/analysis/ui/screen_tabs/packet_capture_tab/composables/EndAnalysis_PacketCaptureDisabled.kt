package com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.screen_tabs.packet_capture_tab.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisInternetStatus

/**
 * A warning card to let user know analysis might not be complete.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndAnalysis_PacketCaptureDisabled(
    onStopAnalysis: () -> Unit, //checks if analysis is actually completed
    analysisInternetStatus: AnalysisInternetStatus,
    updateSelectedTabIndex: (Int) -> Unit,
    markAnalysisAsComplete: () -> Unit,
) {

    Column {
        if (analysisInternetStatus == AnalysisInternetStatus.NO_INTERNET_ACCESS) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.warning),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp)
                )
                Text(
                    text = stringResource(R.string.it_looks_like_you_still_have_no_full_internet_connection_please_complete_the_login_process_of_the_captive_portal_before_stopping_the_analysis),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (analysisInternetStatus == AnalysisInternetStatus.NO_INTERNET_ACCESS) {
                // Continue analysis Button
                RoundCornerButton(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    onClick = {
                        updateSelectedTabIndex(0) // Navigate to the first tab (WebView)
                    },
                    buttonText = stringResource(R.string.continue_analysis),
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // End analysis Button, shown initially before checking if analysis is completed
            if (analysisInternetStatus == AnalysisInternetStatus.INITIAL || analysisInternetStatus == AnalysisInternetStatus.LOADING) {
                RoundCornerButton(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    onClick = {
                        onStopAnalysis()
                    },
                    buttonText = stringResource(R.string.end_analysis),
                    enabled = analysisInternetStatus != AnalysisInternetStatus.LOADING,
                    isLoading = analysisInternetStatus == AnalysisInternetStatus.LOADING
                )
            }

            Spacer(modifier = Modifier.size(8.dp))
            // End analysis Anyway Button (shows when analysis is not completed)
            if (analysisInternetStatus == AnalysisInternetStatus.NO_INTERNET_ACCESS) {
            GhostButton(
                modifier = Modifier
                    .padding( vertical = 8.dp, horizontal = 8.dp).fillMaxWidth(),
                onClick = {
                    markAnalysisAsComplete()
                },
                buttonText = stringResource(R.string.end_analysis_anyway),
            )
        }

            // app have full internet access, so we can mark the analysis as complete
            if (analysisInternetStatus == AnalysisInternetStatus.FULL_INTERNET_ACCESS) {
                markAnalysisAsComplete()
            }

        }
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysis_PacketCaptureDisabled_BeforeCheck() {
    EndAnalysis_PacketCaptureEnabled(
        onStopAnalysis = {},
        onForceStopAnalysis = {},
        analysisInternetStatus = AnalysisInternetStatus.INITIAL,
        onUpdateSelectedTabIndex = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysis_PacketCaptureDisabled_DuringCheck() {
    EndAnalysis_PacketCaptureEnabled(
        onStopAnalysis = {},
        onForceStopAnalysis = {},
        analysisInternetStatus = AnalysisInternetStatus.LOADING,
        onUpdateSelectedTabIndex = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysis_PacketCaptureDisabled_AnalysisComplete() {
    EndAnalysis_PacketCaptureEnabled(
        onStopAnalysis = {},
        onForceStopAnalysis = {},
        analysisInternetStatus = AnalysisInternetStatus.FULL_INTERNET_ACCESS,
        onUpdateSelectedTabIndex = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysis_PacketCaptureDisabled_AnalysisNotComplete() {
    EndAnalysis_PacketCaptureEnabled(
        onStopAnalysis = {},
        onForceStopAnalysis = {},
        analysisInternetStatus = AnalysisInternetStatus.NO_INTERNET_ACCESS,
        onUpdateSelectedTabIndex = {},
    )
}