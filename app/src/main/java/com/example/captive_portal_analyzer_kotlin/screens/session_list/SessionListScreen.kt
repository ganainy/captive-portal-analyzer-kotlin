// FILE: SessionListScreen.kt
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
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
 * Composable screen to show a list of network sessions, filtered by Captive or Normal.
 *
 * @param navigateToWelcome Callback invoked when back navigation is triggered.
 * @param updateClickedSessionId Callback to update the ID of the session clicked (for detail view).
 * @param repository Repository instance for data access.
 * @param navigateToSessionScreen Callback to navigate to the session detail screen.
 * @param mainViewModel ViewModel for global state management (like dialogs).
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

    // Observe ViewModel states
    val uiState by sessionListViewModel.uiState.collectAsState()
    val filteredSessionDataList by sessionListViewModel.filteredSessionDataList.collectAsState()
    val selectedFilter by sessionListViewModel.selectedFilter.collectAsState()

    // Handle back press
    BackHandler {
        navigateToWelcome()
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Filter Tabs Row - Only Captive and Normal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp), // Consistent height for tabs
                horizontalArrangement = Arrangement.SpaceAround, // Evenly space the two tabs
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterTab(
                    filterType = SessionFilterType.CAPTIVE,
                    currentFilter = selectedFilter,
                    onFilterChange = sessionListViewModel::selectFilter // Pass ViewModel function directly
                )
                FilterTab(
                    filterType = SessionFilterType.NORMAL,
                    currentFilter = selectedFilter,
                    onFilterChange = sessionListViewModel::selectFilter
                )
            }

            Divider() // Separator below tabs

            // Prepare confirmation dialog strings
            val deleteTitle = stringResource(R.string.delete_session_confirmation_title)
            val deleteMessage = stringResource(R.string.delete_session_confirmation_message)
            val deleteConfirmText = stringResource(R.string.delete)

            // Display content based on UI state and filtered list
            SessionsContent(
                uiState = uiState,
                sessionDataList = filteredSessionDataList, // Pass the already filtered list
                currentFilter = selectedFilter, // Pass current filter for context
                navigateToSessionScreen = navigateToSessionScreen,
                updateClickedSessionId = updateClickedSessionId,
                onDeleteClick = { networkId ->
                    // Show confirmation dialog via MainViewModel
                    mainViewModel.showDialog(
                        title = deleteTitle,
                        message = deleteMessage,
                        confirmText = deleteConfirmText,
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

/**
 * Composable representing a single filter tab (Captive or Normal).
 *
 * @param filterType The type this tab represents (CAPTIVE or NORMAL).
 * @param currentFilter The currently selected filter in the ViewModel.
 * @param onFilterChange Callback invoked when this tab is clicked.
 */
@Composable
fun FilterTab(
    filterType: SessionFilterType,
    currentFilter: SessionFilterType,
    onFilterChange: (SessionFilterType) -> Unit
) {
    val isSelected = currentFilter == filterType
    val label = when (filterType) {
        SessionFilterType.CAPTIVE -> stringResource(R.string.filter_captive)
        SessionFilterType.NORMAL -> stringResource(R.string.filter_normal)
    }
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge

    Box( // Use Box for better click area and alignment
        modifier = Modifier
            .fillMaxHeight() // Fill height of the parent Row
            .clickable { onFilterChange(filterType) }
            .padding(horizontal = 16.dp), // Padding inside the clickable area
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = textStyle,
            color = textColor
        )
    }
}

/**
 * Displays the main content area based on the overall UI state and the filtered list.
 *
 * @param uiState Overall loading/error/success state from the ViewModel.
 * @param sessionDataList The list of sessions *already filtered* by the ViewModel.
 * @param currentFilter The currently selected filter (for displaying context-specific empty messages).
 * @param navigateToSessionScreen Callback to navigate to session details.
 * @param updateClickedSessionId Callback to update the clicked session ID.
 * @param onDeleteClick Callback invoked when the delete icon on an item is clicked.
 */
@Composable
private fun SessionsContent(
    uiState: SessionListUiState,
    sessionDataList: List<SessionData>?,
    currentFilter: SessionFilterType,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    onDeleteClick: (networkId: String) -> Unit
) {
    when (uiState) {
        SessionListUiState.Loading -> LoadingIndicator()
        is SessionListUiState.Error -> ErrorComponent(stringResource(uiState.messageStringResource))
        SessionListUiState.Empty -> EmptyStateIndicator() // Show general empty state if NO sessions exist at all
        SessionListUiState.Success -> {
            // Data loading succeeded, now display the filtered list or an empty message for the *current filter*
            SessionsSuccessContent(
                sessionDataList = sessionDataList,
                currentFilter = currentFilter,
                navigateToSessionScreen = navigateToSessionScreen,
                updateClickedSessionId = updateClickedSessionId,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

/**
 * Displays the list of sessions when data loading was successful.
 * Handles the case where the *filtered* list might be empty.
 *
 * @param sessionDataList The list of sessions *already filtered* by the ViewModel. Can be null initially or empty.
 * @param currentFilter The currently selected filter (for displaying context-specific empty messages).
 * @param navigateToSessionScreen Callback to navigate to session details.
 * @param updateClickedSessionId Callback to update the clicked session ID.
 * @param onDeleteClick Callback invoked when the delete icon on an item is clicked.
 */
@Composable
private fun SessionsSuccessContent(
    sessionDataList: List<SessionData>?,
    currentFilter: SessionFilterType,
    navigateToSessionScreen: () -> Unit,
    updateClickedSessionId: (String) -> Unit,
    onDeleteClick: (networkId: String) -> Unit
) {
    // Check if the filtered list is empty for the *current* filter
    if (sessionDataList.isNullOrEmpty()) {
        EmptyFilteredListIndicator(filterType = currentFilter)
    } else {
        // Filtered list has items, display the list
        Box(modifier = Modifier.fillMaxSize()) {
            SessionsList(
                sessionDataList = sessionDataList, // Pass the non-empty, filtered list
                onSessionClick = { clickedSession ->
                    // Only captive portals are clickable to navigate
                    if(clickedSession.isCaptivePortal()){
                        updateClickedSessionId(clickedSession.session.networkId)
                        navigateToSessionScreen()
                    }
                },
                onDeleteClick = onDeleteClick
            )
        }
    }
}

/**
 * Displays an indicator when the overall session list is completely empty.
 */
@Composable
fun EmptyStateIndicator() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "\\(o_o)/",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_sessions_detected_explanation),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays an indicator when the list is empty *for the currently selected filter*.
 * @param filterType The filter type for which no sessions were found.
 */
@Composable
fun EmptyFilteredListIndicator(filterType: SessionFilterType) {
    val messageResId = when (filterType) {
        SessionFilterType.CAPTIVE -> R.string.no_captive_sessions_found
        SessionFilterType.NORMAL -> R.string.no_normal_sessions_found
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "(·_·)", // Simple indicator
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/**
 * Displays the actual list of sessions using LazyColumn.
 * Assumes `sessionDataList` is not empty.
 *
 * @param sessionDataList The non-empty, filtered list of session data.
 * @param onSessionClick Lambda called when a session item is clicked.
 * @param onDeleteClick Lambda called when the delete icon for a session is clicked.
 */
@Composable
fun SessionsList(
    sessionDataList: List<SessionData>,
    onSessionClick: (SessionData) -> Unit,
    onDeleteClick: (networkId: String) -> Unit
) {
    // Sort by timestamp descending before displaying
    val sortedSessionDataList = remember(sessionDataList) {
        sessionDataList.sortedByDescending { it.session.timestamp }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp) // Add vertical padding for the list
    ) {
        itemsIndexed(
            items = sortedSessionDataList,
            key = { _, item -> item.session.networkId } // Stable keys for performance
        ) { index, sessionData ->

            val isCaptive = sessionData.isCaptivePortal()

            NetworkSessionItem(
                sessionData = sessionData,
                isCaptivePortal = isCaptive,
                // Click action passed down, triggers navigation only if captive
                onClick = { onSessionClick(sessionData) },
                onDeleteClick = onDeleteClick // Pass delete callback
            )

            // Add Divider between items
            if (index < sortedSessionDataList.lastIndex) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // Indent divider slightly
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp)) // Add space after last item
            }
        }
    }
}

/**
 * Composable for displaying a single network session item in the list.
 * Handles distinct styling for captive vs. normal sessions.
 *
 * @param sessionData The data for the session item.
 * @param isCaptivePortal Boolean indicating if this session is classified as captive.
 * @param onClick Lambda called when the item is clicked (primarily for captive portals).
 * @param onDeleteClick Lambda called when the delete icon is clicked.
 */
@Composable
fun NetworkSessionItem(
    sessionData: SessionData,
    isCaptivePortal: Boolean,
    onClick: (SessionData) -> Unit,
    onDeleteClick: (networkId: String) -> Unit
) {
    // Determine colors based on captive status
    val titleTextColor = if (isCaptivePortal) {
        MaterialTheme.colorScheme.primary // More prominent for captive
    } else {
        MaterialTheme.colorScheme.onSurface // Standard for normal
    }
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant // Consistent secondary color

    // Base modifier, applying padding and click handling
    val itemModifier = Modifier
        .fillMaxWidth()
        .then(
            // Only truly clickable (for navigation) if captive portal
            if (isCaptivePortal) {
                Modifier.clickable { onClick(sessionData) }
            } else Modifier // Not clickable for navigation if normal
        )
        .padding(horizontal = 16.dp, vertical = 12.dp) // Consistent padding within the item

    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.Top, // Align content to top, delete button aligns itself
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Main content column (takes available space)
        Column(
            modifier = Modifier
                .weight(1f) // Occupy remaining space before delete icon
                .padding(end = 8.dp), // Space before delete icon
            verticalArrangement = Arrangement.spacedBy(6.dp) // Space between elements in column
        ) {
            // Network Name (SSID)
            Text(
                text = sessionData.session.ssid ?: stringResource(R.string.unknown_network),
                style = MaterialTheme.typography.titleMedium,
                color = titleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // IP and Gateway Addresses
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NetworkDetailRow(
                    text = stringResource(
                        R.string.ip_address_format,
                        sessionData.session.ipAddress ?: stringResource(R.string.unknown)
                    ),
                    color = secondaryTextColor
                )
                NetworkDetailRow(
                    text = stringResource(
                        R.string.gateway_address_format,
                        sessionData.session.gatewayAddress ?: stringResource(R.string.unknown)
                    ),
                    color = secondaryTextColor
                )
            }

            // Captive Portal specific stats (only show if captive)
            if (isCaptivePortal) {
                NetworkDetailRow(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.requests, sessionData.requestsCount))
                        append(" • ")
                        append(stringResource(R.string.webpages, sessionData.webpageContentCount))
                        append(" • ")
                        append(stringResource(R.string.screenshots, sessionData.screenshotsCount))
                    }.toString(),
                    color = secondaryTextColor.copy(alpha = 0.8f) // Slightly lighter/dimmed
                )
            }

            // Creation Time (Time Ago)
            HintTextWithIcon(
                hint = TimeAgo.using(sessionData.session.timestamp), // Just the time ago string
                iconResId = R.drawable.clock,
                tint = secondaryTextColor
            )
        }

        // Delete Button (always present)
        // Use IconButton for proper accessibility and touch target size
        IconButton(
            onClick = { onDeleteClick(sessionData.session.networkId) },
            modifier = Modifier
                .size(40.dp) // Standard touch target size
                .offset(y = (-4).dp) // Slightly adjust vertical position if needed
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete_session_content_desc, sessionData.session.ssid ?: ""),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f) // Less intense error color
            )
        }
    }
}

