package com.example.captive_portal_analyzer_kotlin.components
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.captive_portal_analyzer_kotlin.R

@Composable
fun HintText(hint:String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = hint,
        style = TextStyle(
            color = Color.Gray,       // Set text color to gray
            fontSize = 12.sp          // Set a smaller font size
        ), textAlign = TextAlign.Center
    )
}
