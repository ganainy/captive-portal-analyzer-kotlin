package com.example.captive_portal_analyzer_kotlin.screens.analysis

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

/**
 * A warning card to let user know analysis might not be complete.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndAnalysisStepComponent(
    modifier: Modifier = Modifier,
    onStopAnalysis: () -> Unit, //checks if analysis is actually completed
    onForceStopAnalysis: () -> Unit, //force end the analysis even if not completed
    analysisStatus: AnalysisStatus,
    updateSelectedTabIndex: (Int) -> Unit
) {

    Column {
        if (analysisStatus == AnalysisStatus.NotCompleted) {
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
            if (analysisStatus == AnalysisStatus.NotCompleted) {
                // Continue analysis Button
                RoundCornerButton(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    onClick = {
                        updateSelectedTabIndex(0) // Navigate to the first tab (WebView)
                    },
                    buttonText = stringResource(R.string.continue_analysis),
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // End analysis Button, shown initially before checking if analysis is completed
            if (analysisStatus == AnalysisStatus.Initial) {
                RoundCornerButton(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    onClick = {
                        onStopAnalysis()
                    },
                    buttonText = stringResource(R.string.end_analysis),
                )
            }

            Spacer(modifier = Modifier.size(8.dp))
            // End analysis Anyway Button (shows when analysis is not completed)
            if (analysisStatus == AnalysisStatus.NotCompleted) {
            GhostButton(
                modifier = Modifier
                    .padding(horizontal =  16.dp, vertical = 8.dp).fillMaxWidth(),
                onClick = {
                    onForceStopAnalysis()
                    //onNavigateToSessionList()
                },
                text = stringResource(R.string.end_analysis_anyway),
            )
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_BeforeCheck() {
    EndAnalysisStepComponent(
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.Initial,
        updateSelectedTabIndex = {},
        onForceStopAnalysis = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_AnalysisComplete() {
    EndAnalysisStepComponent(
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.Completed,
        updateSelectedTabIndex = {},
        onForceStopAnalysis = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_AnalysisNotComplete() {
    EndAnalysisStepComponent(
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.NotCompleted,
        updateSelectedTabIndex = {},
        onForceStopAnalysis = {},
    )
}