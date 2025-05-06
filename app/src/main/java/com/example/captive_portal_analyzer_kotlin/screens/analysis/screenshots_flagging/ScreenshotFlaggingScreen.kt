package com.example.captive_portal_analyzer_kotlin.screens.analysis.screenshots_flagging

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity


@Composable
fun ScreenshotFlaggingScreen(
    viewModel: ScreenshotFlaggingViewModel, // Pass the dedicated ViewModel
    onFinishFlagging: () -> Unit // Lambda to navigate away when done
) {
    val screenshots by viewModel.screenshots.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State to track the currently zoomed screenshot
    var zoomedScreenshot by remember { mutableStateOf<ScreenshotEntity?>(null) }

    // LaunchedEffect to navigate if no screenshots are found after loading
    // This will trigger when isLoading becomes false AND screenshots is empty
    LaunchedEffect(isLoading, screenshots.isEmpty()) {
        if (!isLoading && screenshots.isEmpty()) {
            onFinishFlagging()
        }
    }

    Scaffold { paddingValues ->
        // Use a Box to potentially layer the zoom view over the main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Add padding around the content
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val colors = MaterialTheme.colorScheme
                Text(
                    text = stringResource(R.string.hint_select_privacy_images),
                    style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // hint to let user know clicking an image opens it zoomed
                HintTextWithIcon(
                    hint = stringResource(R.string.hint_click_image_to_zoom),
                    modifier = Modifier.padding(bottom = 16.dp)
                )


                if (isLoading) {
                    LoadingIndicator(message = stringResource(R.string.loading_screenshots))
                } else if (screenshots.isEmpty()) {
                    // Empty list UI is still shown briefly if navigation hasn't happened instantly
                    EmptyListUi(R.string.no_screenshots_captured_yet)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier
                            .weight(1f) // Take available space
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = screenshots,
                            key = { it.screenshotId }
                        ) { screenshot ->
                            // Pass the screenshot object and separate click handlers
                            ScreenshotSelectionCardForAnalysis(
                                screenshot = screenshot, // Pass the full screenshot object
                                isSelected = screenshot.isPrivacyOrTosRelated,
                                // Long click to toggle flag
                                onImageLongClick = {
                                    viewModel.toggleScreenshotAnalysisFlag(it)
                                },
                                // Normal click to show zoomed view
                                onImageClick = {
                                    zoomedScreenshot =
                                        it // Set state to show this screenshot zoomed
                                }
                            )
                        }
                    }

                    // Button to finish flagging
                    RoundCornerButton(
                        onClick = {
                            viewModel.finishFlagging()
                            onFinishFlagging()
                        },
                        buttonText = stringResource(R.string.proceed),
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }

            // Display the zoomed screenshot overlay if zoomedScreenshot is not null
            zoomedScreenshot?.let { screenshot ->
                ZoomedScreenshotView(
                    imagePath = screenshot.path,
                    onClose = { zoomedScreenshot = null } // Set state back to null to close
                )
            }
        }
    }
}

/**
 * Composable for displaying an image card that can be selected/deselected
 * specifically for the analysis/flagging process, now with distinct click behaviors.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenshotSelectionCardForAnalysis(
    screenshot: ScreenshotEntity, // Accept the full entity
    isSelected: Boolean,
    onImageClick: (ScreenshotEntity) -> Unit, // Pass entity on click
    onImageLongClick: (ScreenshotEntity) -> Unit // Pass entity on long click
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            // Use combinedClickable for distinct click/long click actions
            .combinedClickable(
                onClick = { onImageClick(screenshot) }, // Normal click passes entity to lambda
                onLongClick = { onImageLongClick(screenshot) } // Long click passes entity to lambda
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 0.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = screenshot.path)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = stringResource(R.string.screenshot_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // Crop for grid view
            )

            // Overlay for selected state on Long Click
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tos_privacy_related_label),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Composable to display a screenshot in a full-screen-like overlay with a close button.
 */
@Composable
fun ZoomedScreenshotView(
    imagePath: String,
    onClose: () -> Unit
) {
    // Using a Box to layer content. It will cover the whole screen due to fillMaxSize
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Add a semi-transparent background to dim the content behind
            .background(Color.Black.copy(alpha = 0.8f))
            // Add padding to the box itself to control image size relative to screen
            .padding(16.dp), // Adjust padding to get ~90% of screen size
        contentAlignment = Alignment.Center // Center the image within the box
    ) {
        // The Image itself
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(data = imagePath)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = stringResource(R.string.screenshot_image), // Reused string
            modifier = Modifier
                .fillMaxSize() // Image fills the padded area
                .border( // Optional: Add a subtle border
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp) // Match card shape
                ),
            contentScale = ContentScale.Fit // Use Fit to show the whole image, no cropping
        )

        // Close button positioned in the top-right corner of the Box
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(8.dp) // Padding from the edge of the container Box
                    .size(40.dp) // Fixed size for the button
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), // Semi-transparent background
                        CircleShape // Circular button shape
                    )
                    .border( // Optional: Add a border to the button
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close), // Need string resource for "Close"
                    tint = MaterialTheme.colorScheme.onSurface // Icon color
                )
            }
        }
    }
}

// Re-adding the utility composable for EmptyListUi
@Composable
private fun EmptyListUi(@StringRes stringRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(id = stringRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}