/** Helper composable for consistent display of detail rows (IP, Gateway, Stats). */
@Composable
fun NetworkDetailRow(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Helper extension function (can be moved to dataclasses if preferred) */
fun SessionData.isCaptivePortal(): Boolean {
    return this.requestsCount > 0 || this.webpageContentCount > 0 || this.screenshotsCount > 0
}


// --- Previews ---

// Helper function remains the same
private fun createMockSessionData(
    isCaptive: Boolean,
    isUploaded: Boolean = false,
    isRecent: Boolean = false,
    reqCount: Int = if (isCaptive) 5 else 0,
    webCount: Int = if (isCaptive) 2 else 0,
    scrCount: Int = if (isCaptive) 3 else 0,
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
            isCaptiveLocal = false
        ),
        requestsCount = reqCount,
        webpageContentCount = webCount,
        screenshotsCount = scrCount,
        requests = emptyList(), webpageContent = emptyList(), screenshots = emptyList()
    )
}

// Preview Captive Item
@Preview(name = "Captive Item - Light", showBackground = true, widthDp = 380)
@Composable
fun PreviewCaptiveSessionItem_Light() {
    AppTheme {
        Surface {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = true, isRecent = true),
                isCaptivePortal = true,
                onClick = {},
                onDeleteClick = {}
            )
        }
    }
}

