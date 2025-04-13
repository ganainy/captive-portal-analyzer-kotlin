package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


/**
 * A button with a ghost (transparent) background and a scale animation.
 *
 * @param onClick the callback to be called when the button is clicked
 * @param buttonText the text to be displayed on the button
 * @param modifier the modifier to be applied to the button
 * @param enabled whether the button is enabled or not
 * @param contentPadding the padding to be applied to the content of the button
 * @param isLoading whether the button is in a loading state
 * @param fillWidth whether the button should fill the available width
 */
@Composable
fun GhostButton(
    onClick: () -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    isLoading: Boolean = false,
    fillWidth: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    val buttonModifier = modifier
        .scale(scale)
        .height(56.dp)
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)


    OutlinedButton(
        onClick = onClick,
        modifier = buttonModifier,
        shape = RoundedCornerShape(16.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Gray,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.5f)
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.width(24.dp).height(24.dp),
                color = Color.Gray,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = buttonText)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GhostButtonPreview() {
    Surface {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GhostButton(onClick = { /* Handle click */ }, buttonText = "Default")

            GhostButton(onClick = { /* Handle click */ }, buttonText = "Disabled", enabled = false)

            GhostButton(onClick = { /* Handle click */ }, buttonText = "Loading", isLoading = true)

            GhostButton(
                onClick = { /* Handle click */ },
                buttonText = "Loading Disabled",
                isLoading = true,
                enabled = false
            )

            GhostButton(onClick = { /* Handle click */ }, buttonText = "Fill Width", fillWidth = true)

            GhostButton(
                onClick = { /* Handle click */ },
                buttonText = "Fill Width Loading",
                fillWidth = true,
                isLoading = true
            )

        }
    }
}