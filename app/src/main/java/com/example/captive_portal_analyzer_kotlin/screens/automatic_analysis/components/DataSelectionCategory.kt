package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

@Composable
fun <T, ID> DataSelectionCategory( // Use correct ID type parameter if not Any
    title: String,
    items: List<T>,
    selectedIds: Set<ID>,
    idProvider: (T) -> ID,
    contentDescProvider: (T) -> String,
    onToggle: (ID) -> Unit,
    onSetAllSelected: ((Boolean) -> Unit)? = null,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(items.isNotEmpty()) } // Default expanded if items exist
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Only allow expansion toggle if enabled and items exist
                    .clickable(enabled = enabled && items.isNotEmpty()) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha =  0.38f)
                )
                // Show arrow only if expandable (items exist)
                if (items.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) stringResource(R.string.collapse_section) else stringResource(R.string.expand_section), // Add strings
                        tint = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha =  0.38f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded && items.isNotEmpty()) { // Only show content if expanded AND items exist
                Divider(modifier = Modifier.padding(horizontal = 16.dp)) // Add visual separator
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (onSetAllSelected != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            // Use TextButton for less emphasis maybe?
                            Button(onClick = { onSetAllSelected(true) }, enabled = enabled && selectedIds.size < items.size) {
                                Text(stringResource(R.string.select_all))
                            }
                            Button(onClick = { onSetAllSelected(false) }, enabled = enabled && selectedIds.isNotEmpty()) {
                                Text(stringResource(R.string.deselect_all))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    items.forEach { item ->
                        val itemId = idProvider(item)
                        val isSelected = selectedIds.contains(itemId)
                        SelectableItemRow(
                            description = contentDescProvider(item),
                            isSelected = isSelected,
                            onCheckedChange = { onToggle(itemId) }, // Pass ID directly
                            enabled = enabled
                        )
                    }
                }
            }
            // Handle case where category has no items
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_items_available),
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = LocalContentColor.current.copy(alpha =  0.38f) // Use disabled color alpha
                )
            }
        }
    }
}


// --- Previews for DataSelectionCategory ---

// Sample data structure for previews
private data class SampleItem(val id: Int, val description: String)

// Sample data list
private val sampleItems = listOf(
    SampleItem(1, "GET https://example.com/resource1"),
    SampleItem(2, "POST https://api.example.com/data"),
    SampleItem(3, "GET https://example.com/resource2?param=value"),
    SampleItem(4, "PUT https://another.api.com/items/123"),
)

@Preview(name = "Category - Enabled Expanded Some Selected", showBackground = true)
@Composable
private fun DataSelectionCategoryPreview_EnabledExpandedSomeSelected_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                DataSelectionCategory(
                    title = "Network Requests (2 / 4)",
                    items = sampleItems,
                    selectedIds = setOf(1, 3), // Sample selection
                    idProvider = { it.id },
                    contentDescProvider = { it.description },
                    onToggle = { /* Dummy */ },
                    onSetAllSelected = { /* Dummy */ },
                    enabled = true
                )
            }
        }
    }
}

@Preview(name = "Category - Enabled Expanded All Selected", showBackground = true)
@Composable
private fun DataSelectionCategoryPreview_EnabledExpandedAllSelected_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                DataSelectionCategory(
                    title = "Screenshots (4 / 4)",
                    items = sampleItems,
                    selectedIds = sampleItems.map { it.id }.toSet(), // All selected
                    idProvider = { it.id },
                    contentDescProvider = { "Screenshot ${it.id}: ${it.description.take(20)}..." },
                    onToggle = { /* Dummy */ },
                    onSetAllSelected = { /* Dummy */ },
                    enabled = true
                )
            }
        }
    }
}

@Preview(name = "Category - Enabled No Items", showBackground = true)
@Composable
private fun DataSelectionCategoryPreview_EnabledNoItems_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                DataSelectionCategory(
                    title = "Webpage Content (0 / 0)",
                    items = emptyList<SampleItem>(), // No items
                    selectedIds = emptySet<Int>(),
                    idProvider = { it.id },
                    contentDescProvider = { it.description },
                    onToggle = { /* Dummy */ },
                    onSetAllSelected = { /* Dummy */ },
                    enabled = true
                )
            }
        }
    }
}


@Preview(name = "Category - Disabled With Items", showBackground = true)
@Composable
private fun DataSelectionCategoryPreview_DisabledWithItems_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                DataSelectionCategory(
                    title = "Network Requests (1 / 4)",
                    items = sampleItems,
                    selectedIds = setOf(2), // Some selected but disabled
                    idProvider = { it.id },
                    contentDescProvider = { it.description },
                    onToggle = { /* Dummy */ },
                    onSetAllSelected = { /* Dummy */ },
                    enabled = false // Disabled state
                )
            }
        }
    }
}

// --- Dark Mode Previews ---

@Preview(name = "Category - Enabled Expanded (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DataSelectionCategoryPreview_EnabledExpanded_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                DataSelectionCategory(
                    title = "Network Requests (2 / 4)",
                    items = sampleItems,
                    selectedIds = setOf(1, 3),
                    idProvider = { it.id },
                    contentDescProvider = { it.description },
                    onToggle = { /* Dummy */ },
                    onSetAllSelected = { /* Dummy */ },
                    enabled = true
                )
            }
        }
    }
}

@Preview(name = "Category - Disabled No Items (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DataSelectionCategoryPreview_DisabledNoItems_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                DataSelectionCategory(
                    title = "Webpage Content (0 / 0)",
                    items = emptyList<SampleItem>(),
                    selectedIds = emptySet<Int>(),
                    idProvider = { it.id },
                    contentDescProvider = { it.description },
                    onToggle = { /* Dummy */ },
                    onSetAllSelected = { /* Dummy */ },
                    enabled = false // Disabled state
                )
            }
        }
    }
}