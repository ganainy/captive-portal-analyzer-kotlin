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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.IMainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.github.marlonlom.utilities.timeago.TimeAgo
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Composable screen to show a list of network sessions.
 *
 * @param navigateToWelcome a callback that is called when the user clicks the back button
 * @param updateClickedSessionId a callback that is called when the user clicks on a session
 * @param repository the repository that provides the data for the sessions
 * @param navigateToSessionScreen a callback that is called when the user clicks on a session
 * @param mainViewModel ViewModel to handle global state like dialogs
 */
@Composable
fun SessionListScreen(
    navigateToWelcome: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    repository: NetworkSessionRepository,
    navigateToSessionScreen: () -> Unit,
    mainViewModel: IMainViewModel
) {
    val sessionListViewModel: SessionListViewModel = viewModel(
        factory = SessionListViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
        )
    )

    val uiState by sessionListViewModel.uiState.collectAsState()
    val sessionDataList by sessionListViewModel.sessionDataList.collectAsState()

    // State for filtering
    val (filter, setFilter) = remember { mutableStateOf("All") }

    BackHandler {
        navigateToWelcome()
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterButton("All", filter, setFilter)
                FilterButton("Captive", filter, setFilter)
                FilterButton("Normal", filter, setFilter)
            }

            // Filtered content
            val filteredList = when (filter) {
                "Captive" -> sessionDataList?.filter { it.requestsCount > 0 || it.webpageContentCount > 0 || it.screenshotsCount > 0 }
                "Normal" -> sessionDataList?.filter { it.requestsCount == 0 && it.webpageContentCount == 0 && it.screenshotsCount == 0 }
                else -> sessionDataList
            }

            val title = stringResource(R.string.delete_session_confirmation_title)
            val message = stringResource(R.string.delete_session_confirmation_message)
            val confirmText = stringResource(R.string.delete)

            SessionsContent(
                uiState = uiState,
                sessionDataList = filteredList,
                navigateToSessionScreen = navigateToSessionScreen,
                updateClickedSessionId = updateClickedSessionId,
                onDeleteClick = { networkId ->
                    mainViewModel.showDialog(
                        title = title,
                        message = message,
                        confirmText = confirmText,
                        onConfirm = {
                            sessionListViewModel.deleteSession(networkId)
                            mainViewModel.hideDialog()
                        },
                        onDismiss = { mainViewModel.hideDialog() }
                    )
                }
            )
        }
    }
}

