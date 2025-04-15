package com.example.captive_portal_analyzer_kotlin.components // Or your preferred package

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * A composable that wraps an anchor element and displays a popup hint
 * when the anchor is long-pressed.
 *
 * @param modifier Modifier applied to the Box wrapping the anchor.
 * @param popupHint The composable content to display inside the popup hint.
 * @param popupProperties Properties to configure the Popup behavior (e.g., focusable).
 * @param popupContainerColor Background color of the default popup container.
 * @param popupContentColor Content color (e.g., text color) inside the default popup container.
 * @param popupContainerShape Shape of the default popup container.
 * @param popupContainerElevation Elevation for the shadow of the default popup container.
 * @param popupPadding Padding inside the default popup container.
 * @param anchor The composable element that triggers the hint on long press.
 */
@Composable
fun LongPressHintPopup(
    modifier: Modifier = Modifier,
    popupHint: @Composable () -> Unit,
    popupProperties: PopupProperties = PopupProperties(focusable = true), // Allows dismissing with back press
    popupContainerColor: Color = MaterialTheme.colorScheme.inverseSurface, // Good contrast default
    popupContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface, // Contrasting text
    popupContainerShape: Shape = RoundedCornerShape(4.dp),
    popupContainerElevation: Dp = 4.dp,
    popupPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    anchor: @Composable () -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }
    // Store layout coordinates of the anchor to position the popup
    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current

    // Use Box to easily apply modifiers and contain the anchor
    Box(
        modifier = modifier
            // 1. Capture the anchor's layout coordinates
            .onGloballyPositioned { coordinates ->
                anchorCoordinates = coordinates
            }
            // 2. Detect long press gestures
            .pointerInput(Unit) { // Using Unit ensures the detector runs once
                detectTapGestures(
                    // Optional: Add onTap if you want to dismiss on tap too
                    // onTap = { showPopup = false },
                    onLongPress = {
                        // Offset is relative to the Box, not needed for popup positioning here
                        showPopup = true // Show the popup on long press
                    }
                )
            }
    ) {
        // Render the anchor content provided by the caller
        anchor()

        // 3. Show the Popup if needed
        if (showPopup) {
            // Calculate position using a custom PopupPositionProvider
            val positionProvider = remember(anchorCoordinates, density) {
                HintPopupPositionProvider(
                    anchorCoordinates = anchorCoordinates,
                    density = density
                )
            }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { showPopup = false }, // Dismiss when clicking outside
                properties = popupProperties
            ) {
                // Default styled container for the hint
                Surface(
                    modifier = Modifier.shadow(popupContainerElevation, popupContainerShape), // Apply shadow properly
                    shape = popupContainerShape,
                    color = popupContainerColor,
                    contentColor = popupContentColor, // Set content color for descendants
                    tonalElevation = 0.dp // Avoid double elevation if shadow is used
                ) {
                    Box(modifier = Modifier.padding(popupPadding)) {
                        popupHint() // Render the caller's hint content
                    }
                }
            }
        }
    }
}

/**
 * Custom PopupPositionProvider to position the hint usually below the anchor,
 * or above if there isn't enough space below. Tries to center horizontally.
 */
private class HintPopupPositionProvider(
    private val anchorCoordinates: LayoutCoordinates?,
    private val density: Density,
    private val verticalMarginDp: Dp = 8.dp, // Space between anchor and popup
    private val screenEdgePaddingDp: Dp = 16.dp // Padding from screen edges
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, // Bounds of the anchor IN THE WINDOW (useful)
        windowSize: IntSize, // Size of the entire window
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize // Measured size of the popup content
    ): IntOffset {
        val coordinates = anchorCoordinates // Use coordinates captured from onGloballyPositioned
        if (coordinates == null) {
            // Fallback: Center if anchor coordinates aren't available yet
            return IntOffset(
                (windowSize.width - popupContentSize.width) / 2,
                (windowSize.height - popupContentSize.height) / 2
            )
        }

        val anchorRect = coordinates.boundsInWindow() // Use bounds in window for screen positioning
        val verticalMarginPx = with(density) { verticalMarginDp.toPx() }
        val screenEdgePaddingPx = with(density) { screenEdgePaddingDp.toPx() }

        // Calculate potential Y position below the anchor
        val yPosBelow = anchorRect.bottom + verticalMarginPx
        // Calculate potential Y position above the anchor
        val yPosAbove = anchorRect.top - popupContentSize.height - verticalMarginPx

        // Decide whether to place above or below
        val y: Float
        val spaceBelow = windowSize.height - yPosBelow - popupContentSize.height - screenEdgePaddingPx
        val spaceAbove = yPosAbove - screenEdgePaddingPx

        // Prefer below if enough space, otherwise try above
        y = if (spaceBelow >= 0 || spaceAbove < 0) {
            yPosBelow // Place below
        } else {
            yPosAbove // Place above
        }
        // Clamp Y position to stay within screen bounds (considering padding)
        val clampedY = y.coerceIn(
            screenEdgePaddingPx,
            (windowSize.height - popupContentSize.height - screenEdgePaddingPx).toFloat()
        )

        // Calculate X position to center horizontally relative to the anchor
        var x = anchorRect.left + (anchorRect.width / 2f) - (popupContentSize.width / 2f)
        // Clamp X position to stay within screen bounds (considering padding)
        val clampedX = x.coerceIn(
            screenEdgePaddingPx,
            (windowSize.width - popupContentSize.width - screenEdgePaddingPx).toFloat()
        )

        return IntOffset(clampedX.toInt(), clampedY.toInt())
    }
}