package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R


/**
 * A composable function that displays a banner when there is no internet connection.
 *
 * @param modifier The modifier to apply to the Surface composable.
 */
@Composable
fun NoInternetBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.error,
        shadowElevation = 4.dp
    ) {
        Text(
            text = stringResource(R.string.no_internet_connection),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onError,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A composable function that displays an animated banner (slide in effect) when there is no internet connection.
 *
 * @param isConnected Whether the device is connected to the internet.
 */
@Composable
fun AnimatedNoInternetBanner(isConnected: Boolean){
    AnimatedVisibility(
        visible = !isConnected,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top)
    ) {
        NoInternetBanner()
    }
}

/**
 * A preview of the NoInternetBanner composable function.
 */
@Preview(showBackground = true)
@Composable
fun NoInternetBannerPreview() {
    NoInternetBanner()
}