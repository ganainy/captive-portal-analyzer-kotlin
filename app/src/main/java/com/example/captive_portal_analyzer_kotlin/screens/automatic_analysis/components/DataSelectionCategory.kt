package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

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
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R

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
            // Handle case where category is disabled but has no items yet (e.g. loading)
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_items_available),
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = LocalContentColor.current.copy(alpha =  0.38f)
                )
            }
        }
    }
    }