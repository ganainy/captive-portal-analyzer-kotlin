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

@Composable
fun RoundCornerButton(
    onClick: () -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
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

@Preview
@Composable
private fun Preview() {
    AppTheme {
        RoundCornerButton(
            onClick = { },
            buttonText = "Test Button",
            enabled = true,
            isLoading = false
        )
    }
}