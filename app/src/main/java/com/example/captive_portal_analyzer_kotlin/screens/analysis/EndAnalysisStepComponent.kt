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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton

/**
 * A warning card to let user know analysis might not be complete.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndAnalysisStepComponent(
    onNavigateToSessionList: () -> Unit,
    modifier: Modifier = Modifier,
    onStopAnalysis: () -> Unit,
    analysisStatus: AnalysisStatus,
    isEndAnalysisEnabled: Boolean = true,
    updateSelectedTabIndex: (Int) -> Unit
) {

    // This is used to navigate to the session list screen when the analysis is completed
    LaunchedEffect(key1 = analysisStatus) {
        if (analysisStatus == AnalysisStatus.Completed) {
            onNavigateToSessionList()
        }
    }

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

            // End analysis Button
            RoundCornerButton(
                modifier = Modifier
                    .padding(end = 8.dp),
                onClick = {
                    onStopAnalysis()
                },
                buttonText = stringResource(R.string.end_analysis),
                enabled = isEndAnalysisEnabled
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_BeforeCheck_PreviousStepsNotYetCompleted() {
    EndAnalysisStepComponent(
        onNavigateToSessionList = {},
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.Initial,
        isEndAnalysisEnabled = false,
        updateSelectedTabIndex = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_BeforeCheck_PreviousStepsCompleted() {
    EndAnalysisStepComponent(
        onNavigateToSessionList = {},
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.Initial,
        isEndAnalysisEnabled = true,
        updateSelectedTabIndex = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_AnalysisComplete() {
    EndAnalysisStepComponent(
        onNavigateToSessionList = {},
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.Completed,
        isEndAnalysisEnabled = true,
        updateSelectedTabIndex = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEndAnalysisStep_AnalysisNotComplete() {
    EndAnalysisStepComponent(
        onNavigateToSessionList = {},
        onStopAnalysis = {},
        analysisStatus = AnalysisStatus.NotCompleted,
        isEndAnalysisEnabled = true,
        updateSelectedTabIndex = {},
    )
}