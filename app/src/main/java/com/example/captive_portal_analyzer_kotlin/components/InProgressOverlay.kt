package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R

@Composable
fun InProgressOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
        Icon(
            painter = painterResource(id = R.drawable.hourglass_top_18px), // Use painterResource for drawable
            contentDescription = "In Progress",
            tint = Color.Gray,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
        )
    }
}

// Preview 1: Step in Progress
@Preview(showBackground = true)
@Composable
fun PreviewInProgressStep() {
    InProgressOverlay(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Step 1", style = MaterialTheme.typography.titleLarge)
            Text("Work in progress...")
            Button(onClick = { /* Interactable */ }) {
                Text("Click Me")
            }
        }
    }
}