@Composable
fun FilterButton(
    label: String,
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .clickable { onFilterChange(label) }
            .padding(8.dp),
        style = if (currentFilter == label) MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
        else MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

/**
 * A composable function that displays a list of sessions based on the [uiState].
 *
 * @param uiState the ui state that determines what to display
 * @param sessionDataList the list of session data to display
 * @param navigateToSessionScreen a callback that is called when the user clicks on a session
 * @param updateClickedSessionId a callback that is called when the user clicks on a session
 * @param onDeleteClick Callback when delete icon is clicked for a session
 */
@Composable
private fun SessionsContent(
    uiState: SessionListUiState,
    sessionDataList: List<SessionData>?,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    onDeleteClick: (networkId: String) -> Unit // <-- Pass delete callback down
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
        // ---  Empty State ---
        SessionListUiState.Empty -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large ASCII Art / Emoji
                Text(
                    text = "\\(o_o)/",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp)) // Space between emoji and text

                // Explanatory Text
                Text(
                    text = stringResource(R.string.no_sessions_detected_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // --- End  Empty State ---

        // show the list of sessions if there are sessions
        SessionListUiState.Success -> {
            // show the list of sessions
            SessionsSuccessContent(
                sessionDataList = sessionDataList,
                navigateToSessionScreen = navigateToSessionScreen,
                updateClickedSessionId = updateClickedSessionId,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

/**
 * A composable function that displays a list of sessions when the data is successfully loaded.
 *
 * @param sessionDataList The list of session data to display.
 * @param navigateToSessionScreen A lambda function to navigate to the session screen.
 * @param updateClickedSessionId A lambda function to update the clicked session ID.
 * @param onDeleteClick Callback when delete icon is clicked for a session
 */
@Composable
private fun SessionsSuccessContent(
    sessionDataList: List<SessionData>?,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    onDeleteClick: (networkId: String) -> Unit
) {
    sessionDataList?.let { dataList ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SessionsList(
                sessionDataList = dataList,
                onSessionClick = { clickedSession ->
                    updateClickedSessionId(clickedSession.session.networkId)
                },
                onDeleteClick = onDeleteClick, // <-- Pass delete callback down
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
 * @param onDeleteClick Callback when delete icon is clicked for a session
 */
@Composable
fun SessionsList(
    sessionDataList: List<SessionData>,
    onSessionClick: (SessionData) -> Unit,
    navigateToSessionScreen: () -> Unit,
    onDeleteClick: (networkId: String) -> Unit
) {
    val sortedSessionDataList = sessionDataList.sortedByDescending { it.session.timestamp }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(
            items = sortedSessionDataList,
            key = { _, item -> item.session.networkId }
        ) { index, sessionData ->

            // Determine if captive for logic within the item
            val isCaptive = sessionData.requestsCount > 0 || sessionData.webpageContentCount > 0 || sessionData.screenshotsCount > 0

            // Use the new NetworkSessionItem
            NetworkSessionItem(
                sessionData = sessionData,
                isCaptivePortal = isCaptive,
                onClick = if (isCaptive) { { onSessionClick(sessionData) } } else null,
                navigateToSessionScreen = if (isCaptive) navigateToSessionScreen else null,
                onDeleteClick = onDeleteClick
            )

            // Add Divider after each item except the last one
            if (index < sortedSessionDataList.lastIndex) {
                Divider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
            }
        }
    }
}

/**
 * A composable function that displays a card for a network session.
 *
 * @param sessionData The session data to display.
 * @param isCaptivePortal Whether the session is from a captive portal network (determines clickability and delete icon).
 * @param onClick Optional callback for when the card is clicked (only enabled if captive).
 * @param onDeleteClick Callback for when the delete icon is clicked.
 * @param navigateToSessionScreen Optional callback to navigate to session screen (called via onClick if captive).
 */
@Composable
fun NetworkSessionItem(
    sessionData: SessionData,
    isCaptivePortal: Boolean,
    onClick: ((SessionData) -> Unit)? = null,
    onDeleteClick: (networkId: String) -> Unit,
    navigateToSessionScreen: (() -> Unit)? = null
) {

   // Determine the text color for the main title (greyed out if not clickable)
   val titleTextColor = if (isCaptivePortal) {
       MaterialTheme.colorScheme.onSurface // Standard color for clickable
   } else {
       Color.Gray // Greyed-out color for non-clickable
   }

    // Determine the text color for secondary info (stats, time)
    // Stats only show for captive portals, time shows for both.
    // Using onSurfaceVariant for secondary info in both states works well:
    // - For captive portal: It's a standard subdued color for secondary details.
    // - For non-captive portal: It's the greyed-out color, consistent with the title.
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant


    // Use a Row to place the highlight bar next to the content
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top // Align content to the top
    ) {
        // Subtle Highlight Bar (only visible for captive portal)
        Box(
            modifier = Modifier
                .width(2.dp) // Thin bar
                .fillMaxHeight() // Takes height of the item content
        )

        // Main content area (clickable for captive portals, padded, and weighted)
        val contentModifier = Modifier
            .weight(1f) // Takes remaining space
            .then( // Apply clickable *before* padding usually works well
                // Only make the item clickable if it's a captive portal AND click handlers are provided
                // Non-captive portal items are not clickable in this view.
                if (isCaptivePortal && onClick != null && navigateToSessionScreen != null) {
                    Modifier.clickable {
                        onClick(sessionData)
                        navigateToSessionScreen()
                    }
                } else Modifier
            )
            .padding(vertical = 16.dp, horizontal = 8.dp) // Apply padding to the content

        // Inner Row for arranging text content and delete button
        Row(
            modifier = contentModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top // Align content inside to top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f) // Column takes priority space
                    .padding(end = 8.dp), // Add padding before the delete button
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Network Name (SSID) - Color is conditional
                Text(
                    text = stringResource(
                        R.string.network_name,
                        sessionData.session.ssid ?: stringResource(R.string.unknown_network)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = titleTextColor, // Apply the conditional color
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Captive Portal specific stats (Requests, Webpages, Screenshots) - Only shown for captive portals
                if (isCaptivePortal) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.requests, sessionData.requestsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor // Use the secondary color
                        )
                        Text(
                            stringResource(R.string.webpages, sessionData.webpageContentCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor // Use the secondary color
                        )
                        Text(
                            stringResource(R.string.screenshots, sessionData.screenshotsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor // Use the secondary color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Creation Time - Text and icon color use the secondary color
               HintTextWithIcon(
                   hint = stringResource(
                       R.string.created,
                       TimeAgo.using(sessionData.session.timestamp)
                   ),
                   iconResId = R.drawable.clock, // Ensure this drawable exists
                   tint = if (isCaptivePortal) secondaryTextColor else Color.Gray
               )
            }

            // Delete Button (only for Captive Portal sessions)
            if (isCaptivePortal) {
                IconButton(
                    onClick = { onDeleteClick(sessionData.session.networkId) },
                    modifier = Modifier
                        .size(40.dp) // Standard icon button size
                        .align(Alignment.CenterVertically) // Vertically center the button
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_session),
                        tint = MaterialTheme.colorScheme.error // Use error color for delete icon
                    )
                }
            }
        }
    }
}


// --- Previews ---

// Helper function to create mock session data for previews
private fun createMockSessionData(
    isCaptive: Boolean,
    isUploaded: Boolean = false,
    isRecent: Boolean = false,
    reqCount: Int = if(isCaptive) 5 else 0,
    webCount: Int = if(isCaptive) 2 else 0,
    scrCount: Int = if(isCaptive) 3 else 0,
    ssid: String = "Mock WiFi ${if (isCaptive) "(Captive)" else ""}"
): SessionData {
    val timestamp = if (isRecent) System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10)
    else System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
    return SessionData(
        session = NetworkSessionEntity(
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            timestamp = timestamp,
            networkId = UUID.randomUUID().toString(),
            isUploadedToRemoteServer = isUploaded,
            captivePortalUrl = if (isCaptive) "http://portal.example.com" else null,
            ipAddress = "192.168.1.100",
            gatewayAddress = "192.168.1.1",
            isCaptiveLocal = false // Example value
        ),
        requestsCount = reqCount,
        webpageContentCount = webCount,
        screenshotsCount = scrCount,
        requests = emptyList(), // Not needed for card preview
        webpageContent = emptyList(),
        screenshots = emptyList()
    )
}


@Preview(name="Captive Card - Recent, Not Uploaded", showBackground = true, widthDp = 380)
@Composable
fun PreviewCaptiveSessionCard_RecentNotUploaded() {
    AppTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = true, isUploaded = false, isRecent = true),
                isCaptivePortal = true,
                onClick = {},
                navigateToSessionScreen = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(name="Captive Card - Old, Uploaded", showBackground = true, widthDp = 380)
@Composable
fun PreviewCaptiveSessionCard_OldUploaded() {
    AppTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = true, isUploaded = true, isRecent = false),
                isCaptivePortal = true,
                onClick = {},
                navigateToSessionScreen = {},
                onDeleteClick = {}
            )
        }
    }
}


@Preview(name="Non-Captive Card", showBackground = true, widthDp = 380)
@Composable
fun PreviewNonCaptiveSessionCard() {
    AppTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = false),
                isCaptivePortal = false,
                onClick = {}, // Should be null in real usage
                navigateToSessionScreen =  {}, // Should be null in real usage
                onDeleteClick = {} // Delete likely disabled or hidden here
            )
        }
    }
}

@Preview(name="Captive Card (Dark)", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCaptiveSessionCard_Dark() {
    AppTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(8.dp)) {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = true, isUploaded = false, isRecent = true),
                isCaptivePortal = true,
                onClick = {},
                navigateToSessionScreen = {},
                onDeleteClick = {}
            )
        }
    }
}

