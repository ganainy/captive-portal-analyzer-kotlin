package com.example.captive_portal_analyzer_kotlin.screens.session_list

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.MenuItem
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.room.network_session.OfflineNetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.OfflineScreenshotRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.screens.landing.LandingUiState
import com.example.captive_portal_analyzer_kotlin.theme.LightGreen
import com.example.captive_portal_analyzer_kotlin.theme.LightRed
import java.lang.Error
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun SessionListScreen(
    navigateToMenu: () -> Unit,
    navigateToAbout: () -> Unit,
    clickedSession: (NetworkSessionEntity, List<CustomWebViewRequestEntity>, List<WebpageContentEntity>, List<ScreenshotEntity>) -> Unit,
    offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    offlineWebpageContentRepository: OfflineWebpageContentRepository,
    offlineScreenshotRepository: OfflineScreenshotRepository,
    offlineNetworkSessionRepository: OfflineNetworkSessionRepository,
    navigateToSessionScreen: () -> Unit
) {
    val viewModel: SessionListViewModel = viewModel(
        factory = SessionListViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
            offlineWebpageContentRepository = offlineWebpageContentRepository,
            offlineNetworkSessionRepository = offlineNetworkSessionRepository,
            offlineScreenshotRepository = offlineScreenshotRepository,
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val sessionCustomWebViewRequests by viewModel.sessionCustomWebViewRequests.collectAsState()
    val sessionWebpageContent by viewModel.sessionWebpageContent.collectAsState()
    val sessionScreenshot by viewModel.sessionScreenshot.collectAsState()

    // Handle back button to navigate to menu screen instead of last screen (which might be analysis screen)
    BackHandler {
        navigateToMenu()
    }

    Scaffold(
        topBar = {
            ToolbarWithMenu(
                title = stringResource(id = R.string.report_screen_title),
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

            is SessionListUiState.Loading -> {
                // Show loading indicator
                CustomProgressIndicator()
            }

            is SessionListUiState.Error -> {
                // Show error message
                val error = stringResource((uiState as SessionListUiState.Error).messageStringResource)
                ErrorText(error)
            }

            SessionListUiState.Empty -> {
                // Show message that no sessions were detected
                val empty = stringResource(R.string.no_sessions_detected)
                ErrorText(empty)
            }

            SessionListUiState.Success -> {
                sessions?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        SessionsList(
                            sessions = it,
                            requestsBySession = sessionCustomWebViewRequests,
                            contentBySession = sessionWebpageContent,
                            screenshotsBySession = sessionScreenshot,
                            onSessionClick =
                            clickedSession,
                            navigateToSessionScreen = navigateToSessionScreen

                        )
                    }
                }
            }
        }

    }


}

@Composable
private fun ErrorText(
    error: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,  // Choose an appropriate icon
            contentDescription = null,
            modifier = Modifier.width(256.dp).height(256.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun PreviewErrorText() {
    ErrorText(
        error = stringResource(R.string.no_sessions_detected) // Provide a mock error state
    )
}


@Composable
fun SessionsList(
    sessions: List<NetworkSessionEntity>,
    requestsBySession: List<CustomWebViewRequestEntity>,
    contentBySession: List<WebpageContentEntity>,
    screenshotsBySession: List<ScreenshotEntity>,
    onSessionClick: (NetworkSessionEntity, List<CustomWebViewRequestEntity>, List<WebpageContentEntity>, List<ScreenshotEntity>) -> Unit,
    navigateToSessionScreen: () -> Unit
) {
    val sortedSessions = sessions.sortedByDescending { it.timestamp }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedSessions) { session ->
            val requests = requestsBySession.filter { it.sessionId == session.sessionId }
            val content = contentBySession.filter { it.sessionId == session.sessionId }
            val screenshots = screenshotsBySession.filter { it.sessionId == session.sessionId }
            SessionCard(
                session = session,
                requests = requests,
                content = content,
                screenshots = screenshots,
                onClick = { onSessionClick(session, requests, content, screenshots) },
                navigateToSessionScreen = navigateToSessionScreen
            )
        }
    }
}


@Composable
fun SessionCard(
    session: NetworkSessionEntity,
    requests: List<CustomWebViewRequestEntity>,
    content: List<WebpageContentEntity>,
    screenshots: List<ScreenshotEntity>,
    onClick: () -> Unit,
    navigateToSessionScreen: () -> Unit
) {
    val isRecent = (System.currentTimeMillis() - session.timestamp) < 10 * 60 * 1000L
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                onClick()
                navigateToSessionScreen()
            }),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {


            Text(
                text = "Network Session: ${session.sessionId}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            session.ssid?.let {
                Text(stringResource(R.string.network_name, it))
            }
            session.timestamp.let {
                Text(stringResource(R.string.date, formatDate(it)))
            }
            Text(stringResource(R.string.requests, requests.size))
            Text(stringResource(R.string.webpages, content.size))
            Text(stringResource(R.string.screenshots, screenshots.size))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                val icon =
                    if (session.isUploadedToRemoteServer) R.drawable.cloud else R.drawable.local
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = if (session.isUploadedToRemoteServer) "Cloud session" else "Local session",
                    modifier = Modifier
                        .width(48.dp)
                        .height(24.dp)
                )

                if (isRecent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.resource_new),
                        contentDescription = "New session",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .width(96.dp)
                            .height(48.dp)
                    )
                }
            }

        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SessionCard(
        session = NetworkSessionEntity(
            ssid = "SSID",
            bssid = "BSSID",
            timestamp = System.currentTimeMillis(),
            sessionId = UUID.randomUUID().toString(),
            isUploadedToRemoteServer = true
        ),
        requests = emptyList(),
        content = emptyList(),
        screenshots = emptyList(),
        onClick = {},
        navigateToSessionScreen = {}
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}



