package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod

@Composable
fun RequestMethodView(method: RequestMethod) {
    val textColor = when (method) {
        RequestMethod.GET -> Color.Green
        RequestMethod.POST -> Color.Blue
        RequestMethod.PUT -> Color.Yellow
        RequestMethod.DELETE -> Color.Red
        RequestMethod.PATCH -> Color.Magenta
        RequestMethod.HEAD -> Color.Cyan
        RequestMethod.OPTIONS -> Color.Gray
        RequestMethod.UNKNOWN -> Color.LightGray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Method:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(
            text = method.name,
            color = textColor
        )
    }
}

@Preview
@Composable
fun RequestMethodViewPreview() {
    RequestMethodView(RequestMethod.GET)
}