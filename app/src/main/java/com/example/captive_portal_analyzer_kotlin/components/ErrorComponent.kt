package com.example.captive_portal_analyzer_kotlin.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource


sealed interface ErrorIcon {
    data class ResourceIcon(val resourceId: Int) : ErrorIcon
    data class VectorIcon(val imageVector: ImageVector) : ErrorIcon
}

@Composable
fun ErrorComponent(
    error: String,
    icon: ErrorIcon = ErrorIcon.VectorIcon(Icons.Default.Search),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (icon) {
            is ErrorIcon.ResourceIcon -> {
                Image(
                    painter = painterResource(id = icon.resourceId),
                    contentDescription = null,
                    modifier = Modifier.size(256.dp)
                )
            }
            is ErrorIcon.VectorIcon -> {
                Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(256.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorComponentPreviewWithVector() {
    MaterialTheme {
        ErrorComponent(
            error = "Something went wrong!",
            icon = ErrorIcon.VectorIcon(Icons.Default.Search)
        )
    }
}

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ErrorComponentPreviewWithResource() {
    MaterialTheme {
        ErrorComponent(
            error = "No internet connection",
            icon = ErrorIcon.ResourceIcon(R.drawable.no_wifi) // Replace with your actual drawable resource
        )
    }
}