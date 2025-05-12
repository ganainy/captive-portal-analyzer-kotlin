package com.example.captive_portal_analyzer_kotlin.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme


/**
 * Displays a small icon followed by a hint text message.
 *
 * Adheres to Material Design guidelines by using theme typography and colors.
 * Ensures accessibility by requiring a content description for the icon.
 *
 * @param hint The text message to display.
 * @param iconResId The drawable resource ID for the icon.
 * @param contentDescription A meaningful description of the icon for accessibility.
 * @param modifier Optional Modifier for the Row layout.
 * @param tint Optional tint color for the icon. Defaults to the local content color
 *        (which is typically the same as the text color set by this composable).
 * @param textColor Optional text color. Defaults to `MaterialTheme.colorScheme.onSurfaceVariant`.
 */
@Composable
fun HintTextWithIcon(
    hint: String,
    @DrawableRes iconResId: Int? = R.drawable.info,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified, // Default allows Icon to use LocalContentColor
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Provide a default content color that suits hint text, affecting both Text and Icon (if tint is Unspecified)
    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconResId ?: R.drawable.info),
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp),
                tint = tint
            )

            Spacer(modifier = Modifier.width(4.dp)) // Reduced spacing for smaller icon/text

            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current
            )
        }
    }
}

// --- Previews ---

@Preview(name = "Hint - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HintTextWithIconPreviewLight() {
    AppTheme { // Wrap previews in your theme
        Surface(modifier = Modifier.padding(8.dp)) {
            HintTextWithIcon(
                hint = stringResource(R.string.created, "10 minutes ago"),
                iconResId = R.drawable.clock,
                contentDescription = "creation_time_icon_desc"
            )
        }
    }
}

@Preview(name = "Hint - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HintTextWithIconPreviewDark() {
    AppTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(8.dp)) {
            HintTextWithIcon(
                hint = "Upload pending",
                iconResId = R.drawable.cloud,
                contentDescription = "Upload status icon",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Preview(name = "Hint - Long Text", showBackground = true, widthDp = 200)
@Composable
private fun HintTextWithIconPreviewLong() {
    AppTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            HintTextWithIcon(
                hint = stringResource(R.string.long_lorem_ipsum),
                iconResId = R.drawable.info,
                contentDescription = "Information icon"
            )
        }
    }
}