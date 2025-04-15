package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * A customizable chip composable that adapts to light/dark themes
 * and allows overriding default colors.
 *
 * @param label The text to display inside the chip.
 * @param modifier Modifier for the chip.
 * @param onClick Lambda executed when the chip is clicked.
 * @param isSelected Whether the chip is currently selected. Controls the default appearance.
 * @param selectedContainerColor Optional color for the chip's container when selected.
 *                               Defaults to MaterialTheme.colorScheme.secondaryContainer.
 * @param selectedLabelColor Optional color for the chip's label when selected.
 *                           Defaults to MaterialTheme.colorScheme.onSecondaryContainer.
 * @param unselectedContainerColor Optional color for the chip's container when not selected.
 *                                 Defaults to MaterialTheme.colorScheme.surfaceVariant.
 * @param unselectedLabelColor Optional color for the chip's label when not selected.
 *                             Defaults to MaterialTheme.colorScheme.onSurfaceVariant.
 */
@OptIn(ExperimentalMaterial3Api::class) // Required for AssistChip
@Composable
fun CustomChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isSelected: Boolean = false,
    // Optional colors with theme-aware defaults
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unselectedLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Determine the actual colors to use based on selection state and passed parameters
    val containerColor = if (isSelected) selectedContainerColor else unselectedContainerColor
    val labelColor = if (isSelected) selectedLabelColor else unselectedLabelColor

    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1
                // Chip itself handles width based on content and constraints
            )
        },
        // Use standard Material 3 chip height for consistency
        modifier = modifier.height(AssistChipDefaults.Height),
        // Use default border behavior (often adds a border only when unselected)
       border = AssistChipDefaults.assistChipBorder(enabled = isSelected),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor
            // Let other colors like leading/trailing icons and disabled states use standard defaults
        )
    )
}

// --- Preview Functions ---

@Preview(name = "Chip - Unselected - Light", showBackground = true)
@Composable
fun CustomChipPreviewUnselectedLight() {
    AppTheme(darkTheme = false) { // Ensure light theme is applied
        Surface(color = MaterialTheme.colorScheme.background) { // Use Surface for context
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomChip(label = "Default Unselected", isSelected = false)
                CustomChip(
                    label = "Custom Unselected",
                    isSelected = false,
                    unselectedContainerColor = Color.LightGray,
                    unselectedLabelColor = Color.Black
                )
            }
        }
    }
}

@Preview(name = "Chip - Selected - Light", showBackground = true)
@Composable
fun CustomChipPreviewSelectedLight() {
    AppTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomChip(label = "Default Selected", isSelected = true)
                CustomChip(
                    label = "Custom Selected",
                    isSelected = true,
                    selectedContainerColor = Color(0xFFBBDEFB), // Light Blue
                    selectedLabelColor = Color(0xFF0D47A1)      // Dark Blue
                )
            }
        }
    }
}

@Preview(name = "Chip - Unselected - Dark", showBackground = true)
@Composable
fun CustomChipPreviewUnselectedDark() {
    AppTheme(darkTheme = true) { // Ensure dark theme is applied
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomChip(label = "Default Unselected", isSelected = false)
                CustomChip(
                    label = "Custom Unselected",
                    isSelected = false,
                    unselectedContainerColor = Color(0xFF424242), // Dark Gray
                    unselectedLabelColor = Color(0xFFE0E0E0)      // Light Gray
                )
            }
        }
    }
}

@Preview(name = "Chip - Selected - Dark", showBackground = true)
@Composable
fun CustomChipPreviewSelectedDark() {
    AppTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomChip(label = "Default Selected", isSelected = true)
                CustomChip(
                    label = "Custom Selected",
                    isSelected = true,
                    selectedContainerColor = Color(0xFF0D47A1), // Dark Blue
                    selectedLabelColor = Color(0xFFBBDEFB)      // Light Blue
                )
            }
        }
    }
}