package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import SessionData
import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.captive_portal_analyzer_kotlin.R

import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionScreen(
    sharedViewModel: SharedViewModel,
    repository : NetworkSessionRepository
) {

    val clickedReport by sharedViewModel.clickedSessionData.collectAsState()

    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
        )
    )

    val isUploading by sessionViewModel.isUploading.collectAsState()

    val showToast= { message:String, style: ToastStyle ->
        sharedViewModel.showToast(
            message = message, style = style,
        )
    }

    Scaffold(

    ) { paddingValues ->


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            clickedReport?.let {
                SessionDetail(
                    clickedSessionData = clickedReport!!,
                    uploadSession = {
                        sessionViewModel.uploadSession(
                            clickedReport!!,
                            showToast,
                            sharedViewModel::updateClickedSessionData
                        )
                    },
                    isUploading = isUploading
                )
            }


        }
    }

}


@Composable
fun SessionDetail(
    clickedSessionData: SessionData,
    uploadSession: () -> Unit,
    isUploading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Session Details",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Network Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                clickedSessionData.session.ssid?.let { Text("SSID: $it") }
                clickedSessionData.session.bssid?.let { Text("BSSID: $it") }
                clickedSessionData.session.ipAddress?.let { Text("IP: $it") }
                clickedSessionData.session.gatewayAddress?.let { Text("Gateway: $it") }
                clickedSessionData.session.securityType?.let { Text("Security: $it") }
                clickedSessionData.session.captivePortalUrl?.let { Text("Portal URL: $it") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape =  RoundedCornerShape(16.dp),
            enabled = !clickedSessionData.session.isUploadedToRemoteServer,
            onClick = {
                uploadSession()
            }
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                if (clickedSessionData.session.isUploadedToRemoteServer) {
                    Text(stringResource(R.string.already_uploaded))
                } else {
                    Text(stringResource(R.string.upload_session_for_analysis))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Requests, Content, and Screenshots in TabRow
        var selectedTab by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Requests (${clickedSessionData.requests.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Content (${clickedSessionData.webpageContent.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Screenshots (${clickedSessionData.screenshots.size})") }
            )
        }

        when (selectedTab) {
            0 -> RequestsList(clickedSessionData.requests)
            1 -> ContentList(clickedSessionData.webpageContent)
            2 -> ScreenshotsList(clickedSessionData.screenshots)
        }
    }
}

@Preview(showBackground = true,)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SessionDetailPreview() {
    SessionDetail(
        clickedSessionData = SessionData(
            session = NetworkSessionEntity(
                ssid = "test ssid",
                bssid = "test bssid",
                ipAddress = "test ip",
                gatewayAddress = "test gateway",
                securityType = "test security",
                captivePortalUrl = "test portal url",
                timestamp = Date().time,
                sessionId = 1.toString(),
                isCaptiveLocal = true,
                isUploadedToRemoteServer = false,
            ),
            requests = listOf(
                CustomWebViewRequestEntity(
                    sessionId = 11.toString(),
                    type = "GET",
                    url = "https://www.example.com",
                    method = "GET",
                    body = "{}",
                    headers = "{}",
                )
            ),
            screenshots = listOf(
                ScreenshotEntity(
                    sessionId = "11",
                    timestamp = Date().time,
                    path = "test path",
                    size = "100KB",
                    url = "test url"
                )
            ),
            webpageContent = listOf(
                WebpageContentEntity(
                    sessionId = 11.toString(),
                    url = "https://www.example.com",
                    htmlContent = "<html><body>This is a test.</body></html>",
                    timestamp = Date().time,
                    contentId = 1234,
                    jsContent = "TODO()",
                )
            )
        ),
        uploadSession = { },
        isUploading = false
    )
}


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun RequestsList(requests: List<CustomWebViewRequestEntity>) {
    LazyColumn {
        items(requests) { request ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    request.url?.let { Text("URL: $it") }
                    request.method?.let { Text("Method: $it") }
                    request.type?.let { Text("Type: $it") }
                }
            }
        }
    }
}

@Composable
private fun ContentList(content: List<WebpageContentEntity>) {
    LazyColumn {
        items(content) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    item.url?.let { Text("URL: $it") }
                    Text("Timestamp: ${formatDate(item.timestamp)}")
                }
            }
        }
    }
}


@Composable
private fun ScreenshotsList(screenshots: List<ScreenshotEntity>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 400.dp), // Adjust number of columns as needed
            modifier = Modifier.padding(16.dp)
        ) {
            items(screenshots) { screenshot ->
                AsyncImage(
                    model = screenshot.path, // Path to the local image
                    contentDescription = "Local Image",
                    modifier = Modifier
                        .height(350.dp)
                        .width(350.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}

