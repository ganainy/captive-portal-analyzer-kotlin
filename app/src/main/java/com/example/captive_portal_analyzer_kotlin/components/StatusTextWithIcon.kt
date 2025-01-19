package com.example.captive_portal_analyzer_kotlin.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A composable function that displays a status text with a checkmark or close icon, depending
 * on the [isSuccess] parameter.
 *
 * @param text the text to be displayed
 * @param isSuccess whether the status is successful or not
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusTextWithIcon(text: String, isSuccess: Boolean) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = if (isSuccess) {
                MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.LineThrough
                )
            } else {
                MaterialTheme.typography.bodyLarge
            }
        )
        Spacer(modifier = Modifier.width(8.dp))  // Space between text and icon
        if (isSuccess) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Success",
                tint = Color.Green
            )
        } else {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Failure", tint = Color.Red)
        }
    }
}

/**
 * Preview function for [StatusTextWithIcon].
 */
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StatusTextWithIconPreview() {
    MaterialTheme {
        StatusTextWithIcon(
            text = "Text",
            isSuccess = true
        )
    }
}

/**
 * Preview function for [StatusTextWithIcon].
 */
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StatusTextWithIconPreview2() {
    MaterialTheme {
        StatusTextWithIcon(
            text = "Text",
            isSuccess = false
        )
    }
}