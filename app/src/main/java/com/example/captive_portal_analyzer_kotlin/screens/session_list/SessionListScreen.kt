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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import java.util.UUID
import java.util.concurrent.TimeUnit


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
 * @param navigateToSessionScreen a callback that is called when the user clicks on a session
 * @param updateClickedSessionId a callback that is called when the user clicks on a session
 */
@Composable
private fun SessionsContent(
    uiState: SessionListUiState,
    sessionDataList: List<SessionData>?,
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
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit
) {
    sessionDataList?.let { sessionDataList ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            if (sessionData.requestsCount == 0 && sessionData.webpageContentCount == 0 && sessionData.screenshotsCount == 0) {
                // if a session doesn't have any requests, webpage content or screenshots, show a NoCaptiveSessionCard
                NetworkSessionCard(
                    sessionData = sessionData,
                    isCaptivePortal = false,
                    onClick = {},
                    navigateToSessionScreen = {}
                )
            } else {
                // else show a CaptiveSessionCard
                NetworkSessionCard(
                    sessionData = sessionData,
                    onClick = { onSessionClick(sessionData) },
                    navigateToSessionScreen = navigateToSessionScreen,
                    isCaptivePortal = true
                )
            }

        }
    }
}

/**
 * A composable function that displays a card for a network session.
 *
 * @param sessionData The session data to display.
 * @param isCaptivePortal Whether the session is from a captive portal network.
 * @param onClick Optional callback for when the card is clicked (only for captive portal sessions).
 * @param navigateToSessionScreen Optional callback to navigate to session screen (only for captive portal sessions).
 */
@Composable
fun NetworkSessionCard(
    sessionData: SessionData,
    isCaptivePortal: Boolean,
    onClick: ((SessionData) -> Unit)? = null,
    navigateToSessionScreen: (() -> Unit)? = null
) {
    val isRecent = remember(sessionData.session.timestamp) {
        (System.currentTimeMillis() - sessionData.session.timestamp) < TimeUnit.MINUTES.toMillis(30)
    }
    val isRecentDuration = 30

    val currentLocale = LocalContext.current.resources.configuration.locales[0]
    val timeAgoMessages = remember(currentLocale) {
        TimeAgoMessages.Builder().withLocale(currentLocale).build()
    }

    val cardColors = when {
        isCaptivePortal -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val textColor = when {
        isCaptivePortal -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCaptivePortal) {
                    Modifier.clickable {
                        onClick?.invoke(sessionData)
                        navigateToSessionScreen?.invoke()
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = cardColors),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCaptivePortal) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Network name
            Text(
                text = stringResource(
                    R.string.network_name,
                    sessionData.session.ssid ?: "Unknown Network"
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Only show these for captive portal sessions
            if (isCaptivePortal) {
                Text(stringResource(R.string.requests, sessionData.requestsCount))
                Text(stringResource(R.string.webpages, sessionData.webpageContentCount))
                Text(stringResource(R.string.screenshots, sessionData.screenshotsCount))
            }

            // Timestamp
            HintTextWithIcon(
                hint = stringResource(
                    R.string.created,
                    TimeAgo.using(sessionData.session.timestamp, timeAgoMessages)
                ),
                iconResId = R.drawable.clock,
            )

            // Status indicators for captive portal
            if (isCaptivePortal) {
                HintTextWithIcon(
                    hint = if (sessionData.session.isUploadedToRemoteServer)
                        stringResource(R.string.thanks_for_uploading_network_data_for_further_analysis)
                    else
                        stringResource(R.string.please_review_and_upload_collected_data_to_help_us_research_this_network),
                    iconResId = if (sessionData.session.isUploadedToRemoteServer)
                        R.drawable.cloud else R.drawable.local
                )
            } else {
                //  indicator for non-captive portal network card
                HintTextWithIcon(hint = stringResource(R.string.not_a_captive_portal_network))
            }

            if (isRecent) {
                HintTextWithIcon(
                    hint = stringResource(R.string.this_is_new_session, isRecentDuration),
                    iconResId = R.drawable.resource_new,
                )
            }
        }
    }
}

    /**
     * Preview composable for a captive session card in light and dark modes.
     * @see CaptiveSessionCard
     */
    @Composable
    @Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
    @Preview(
        showBackground = true,
        device = "spec:width=1280dp,height=800dp,dpi=240",
        name = "tablet",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    fun CaptiveSessionCardPreview() {
        AppTheme {
            // example of a mock captive session card
            NetworkSessionCard(
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
                navigateToSessionScreen = {},
                isCaptivePortal = true
            )
        }
    }

    /**
     * Preview composable for a no captive session card in light and dark modes.
     * @see NoCaptiveSessionCard
     */
    @Composable
    @Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
    @Preview(
        showBackground = true,
        device = "spec:width=1280dp,height=800dp,dpi=240",
        name = "tablet",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    fun NoCaptiveSessionCardPreview() {
        AppTheme {

            NetworkSessionCard(
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
                isCaptivePortal = false,
                onClick = {},
                navigateToSessionScreen =  {},
            )
        }

    }


