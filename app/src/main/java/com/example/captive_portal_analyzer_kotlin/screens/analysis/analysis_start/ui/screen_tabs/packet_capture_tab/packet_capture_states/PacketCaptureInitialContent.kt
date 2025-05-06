package com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * Composable content displayed when packet capture mode has not been selected yet.
 */
@Composable
fun PacketCaptureInitialContent() {
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
            painter = painterResource(id = R.drawable.info),
            contentDescription = "Initial status",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = stringResource(R.string.select_capture_mode),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Description
        Text(
            text = stringResource(R.string.select_capture_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// --- PacketCaptureInitialContent Previews ---

@Preview(
    showBackground = true,
    name = "Initial State (Phone)",
    device = "spec:width=411dp,height=891dp"
)
@Preview(
    showBackground = true,
    name = "Initial State (Tablet)",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun PacketCaptureInitialContentPreview() {
    AppTheme {
        PacketCaptureInitialContent()
    }
}