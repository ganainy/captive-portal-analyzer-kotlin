package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

// Collapsible and Hidable Hints Section --- Starts Collapsed
@Composable
fun CollapsibleAnalysisHints(
    isVisible: Boolean,
    isExpanded: Boolean, // Passed in state
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = isVisible) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Header Row for Hints (Always visible when isVisible=true) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analysis_hints_title),
                    style = MaterialTheme.typography.titleSmall, // Keep small title
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, // Note: Icons are reversed now for collapse state
                    contentDescription = if (isExpanded) stringResource(R.string.collapse_hints) else stringResource(R.string.expand_hints),
                    modifier = Modifier.size(20.dp) // Smaller icon
                )
                Spacer(Modifier.width(8.dp))
                // Dismiss ('X') Icon
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp) // Keep dismiss clickable area reasonable
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.dismiss_hints),
                        modifier = Modifier.size(20.dp) // Smaller dismiss icon
                    )
                }
            } // End Header Row

            // --- Animated Content for Hints ---
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) { // Padding when expanded
                    HintTextWithIcon(
                        hint = stringResource(R.string.ai_hint_1),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    HintTextWithIcon(
                        hint = stringResource(R.string.ai_hint_2),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    HintTextWithIcon(
                        hint = stringResource(R.string.hint_network),
                    )
                }
            } // End Animated Content
        } // End Main Column for Hints
    } // End AnimatedVisibility for overall visibility
}

// --- Previews for CollapsibleAnalysisHints ---

@Preview(name = "Hints - Visible Collapsed", showBackground = true)
@Composable
private fun CollapsibleAnalysisHintsPreview_VisibleCollapsed_Light() {
    AppTheme(darkTheme = false) {
        Surface { // Added surface for background color
            CollapsibleAnalysisHints(
                isVisible = true,
                isExpanded = false,
                onToggleExpand = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Hints - Visible Expanded", showBackground = true)
@Composable
private fun CollapsibleAnalysisHintsPreview_VisibleExpanded_Light() {
    AppTheme(darkTheme = false) {
        Surface {
            CollapsibleAnalysisHints(
                isVisible = true,
                isExpanded = true,
                onToggleExpand = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Hints - Visible Collapsed (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollapsibleAnalysisHintsPreview_VisibleCollapsed_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            CollapsibleAnalysisHints(
                isVisible = true,
                isExpanded = false,
                onToggleExpand = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Hints - Visible Expanded (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollapsibleAnalysisHintsPreview_VisibleExpanded_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            CollapsibleAnalysisHints(
                isVisible = true,
                isExpanded = true,
                onToggleExpand = {},
                onDismiss = {}
            )
        }
    }
}