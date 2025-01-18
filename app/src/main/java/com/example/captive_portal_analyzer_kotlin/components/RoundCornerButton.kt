package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme



/**
 * A composable function that displays a round corner button with optional disabled or loading state.
 *
 * @param onClick the callback to be called when the button is clicked
 * @param buttonText the text to be displayed on the button
 * @param modifier the modifier to be applied to the button
 * @param enabled whether the button is enabled or not
 * @param isLoading whether the button should display a loading indicator instead of text
 * @param fillWidth whether the button should fill the maximum width
 * @param colors the color scheme to be applied to the button
 */
@Composable
fun RoundCornerButton(
    onClick: () -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fillWidth: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    var modifier = modifier
        .padding(horizontal = 16.dp)
        .height(56.dp)

    if (fillWidth) {
        modifier = modifier.fillMaxWidth()
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        modifier = modifier,
        colors = colors
    ) {

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * A preview composable function for the [RoundCornerButton].
 */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        RoundCornerButton(
            onClick = { },
            buttonText = "Test Button",
            enabled = true,
            isLoading = false,
            fillWidth = true
        )
    }
}