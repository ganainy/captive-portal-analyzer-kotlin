package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CustomChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isSelected: Boolean = false
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                modifier = modifier.wrapContentWidth()
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) Color(0xFFE0E0E0) else Color.White,
            labelColor = if (isSelected) Color.Black else Color.Gray
        ),
        modifier = modifier.height(24.dp)
    )
}

@Preview
@Composable
fun CustomChipPreview() {
    CustomChip("Label")
}

@Preview
@Composable
fun CustomChipSelectedPreview() {
    CustomChip("Label", isSelected = true)
}