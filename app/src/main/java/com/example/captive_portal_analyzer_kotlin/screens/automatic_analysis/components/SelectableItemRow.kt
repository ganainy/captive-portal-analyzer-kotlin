package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SelectableItemRow(
    description: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit, // This lambda receives the new checked state
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!isSelected) } // Toggle the state on click
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckedChange, // Let Checkbox handle the state change propagation
            enabled = enabled
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2, // Allow slightly more space for URLs/Paths
            overflow = TextOverflow.Ellipsis,
            color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha =  0.38f)
        )
    }
}