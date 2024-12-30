package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    navigateBack: () -> Unit,
    navigateToAbout: () -> Unit,
    clickedSession: (NetworkSessionEntity, List<CustomWebViewRequestEntity>, List<WebpageContentEntity>, List<ScreenshotEntity>) -> Unit,
    offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    offlineWebpageContentRepository: OfflineWebpageContentRepository,
    offlineScreenshotRepository: OfflineScreenshotRepository,
    offlineNetworkSessionRepository: OfflineNetworkSessionRepository,
    navigateToSessionScreen: () -> Unit
) {
    val viewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory(
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

            is ReportUiState.Loading -> {
                // Show loading indicator
                CustomProgressIndicator()
            }

            is ReportUiState.Error -> {
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

            ReportUiState.Empty -> {
                // Show message that no sessions were detected
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_sessions_detected),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            ReportUiState.Success -> {
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
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Network Session: ${session.sessionId}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            session.ssid?.let {
                Text("SSID: $it")
            }
            session.timestamp.let {
                Text("Date: ${formatDate(it)}")
            }
            Text("Requests: ${requests.size}")
            Text("Webpage Contents: ${content.size}")
            Text("Screenshots: ${screenshots.size}")

            if (isRecent) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.resource_new),
                    contentDescription = "New session",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}



