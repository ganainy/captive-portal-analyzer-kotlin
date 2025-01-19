package com.example.captive_portal_analyzer_kotlin.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.captive_portal_analyzer_kotlin.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * A composable function to display a hint text with an icon.
 *
 * @param hint the hint text to display
 * @param iconResId the resource id of the icon to display next to the hint text
 * @param modifier the modifier to use for the row
 * @param textAlign the text alignment. Defaults to center
 * @param color the color of the text
 * @param rowAllignment the alignment of the row
 */

@Composable
fun HintTextWithIcon(
    hint: String,
    iconResId: Int = R.drawable.hint2,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Gray,
    rowAllignment: Alignment = Alignment.CenterStart

) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(rowAllignment),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val painter = androidx.compose.ui.res.painterResource(iconResId)
        Icon(
            painter = painter,
            contentDescription = stringResource(id = iconResId),
            modifier = Modifier
                .width(24.dp)
                .height(24.dp) // Set a fixed small size
        )

        Text(
            text = hint,
            style = TextStyle(
                color = color,
                fontSize = 12.sp,         // Set a smaller font size
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .padding(start = 8.dp)
                .wrapContentSize(Alignment.CenterStart),
            textAlign = textAlign
        )
    }
}


/**
 * Preview function for HintText composable in light theme.
 */
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HintTextWithIconPreviewLight() {
    HintTextWithIcon(
        hint = "stringResource(R.string.long_lorem_ipsum)",
        iconResId = R.drawable.cloud
    )
}

/**
 * Preview function for HintText composable in dark theme.
 */
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HintTextWithIconPreviewDark() {
    HintTextWithIcon(
        hint = stringResource(R.string.long_lorem_ipsum),
    )
}