package com.example.captive_portal_analyzer_kotlin.components



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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

@Composable
fun RoundCornerButton(
    onClick: () -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fillWidth: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    trailingIcon: Painter? = null // Optional icon to show after text
) {
    var buttonModifier = modifier
        .height(56.dp)

    if (fillWidth) {
        buttonModifier = buttonModifier.fillMaxWidth()
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        modifier = buttonModifier,
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
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
                if (trailingIcon != null) {
                    Icon(
                        painter = trailingIcon,
                        contentDescription = null, // Add appropriate description if needed
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(24.dp)
                            .height(24.dp)
                    )
                }
            }
        }
    }
}

// Previews for the RoundCornerButton

@Preview(showBackground = true, name = "RoundCornerButton - Without Icon")
@Composable
fun RoundCornerButtonPreview_WithoutIcon() {
    AppTheme {
        RoundCornerButton(
            onClick = {},
            buttonText = "End Analysis",
            modifier = Modifier.padding(16.dp),
            enabled = true,
            isLoading = false,
            fillWidth = true
        )
    }
}

@Preview(showBackground = true, name = "RoundCornerButton - With Icon")
@Composable
fun RoundCornerButtonPreview_WithIcon() {
    AppTheme {
        RoundCornerButton(
            onClick = {},
            buttonText = "Open File",
            modifier = Modifier.padding(16.dp),
            enabled = true,
            isLoading = false,
            fillWidth = true,
            trailingIcon = painterResource(id = R.drawable.folder_open_24px)
        )
    }
}

@Preview(showBackground = true, name = "RoundCornerButton - Loading")
@Composable
fun RoundCornerButtonPreview_Loading() {
    AppTheme {
        RoundCornerButton(
            onClick = {},
            buttonText = "Start",
            modifier = Modifier.padding(16.dp),
            enabled = true,
            isLoading = true,
            fillWidth = true,
            trailingIcon = painterResource(id = R.drawable.play_arrow_24px)
        )
    }
}

@Preview(showBackground = true, name = "RoundCornerButton - Disabled")
@Composable
fun RoundCornerButtonPreview_Disabled() {
    AppTheme {
        RoundCornerButton(
            onClick = {},
            buttonText = "Stop",
            modifier = Modifier.padding(16.dp),
            enabled = false,
            isLoading = false,
            fillWidth = true,
            trailingIcon = painterResource(id = R.drawable.stop_24px)
        )
    }
}