package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.MenuItem
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.room.screenshots.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.WebpageContentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionScreen(
    navigateBack: () -> Unit,
    navigateToAbout: () -> Unit,
    sharedViewModel: SharedViewModel,
) {

    val clickedSession by sharedViewModel.clickedSession.collectAsState()
    val clickedSessionRequests by sharedViewModel.clickedSessionRequests.collectAsState()
    val clickedSessionContentList by sharedViewModel.clickedSessionContentList.collectAsState()
    val clickedSessionScreenshotList by sharedViewModel.clickedSessionScreenshotList.collectAsState()


    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
        )
    )
    val uiState by sessionViewModel.uiState.collectAsState()



    Scaffold(
        topBar = {
            ToolbarWithMenu(
                title = stringResource(id = R.string.session_screen_title),
                menuItems = listOf(
                    MenuItem(
                        iconPath = R.drawable.about,
                        itemName = stringResource(id = R.string.about),
                        onClick = {
                            navigateToAbout()
                        }
                    ),

                    )
            )
        },
    ) { paddingValues ->

        when (uiState) {

            is SessionUiState.Loading -> {
                // Show loading indicator
                CustomProgressIndicator()
            }

            is SessionUiState.Error -> {
                // Show error message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource((uiState as LandingUiState.Error).messageStringResource),
                        color = Color.Red
                    )
                }
            }

            SessionUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {

                    clickedSession?.let {
                        SessionDetail(
                            session = it,
                            requests = clickedSessionRequests,
                            content = clickedSessionContentList,
                            screenshots = clickedSessionScreenshotList,
                        )
                    }


                }
            }
        }
    }

}


@Composable
fun SessionDetail(
    session: NetworkSessionEntity,
    requests: List<CustomWebViewRequestEntity>,
    content: List<WebpageContentEntity>,
    screenshots: List<ScreenshotEntity>
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
                session.ssid?.let { Text("SSID: $it") }
                session.bssid?.let { Text("BSSID: $it") }
                session.ipAddress?.let { Text("IP: $it") }
                session.gatewayAddress?.let { Text("Gateway: $it") }
                session.securityType?.let { Text("Security: $it") }
                session.captivePortalUrl?.let { Text("Portal URL: $it") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Requests, Content, and Screenshots in TabRow
        var selectedTab by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Requests (${requests.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Content (${content.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Screenshots (${screenshots.size})") }
            )
        }

        when (selectedTab) {
            0 -> RequestsList(requests)
            1 -> ContentList(content)
            2 -> ScreenshotsList(screenshots)
        }
    }
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

