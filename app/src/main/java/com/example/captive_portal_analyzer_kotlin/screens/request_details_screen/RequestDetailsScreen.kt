package com.example.captive_portal_analyzer_kotlin.screens.request_details_screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.components.RequestMethodView
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod

@Composable
fun RequestDetailsScreen(mainViewModel: MainViewModel) {

    // Collect the selected WebViewRequest from the sharedViewModel
    val webViewRequest by mainViewModel.clickedWebViewRequestEntity.collectAsState()

    Scaffold(
    ) { paddingValues ->
        RequestDetailsContent(paddingValues, webViewRequest)
    }

}



@Composable
private fun RequestDetailsContent(
    paddingValues: PaddingValues,
    webViewRequest: CustomWebViewRequestEntity?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Display each field in a Text component with bold labels and dividers
        Text(
            text = "Custom WebView Request ID: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = webViewRequest?.customWebViewRequestId.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Session ID:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = webViewRequest?.sessionId ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Type:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = webViewRequest?.type ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "URL:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = webViewRequest?.url ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        RequestMethodView(method = webViewRequest?.method ?: RequestMethod.UNKNOWN)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Body:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = webViewRequest?.body?.takeIf { it.isNotEmpty() } ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Headers:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = webViewRequest?.headers ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = if (webViewRequest?.hasFullInternetAccess == true) stringResource(R.string.after_authentication_request) else stringResource(
                R.string.before_authentication_request
            ),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun RequestDetailsScreenPreview() {
    RequestDetailsContent(
        PaddingValues(), CustomWebViewRequestEntity(
            customWebViewRequestId = 1,
            sessionId = "1",
            type = "HTML",
            url = "www.example.com",
            method = RequestMethod.GET,
            body = "",
            headers = "test",
        )
    )
}