// Preview Normal Item
@Preview(name = "Normal Item - Light", showBackground = true, widthDp = 380)
@Composable
fun PreviewNormalSessionItem_Light() {
    AppTheme {
        Surface {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = false),
                isCaptivePortal = false,
                onClick = {}, // Click does nothing for normal items visually
                onDeleteClick = {}
            )
        }
    }
}

// Preview Captive Item (Dark)
@Preview(name = "Captive Item - Dark", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCaptiveSessionItem_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = true, isRecent = false, isUploaded = true),
                isCaptivePortal = true,
                onClick = {},
                onDeleteClick = {}
            )
        }
    }
}

// Preview Normal Item (Dark)
@Preview(name = "Normal Item - Dark", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewNormalSessionItem_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            NetworkSessionItem(
                sessionData = createMockSessionData(isCaptive = false, isRecent = true),
                isCaptivePortal = false,
                onClick = {},
                onDeleteClick = {}
            )
        }
    }
}

// Preview Empty Filtered State
@Preview(name = "Empty Filtered (Captive) - Light", showBackground = true, heightDp = 200)
@Composable
fun PreviewEmptyFilteredCaptive() {
    AppTheme {
        Surface(Modifier.fillMaxSize()) {
            EmptyFilteredListIndicator(filterType = SessionFilterType.CAPTIVE)
        }
    }
}

// Preview Empty Filtered State (Dark)
@Preview(name = "Empty Filtered (Normal) - Dark", showBackground = true, heightDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEmptyFilteredNormal_Dark() {
    AppTheme(darkTheme=true) {
        Surface(Modifier.fillMaxSize()) {
            EmptyFilteredListIndicator(filterType = SessionFilterType.NORMAL)
        }
    }
}

// Preview Overall Empty State
@Preview(name = "Empty State (No Sessions) - Light", showBackground = true, heightDp = 300)
@Composable
fun PreviewEmptyStateOverall() {
    AppTheme {
        Surface(Modifier.fillMaxSize()) {
            EmptyStateIndicator()
        }
    }
}