package com.example.captive_portal_analyzer_kotlin.screens.session_list

import NetworkSessionRepository
import android.app.Application
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun SessionListScreen(
    navigateToWelcome: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    repository: NetworkSessionRepository,
    navigateToSessionScreen: () -> Unit
) {
    val sessionListViewModel: SessionListViewModel = viewModel(
        factory = SessionListViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
        )
    )
    val uiState by sessionListViewModel.uiState.collectAsState()
    val sessionDataList by sessionListViewModel.sessionDataList.collectAsState()

    // Handle back button to navigate to menu screen instead of last screen (which might be analysis screen)
    BackHandler {
        navigateToWelcome()
    }

    Scaffold { paddingValues ->
        SessionsContent(
            uiState = uiState,
            sessionDataList = sessionDataList,
            paddingValues = paddingValues,
            navigateToSessionScreen = navigateToSessionScreen,
            updateClickedSessionId = { updateClickedSessionId(it) }
        )
    }


}

@Composable
private fun SessionsContent(
    uiState: SessionListUiState,
    sessionDataList: List<SessionData>?,
    paddingValues: PaddingValues,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit
) {
    when (uiState) {
        SessionListUiState.Loading -> {
            LoadingIndicator()
        }

        is SessionListUiState.Error -> {
            ErrorComponent( stringResource((uiState as SessionListUiState.Error).messageStringResource))
        }

        SessionListUiState.Empty -> {
            ErrorComponent(stringResource(R.string.no_sessions_detected))
        }

        SessionListUiState.Success -> {
            SessionsSuccessContent(
                sessionDataList = sessionDataList,
                paddingValues = paddingValues,
                navigateToSessionScreen = navigateToSessionScreen,
                updateClickedSessionId =updateClickedSessionId

            )
        }
    }
}

@Composable
private fun SessionsSuccessContent(
    sessionDataList: List<SessionData>?,
    paddingValues: PaddingValues,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit
) {
    sessionDataList?.let { sessionDataList ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SessionsList(
                sessionDataList = sessionDataList,
                onSessionClick = { clickedSession ->
                    updateClickedSessionId(clickedSession.session.networkId)
                },
                navigateToSessionScreen = navigateToSessionScreen
            )
        }
    }
}





@Composable
fun SessionsList(
    sessionDataList: List<SessionData>,
    onSessionClick: (SessionData) -> Unit,
    navigateToSessionScreen: () -> Unit
) {
    val sortedSessionDataList = sessionDataList.sortedByDescending { it.session.timestamp }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedSessionDataList) { sessionData ->
            if (sessionData.requests.isNullOrEmpty() || sessionData.webpageContent.isNullOrEmpty() || sessionData.screenshots.isNullOrEmpty()){
                NoCaptiveSessionCard(
                    sessionData = sessionData,
                )
            }else{
                CaptiveSessionCard(
                    sessionData = sessionData,
                    onClick = { onSessionClick(sessionData) },
                    navigateToSessionScreen = navigateToSessionScreen
                )
            }

        }
    }
}

//used to show session details of a network with captive portal
@Composable
fun CaptiveSessionCard(
    sessionData: SessionData,
    onClick: (SessionData) -> Unit,
    navigateToSessionScreen: () -> Unit
) {
    val isRecent = (System.currentTimeMillis() - sessionData.session.timestamp) < 10 * 60 * 1000L
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(sessionData); navigateToSessionScreen() }),
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
                text = "Network Name: ${sessionData.session.ssid}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            sessionData.session.networkId.let {
                Text(stringResource(R.string.network_id, it))
            }
            sessionData.session.timestamp.let {
                Text(stringResource(R.string.date, formatDate(it)))
            }
            Text(stringResource(R.string.requests, sessionData.requests.size))
            Text(stringResource(R.string.webpages, sessionData.webpageContent.size))
            Text(stringResource(R.string.screenshots, sessionData.screenshots.size))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                val icon =
                    if (sessionData.session.isUploadedToRemoteServer) R.drawable.cloud else R.drawable.local
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = if (sessionData.session.isUploadedToRemoteServer) "Cloud session" else "Local session",
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

//used to show session details of a network with no captive portal (normal network)
@Composable
fun NoCaptiveSessionCard(
    sessionData: SessionData,
) {
    val isRecent = (System.currentTimeMillis() - sessionData.session.timestamp) < 10 * 60 * 1000L
    val disabledColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) // Light gray for disabled state
    val textColor = if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = disabledColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Network Name: ${sessionData.session.ssid}",
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            sessionData.session.networkId.let {
                Text(
                    text = stringResource(R.string.network_id, it),
                    color = textColor
                )
            }

            sessionData.session.timestamp.let {
                Text(
                    text = stringResource(R.string.date, formatDate(it)),
                    color = textColor
                )
            }
            val colors = MaterialTheme.colorScheme
                Text(
                    text = stringResource(id = R.string.not_a_captive_portal_network),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp),
                )

            if (isRecent) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.resource_new),
                    contentDescription = "New session",
                    tint = colors.onPrimary,
                    modifier = Modifier
                        .width(96.dp)
                        .height(48.dp)
                )
            }

        }
    }
}


@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(showBackground = true)
@Composable
fun CaptiveSessionCardPreview() {
    AppTheme {

        CaptiveSessionCard(SessionData(
            session = NetworkSessionEntity(
                ssid = "SSID",
                bssid = "BSSID",
                timestamp = System.currentTimeMillis(),
                networkId = UUID.randomUUID().toString(),
                isUploadedToRemoteServer = true,
                captivePortalUrl = "TODO()",
                ipAddress = "192.168.0.2",
                gatewayAddress = "192.168.0.1",
                securityType = "WPA2",
                isCaptiveLocal = false
            ),
            requests = emptyList(),
            webpageContent = emptyList(),
            screenshots = emptyList(),
        ),
            onClick = {},
            navigateToSessionScreen = {}
        )
    }

}


@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(showBackground = true)
@Composable
fun NoCaptiveSessionCardPreview() {
    AppTheme {

        NoCaptiveSessionCard(SessionData(
            session = NetworkSessionEntity(
                ssid = "SSID",
                bssid = "BSSID",
                timestamp = System.currentTimeMillis(),
                networkId = UUID.randomUUID().toString(),
                isUploadedToRemoteServer = true,
                captivePortalUrl = "TODO()",
                ipAddress = "192.168.0.2",
                gatewayAddress = "192.168.0.1",
                securityType = "WPA2",
                isCaptiveLocal = false
            ),
            requests = emptyList(),
            webpageContent = emptyList(),
            screenshots = emptyList(),
        ),
        )
    }

}


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}



