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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource


/**
 * A sealed interface representing an icon for the [ErrorComponent].
 */
sealed interface ErrorIcon {
    /**
     * A data class representing a resource icon.
     *
     * @property resourceId the resource id of the icon.
     */
    data class ResourceIcon(val resourceId: Int) : ErrorIcon

    /**
     * A data class representing a vector icon.
     *
     * @property imageVector the image vector of the icon.
     */
    data class VectorIcon(val imageVector: ImageVector) : ErrorIcon
}

/**
 * A composable function to display an error message with an icon.
 *
 * @param error the error message.
 * @param icon the icon to display. If not provided, a default error icon is used.
 * @param modifier the modifier to use for the error component.
 * @param onRetryClick an optional lambda to invoke when the retry button is clicked.
 */
@Composable
fun ErrorComponent(
    error: String,
    icon: ErrorIcon = ErrorIcon.ResourceIcon(R.drawable.error),
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null
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
                    modifier = Modifier.size(256.dp),
                    colorFilter = ColorFilter.tint(LocalContentColor.current) // Apply tint
                )
            }
            is ErrorIcon.VectorIcon -> {
                Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null,
                    tint = LocalContentColor.current,
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
        Spacer(modifier = Modifier.height(8.dp))

        onRetryClick?.let {
            GhostButton(
                onClick = it,
                buttonText = stringResource(R.string.retry)
            )
        }
    }
}

/**
 * A preview function for the [ErrorComponent] with a vector icon.
 */
@Preview(showBackground = true)
@Composable
fun ErrorComponentPreviewWithVector() {
    MaterialTheme {
        ErrorComponent(
            error = "Something went wrong!",
            icon = ErrorIcon.VectorIcon(Icons.Default.Search),
            onRetryClick = {}
        )
    }
}

/**
 * A preview function for the [ErrorComponent] with a resource icon.
 */
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