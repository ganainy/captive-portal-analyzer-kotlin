package com.example.captive_portal_analyzer_kotlin.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R

/**
 * A composable function that displays a status text with a checkmark or close icon, depending
 * on the [isSuccess] parameter.
 *
 * @param text the text to be displayed
 * @param isSuccess whether the status is successful or not
 */
@Composable
fun StatusTextWithIcon(text: String, isSuccess: Boolean, isLoading: Boolean = false, number: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${number?.let { "$it. " } ?: "• "} $text",
            style = if (isSuccess) {
                MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.LineThrough
                )
            } else {
                MaterialTheme.typography.bodyLarge
            },
            modifier = Modifier.weight(1f, fill = false)  // Take available space but don't force expansion
        )

        // Icon section
        Box(modifier = Modifier.padding(start = 4.dp)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else if (isSuccess) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Success",
                    tint = Color.Green
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Failure",
                    tint = Color.Red
                )
            }
        }
    }
}
/**
 * Preview function for [StatusTextWithIcon].
 */
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StatusTextWithIconPreview_Success() {
    MaterialTheme {
        StatusTextWithIcon(
            text = "Text",
            isSuccess = true,
            number = 1
        )
    }
}

/**
 * Preview function for [StatusTextWithIcon].
 */
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StatusTextWithIconPreview_Error() {
    MaterialTheme {
        StatusTextWithIcon(
            text = stringResource(R.string.long_lorem_ipsum),
            isSuccess = false
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StatusTextWithIconPreview_Loading() {
    MaterialTheme {
        StatusTextWithIcon(
            text = "Text",
            isLoading = true,
            isSuccess = false
        )
    }
}