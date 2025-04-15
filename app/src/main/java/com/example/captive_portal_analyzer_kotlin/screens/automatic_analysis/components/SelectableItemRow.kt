package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

// No changes needed in SelectableItemRow.kt
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha =  0.38f)
        )
    }
}

// --- Previews for SelectableItemRow ---

@Preview(name = "Item - Enabled Unselected", showBackground = true)
@Composable
private fun SelectableItemRowPreview_EnabledUnselected_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) { // Column to give some padding
                SelectableItemRow(
                    description = "GET https://example.com/resource",
                    isSelected = false,
                    onCheckedChange = {},
                    enabled = true
                )
            }
        }
    }
}

@Preview(name = "Item - Enabled Selected", showBackground = true)
@Composable
private fun SelectableItemRowPreview_EnabledSelected_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SelectableItemRow(
                    description = "POST https://api.tracker.com/data",
                    isSelected = true,
                    onCheckedChange = {},
                    enabled = true
                )
            }
        }
    }
}

@Preview(name = "Item - Disabled Unselected", showBackground = true)
@Composable
private fun SelectableItemRowPreview_DisabledUnselected_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SelectableItemRow(
                    description = "GET https://example.com/disabled_resource",
                    isSelected = false,
                    onCheckedChange = {},
                    enabled = false // Disabled state
                )
            }
        }
    }
}

@Preview(name = "Item - Disabled Selected", showBackground = true)
@Composable
private fun SelectableItemRowPreview_DisabledSelected_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SelectableItemRow(
                    description = "POST https://api.tracker.com/disabled_data",
                    isSelected = true, // Selected but disabled
                    onCheckedChange = {},
                    enabled = false
                )
            }
        }
    }
}

@Preview(name = "Item - Long Text Ellipsis", showBackground = true, widthDp = 200) // Narrow width
@Composable
private fun SelectableItemRowPreview_LongText_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SelectableItemRow(
                    description = "GET https://very.long.domain.name.that.should.definitely.cause.ellipsis/path/to/a/resource/file.html?query=param&another=value",
                    isSelected = false,
                    onCheckedChange = {},
                    enabled = true
                )
            }
        }
    }
}

// --- Dark Mode Previews ---

@Preview(name = "Item - Enabled Selected (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectableItemRowPreview_EnabledSelected_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SelectableItemRow(
                    description = "POST https://api.tracker.com/data",
                    isSelected = true,
                    onCheckedChange = {},
                    enabled = true
                )
            }
        }
    }
}

@Preview(name = "Item - Disabled Unselected (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectableItemRowPreview_DisabledUnselected_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SelectableItemRow(
                    description = "GET https://example.com/disabled_resource",
                    isSelected = false,
                    onCheckedChange = {},
                    enabled = false
                )
            }
        }
    }
}