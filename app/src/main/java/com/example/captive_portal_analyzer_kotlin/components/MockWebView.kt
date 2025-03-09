package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

@Composable
fun MockWebView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) {
        Text(
            text = "Mock WebView Preview",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
@Preview
fun MockWebViewPreview() {
    AppTheme {
        MockWebView()
    }
}