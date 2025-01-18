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
import com.example.captive_portal_analyzer_kotlin.utils.Utils
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.formatDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
/**
 * Composable screen to show a list of network sessions.
 *
 * @param navigateToWelcome a callback that is called when the user clicks the back button
 * @param updateClickedSessionId a callback that is called when the user clicks on a session
 * @param repository the repository that provides the data for the sessions
 * @param navigateToSessionScreen a callback that is called when the user clicks on a session
 */
@Composable
fun SessionListScreen(
    navigateToWelcome: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    repository: NetworkSessionRepository,
    navigateToSessionScreen: () -> Unit
) {
    // Create the view model for the session list screen
    val sessionListViewModel: SessionListViewModel = viewModel(
        factory = SessionListViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
        )
    )

    // Collect the ui state and session data list from the view model as state
    val uiState by sessionListViewModel.uiState.collectAsState()
    val sessionDataList by sessionListViewModel.sessionDataList.collectAsState()

    // Handle back button press to navigate to the welcome screen instead of the last screen
    BackHandler {
        navigateToWelcome()
    }

    // Create the scaffold for the screen and pass the padding values and content to it
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
/**
 * A composable function that displays a list of sessions based on the [uiState].
 *
 * @param uiState the ui state that determines what to display
 * @param sessionDataList the list of session data to display
 * @param paddingValues the padding values for the composable
 * @param navigateToSessionScreen a callback that is called when the user clicks on a session
 * @param updateClickedSessionId a callback that is called when the user clicks on a session
 */
@Composable
private fun SessionsContent(
    uiState: SessionListUiState,
    sessionDataList: List<SessionData>?,
    paddingValues: PaddingValues,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit
) {
    // Handle different ui states
    when (uiState) {
        // show loading indicator when loading sessions
        SessionListUiState.Loading -> {
            LoadingIndicator()
        }

        // show error message if there is an error
        is SessionListUiState.Error -> {
            // show error text from the ui state
            ErrorComponent(stringResource((uiState as SessionListUiState.Error).messageStringResource))
        }

        // show empty list text if there are no sessions
        SessionListUiState.Empty -> {
            // show text that there are no sessions
            ErrorComponent(stringResource(R.string.no_sessions_detected))
        }

        // show the list of sessions if there are sessions
        SessionListUiState.Success -> {
            // show the list of sessions
            SessionsSuccessContent(
                sessionDataList = sessionDataList,
                paddingValues = paddingValues,
                navigateToSessionScreen = navigateToSessionScreen,
                updateClickedSessionId = updateClickedSessionId
            )
        }
    }
}

/**
 * A composable function that displays a list of sessions when the data is successfully loaded.
 *
 * @param sessionDataList The list of session data to display.
 * @param paddingValues The padding values to apply to the content.
 * @param navigateToSessionScreen A lambda function to navigate to the session screen.
 * @param updateClickedSessionId A lambda function to update the clicked session ID.
 */
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

/**
 * A composable function that displays a list of sessions. The list is sorted in descending order by session timestamp.
 *
 * @param sessionDataList The list of session data to display.
 * @param onSessionClick A lambda function to call when a session is clicked.
 * @param navigateToSessionScreen A lambda function to navigate to the session screen.
 */
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
            if (sessionData.requests.isNullOrEmpty() || sessionData.webpageContent.isNullOrEmpty() || sessionData.screenshots.isNullOrEmpty()) {
                // if a session doesn't have any requests, webpage content or screenshots, show a NoCaptiveSessionCard
                NoCaptiveSessionCard(
                    sessionData = sessionData,
                )
            } else {
                // else show a CaptiveSessionCard
                CaptiveSessionCard(
                    sessionData = sessionData,
                    onClick = { onSessionClick(sessionData) },
                    navigateToSessionScreen = navigateToSessionScreen
                )
            }

        }
    }
}

/**
 * A composable function that displays a clickable card for a captive session.
 *
 * @param sessionData The session data to display.
 * @param onClick A lambda function to call when the card is clicked.
 * @param navigateToSessionScreen A lambda function to navigate to the session screen.
 */
@Composable
fun CaptiveSessionCard(
    sessionData: SessionData,
    onClick: (SessionData) -> Unit,
    navigateToSessionScreen: () -> Unit
) {
    // Check if the session is recent (created in latest 10 minutes)
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

            // Show network name
            Text(
                text = "Network Name: ${sessionData.session.ssid}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Show network ID
            sessionData.session.networkId.let {
                Text(stringResource(R.string.network_id, it))
            }

            // Show date
            sessionData.session.timestamp.let {
                Text(stringResource(R.string.date, formatDate(it)))
            }

            // Show number of requests
            Text(stringResource(R.string.requests, sessionData.requests.size))

            // Show number of webpages
            Text(stringResource(R.string.webpages, sessionData.webpageContent.size))

            // Show number of screenshots
            Text(stringResource(R.string.screenshots, sessionData.screenshots.size))

            // Show indicator if session is remote or new using different icons
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

                //show indicator if session is recent by giving it a new icon and different color
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
/**
 * A composable that shows a card for a session that does not have a captive portal aka normal network
 * with full internet access.
 *
 * @param sessionData the session data associated with the network, including the network session,
 * webpages and screenshots
 */
@Composable
fun NoCaptiveSessionCard(
    sessionData: SessionData,
) {
    //flag to check if the session is recent (created in latest 10 minutes)
    val isRecent = (System.currentTimeMillis() - sessionData.session.timestamp) < 10 * 60 * 1000L
    //give greyed out color for the card and text to indicate that it is not clickable
    val disabledColor =
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textColor =
        if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.6f
        )

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
            // show network name
            sessionData.session.ssid.let {
            Text(
                text = stringResource(R.string.network_name, it?:"Unknown Network"),
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            }
            // show network id
            sessionData.session.networkId.let {
                Text(
                    text = stringResource(R.string.network_id, it),
                    color = textColor
                )
            }

            // show session timestamp
            sessionData.session.timestamp.let {
                Text(
                    text = stringResource(R.string.date, Utils.formatDate(it)),
                    color = textColor
                )
            }
            val colors = MaterialTheme.colorScheme
            // show a hint that this is not a captive portal
            Text(
                text = stringResource(id = R.string.not_a_captive_portal_network),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp),
            )

            // show indicator if session is recent
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

/**
 * Preview composable for a captive session card in light and dark modes.
 * @see CaptiveSessionCard
 */
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(showBackground = true)
@Composable
fun CaptiveSessionCardPreview() {
    AppTheme {
        // example of a mock captive session card
        CaptiveSessionCard(
            SessionData(
                // session data
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
                // empty list of requests
                requests = emptyList(),
                // empty list of webpage content
                webpageContent = emptyList(),
                // empty list of screenshots
                screenshots = emptyList(),
            ),
            onClick = {},
            navigateToSessionScreen = {}
        )
    }
}

/**
 * Preview composable for a no captive session card in light and dark modes.
 * @see NoCaptiveSessionCard
 */
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(showBackground = true)
@Composable
fun NoCaptiveSessionCardPreview() {
    AppTheme {

        NoCaptiveSessionCard(
            SessionData(
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


