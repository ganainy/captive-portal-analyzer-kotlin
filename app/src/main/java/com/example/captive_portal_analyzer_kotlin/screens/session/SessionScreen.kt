package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.CustomChip
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.RequestMethodView
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.AppUtils.Companion.formatDate

// region Main Screen Composable
/**
 * Composable function to display the session details of a network with captive portal.
 *
 * @param mainViewModel The shared view model containing the clicked session ID and
 * connectivity status.
 * @param repository The repository to access network session data.
 * @param navigateToAutomaticAnalysis A function that navigates to the automatic analysis screen.
 */
@Composable
fun SessionScreen(
    mainViewModel: MainViewModel,
    repository: NetworkSessionRepository,
    navigateToAutomaticAnalysis: () -> Unit,
    navigateToWebpageContentScreen: () -> Unit,
    navigateToRequestDetailsScreen: () -> Unit
) {
    // Collect the clicked session ID from the shared view model.
    val clickedSessionId by mainViewModel.clickedSessionId.collectAsState()

    // Create a view model for the session data.
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            clickedSessionId = clickedSessionId,
        )
    )

    // Collect the session data from the view model.
    val sessionUiData by sessionViewModel.sessionUiData.collectAsState()

    // Collect the upload state from the view model.
    val sessionState by sessionViewModel.sessionState.collectAsState()

    // Collect the connectivity status from the shared view model.
    val isConnected by mainViewModel.isConnected.collectAsState()

    // A function to show a toast message that will be passed to viewModel to show toast when needed.
    val showToast = { message: String, style: ToastStyle ->
        mainViewModel.showToast(
            message = message, style = style,
        )
    }

    SessionScreenContent(
        sessionState = sessionState,
        sessionUiData = sessionUiData,
        showToast = showToast,
        navigateToAutomaticAnalysis = navigateToAutomaticAnalysis,
        navigateToWebpageContentScreen = navigateToWebpageContentScreen,
        navigateToRequestDetailsScreen = navigateToRequestDetailsScreen,
        isConnected = isConnected,
        uploadSession = sessionViewModel::uploadSession,
        toggleScreenshotPrivacyOrToSrelated = sessionViewModel::toggleScreenshotPrivacyOrToSrelated,
        toggleShowBottomSheet = sessionViewModel::toggleShowBottomSheet,
        toggleIsBodyEmpty = sessionViewModel::toggleIsBodyEmpty,
        modifySelectedMethods = sessionViewModel::modifySelectedMethods,
        resetFilters = sessionViewModel::resetFilters,
        updateClickedContent = mainViewModel::updateClickedContent,
        updateClickedRequest = mainViewModel::updateClickedRequest
    )

}

@Composable
private fun SessionScreenContent(
    sessionState: SessionState,
    sessionUiData: SessionUiData,
    isConnected: Boolean,
    showToast: (String, ToastStyle) -> Unit,
    navigateToAutomaticAnalysis: () -> Unit,
    navigateToWebpageContentScreen: () -> Unit,
    navigateToRequestDetailsScreen: () -> Unit,
    uploadSession: (SessionData?, (String, ToastStyle) -> Unit) -> Unit,
    toggleScreenshotPrivacyOrToSrelated: (ScreenshotEntity) -> Unit,
    toggleShowBottomSheet: () -> Unit,
    toggleIsBodyEmpty: () -> Unit,
    modifySelectedMethods: (RequestMethod) -> Unit,
    resetFilters: () -> Unit,
    updateClickedContent: (WebpageContentEntity) -> Unit,
    updateClickedRequest: (CustomWebViewRequestEntity) -> Unit
) {
    Scaffold { paddingValues ->
        /**
         * Handles the upload state.
         *
         * If the upload state is [SessionState.Uploading], it displays a loading indicator.
         *
         * If the upload state is [SessionState.Loading], it displays a loading indicator.
         *
         * If the upload state is [SessionState.Error], it displays an error component with a retry button.
         *
         * If the upload state is [SessionState.AlreadyUploaded], [SessionState.Success], or [SessionState.NeverUploaded],
         * it displays the session details with only the button changed based on the three different states.
         */
        Box(Modifier.padding(paddingValues)) {
            when (sessionState) {
                // Display a loading indicator while the session is being uploaded to remote server
                SessionState.Uploading -> {
                    LoadingIndicator(message = stringResource(R.string.uploading_information_to_be_analyzed))
                }
                // Display a loading indicator while the session is being loaded from DB
                SessionState.Loading -> {
                    LoadingIndicator(message = stringResource(R.string.loading_session))
                }
                //Display an error component with a retry button if error while uploading to remote server
                is SessionState.ErrorUploading -> {
                    val errorMessage = (sessionState as SessionState.ErrorUploading).message
                    ErrorComponent(
                        error = errorMessage,
                        onRetryClick = {
                            uploadSession(
                                sessionUiData.sessionData,
                                showToast,
                            )
                        }
                    )
                }
                //Display an error message if error while loading from DB
                is SessionState.ErrorLoading -> {
                    val errorMessage = (sessionState as SessionState.ErrorLoading).message
                    ErrorComponent(
                        error = errorMessage,
                    )
                }

                else -> {
                    // This branch is for all other SessionState values
                    // If the upload state is UploadState.AlreadyUploaded, UploadState.Success, or UploadState.NeverUploaded,
                    // it displays the session details with only the button changed based on the three different states.
                    if (sessionState is SessionState.AlreadyUploaded || sessionState is SessionState.Success || sessionState is SessionState.NeverUploaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            SessionDetail(
                                sessionUiData = sessionUiData,
                                uploadSession = {
                                    uploadSession(
                                        sessionUiData.sessionData,
                                        showToast,
                                    )
                                },
                                switchScreenshotPrivacyOrToSrealted = toggleScreenshotPrivacyOrToSrelated,
                                navigateToAutomaticAnalysis = navigateToAutomaticAnalysis,
                                uploadState = sessionState,
                                onContentItemClick = {
                                    updateClickedContent(it)
                                    navigateToWebpageContentScreen()
                                },
                                onRequestItemClick = {
                                    updateClickedRequest(it)
                                    navigateToRequestDetailsScreen()
                                },
                                onToggleShowBottomSheet = toggleShowBottomSheet,
                                onToggleIsBodyEmpty = toggleIsBodyEmpty,
                                onModifySelectedMethods = modifySelectedMethods,
                                onResetFilters = resetFilters,
                            )


                            HintInfoBox(
                                context = LocalContext.current,
                                modifier = Modifier.align(Alignment.Center),
                            )

                            AnimatedNoInternetBanner(isConnected = isConnected)
                        }
                    } else {
                        // If the upload state is not recognized, throw an exception
                        throw IllegalStateException("Unexpected upload state: $sessionState")
                    }
                }
            }
        }
    }
}
// endregion

// region Session Detail Structure
/**
 * A composable function to display a session detail page.
 *
 * @param sessionUiData The SessionUiData of the clicked session.
 * @param uploadSession A function to upload the session to the remote server.
 * @param uploadState The state of the upload process.
 * @param switchScreenshotPrivacyOrToSrealted A function to switch the privacy of a screenshot or to
 * its related screenshot (as a reaction to user clicking the image).
 * @param navigateToAutomaticAnalysis A function to navigate to the automatic AI analysis screen.
 */
@Composable
fun SessionDetail(
    sessionUiData: SessionUiData,
    uploadSession: () -> Unit,
    uploadState: SessionState,
    switchScreenshotPrivacyOrToSrealted: (ScreenshotEntity) -> Unit,
    navigateToAutomaticAnalysis: () -> Unit,
    onContentItemClick: (WebpageContentEntity) -> Unit,
    onRequestItemClick: (CustomWebViewRequestEntity) -> Unit,
    onToggleShowBottomSheet: () -> Unit,
    onToggleIsBodyEmpty: () -> Unit,
    onModifySelectedMethods: (RequestMethod) -> Unit,
    onResetFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp) // Apply horizontal padding here
    ) {
        // Section displaying network details (no longer a Card)
        SessionGeneralDetails(
            sessionUiData.sessionData,
            Modifier.padding(top = 16.dp)
        ) // Add top padding

        Spacer(modifier = Modifier.height(16.dp))

        // TabRow for navigating between requests, content, and screenshots
        var selectedTab by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Requests (${sessionUiData.sessionData?.requests?.size ?: 0})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Content (${sessionUiData.sessionData?.webpageContent?.size ?: 0})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Images (${sessionUiData.sessionData?.screenshots?.size ?: 0})") }
            )
        }

        // Container for tab content
        Column(modifier = Modifier.weight(1f)) { // This column takes remaining space
            // Fixed Filter Header for Requests Tab
            if (selectedTab == 0) {
                FiltersHeader(
                    onToggleShowBottomSheet = onToggleShowBottomSheet,
                    modifier = Modifier.padding(vertical = 8.dp) // Consistent padding
                )
                Divider() // Optional divider below filters
            }

            // Scrollable Content Area based on selected tab
            Box(
                modifier = Modifier
                    .weight(1f) // This Box takes space within the Column below tabs/filters
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> RequestsList(
                        requests = sessionUiData.sessionData?.requests,
                        onRequestItemClick = onRequestItemClick
                    )

                    1 -> ContentList(
                        content = sessionUiData.sessionData?.webpageContent,
                        onContentItemClick = onContentItemClick
                    )

                    2 -> ScreenshotsList(
                        screenshots = sessionUiData.sessionData?.screenshots,
                        toggleScreenshotPrivacyOrToSrelated = switchScreenshotPrivacyOrToSrealted,
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons for upload and analysis functionalities
        SessionActionButtons(
            uploadState = uploadState,
            onUploadClick = uploadSession,
            onAnalysisClick = navigateToAutomaticAnalysis,
            modifier = Modifier.padding(bottom = 16.dp) // Add bottom padding
        )

        // Bottom Sheet shown only when user clicks on filter icon (controlled outside the main layout flow)
        if (sessionUiData.showFilteringBottomSheet) {
            FilterBottomSheet(
                onDismiss = onToggleShowBottomSheet,
                isBodyEmptyChecked = sessionUiData.isBodyEmptyChecked,
                onToggleIsBodyEmpty = onToggleIsBodyEmpty,
                selectedMethods = sessionUiData.selectedMethods,
                onToggleSelectedMethods = onModifySelectedMethods,
                onResetFilters = onResetFilters
            )
        }
    }
}

/**
 * Displays the general network session details. No longer uses a Card.
 *
 * @param clickedSessionData The session data to display.
 * @param modifier Modifier for the outer Box.
 */
@Composable
private fun SessionGeneralDetails(clickedSessionData: SessionData?, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxHeight = 85.dp // Max height when collapsed

    // Using Box to allow positioning the expand/collapse icon easily
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border( // Optional: add a subtle border if desired
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(bottom = 8.dp) // Add some padding at the bottom
    ) {
        // Expand/Collapse Icon Button in top-right corner
        IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp) // Keep padding for the icon button itself
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

        // Content with max height constraint
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp) // Apply padding to content
                .padding(end = 48.dp) // Padding to avoid overlap with icon
                .then(
                    if (!isExpanded) Modifier.heightIn(max = maxHeight) else Modifier
                )
        ) {
            clickedSessionData?.session?.apply {
                ssid?.let {
                    DetailItem(label = stringResource(R.string.ssid_label), value = it)
                }
                pcapFilePath?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.pcap_file_label)} ", // Added space
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = it.substringAfterLast('/'), // Show only filename
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false) // Adjust weight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.fiber_new_24px),
                            contentDescription = stringResource(R.string.new_pcap_file_available),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                bssid?.let { DetailItem(label = stringResource(R.string.bssid_label), value = it) }
                ipAddress?.let { DetailItem(label = stringResource(R.string.ip_label), value = it) }
                gatewayAddress?.let {
                    DetailItem(
                        label = stringResource(R.string.gateway_label),
                        value = it
                    )
                }
                captivePortalUrl?.let {
                    DetailItem(
                        label = stringResource(R.string.portal_url_label),
                        value = it
                    )
                }
            }
        }
    }
}

/** Helper composable for key-value pairs in SessionGeneralDetails */
@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label ", // Add space after label
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1 // Ensure long values don't wrap excessively
        )
    }
}


/**
 * A composable function to display two buttons for a session detail page.
 *
 * @param uploadState The state of the upload process.
 * @param onUploadClick A function to upload the session to the remote server.
 * @param onAnalysisClick A function to navigate to the automatic AI analysis screen.
 * @param modifier Modifier to apply to the FlowRow container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionActionButtons(
    uploadState: SessionState,
    onUploadClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterHorizontally
        ), // Spacing and centering
        verticalArrangement = Arrangement.Center
    ) {

        when (uploadState) {
            SessionState.AlreadyUploaded -> {
                RoundCornerButton(
                    onClick = { },
                    buttonText = stringResource(R.string.already_uploaded),
                    enabled = false,
                    fillWidth = false,
                )
            }

            SessionState.Success -> {
                RoundCornerButton(
                    onClick = { },
                    buttonText = stringResource(R.string.thanks_for_uploading),
                    enabled = false,
                    fillWidth = false,
                )
            }

            SessionState.NeverUploaded -> {
                RoundCornerButton(
                    onClick = onUploadClick,
                    buttonText = stringResource(R.string.upload_session_for_analysis),
                    enabled = true,
                    fillWidth = false,
                )
            }
            // Handle error states if needed, though usually handled by ErrorComponent
            else -> {
                // Button might be hidden or disabled in error states, handled by the parent logic
            }
        }

        // Always show the analysis button if data is present
        GhostButton(
            onClick = onAnalysisClick,
            buttonText = stringResource(R.string.automatic_analysis_button),
            fillWidth = false,
        )
    }
}
// endregion

// region Requests Tab Content
/**
 * Header section for the filters in the Requests tab. This part is fixed.
 *
 * @param onToggleShowBottomSheet Callback to open/close the filter bottom sheet.
 * @param modifier Modifier for the Row container.
 */
@Composable
private fun FiltersHeader(onToggleShowBottomSheet: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Use padding from parent if needed, or add specific here
    ) {
        Text(
            stringResource(R.string.filters),
            style = MaterialTheme.typography.titleMedium
        ) // Use TitleMedium for prominence
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleShowBottomSheet) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.filter),
                contentDescription = stringResource(R.string.show_filters) // More descriptive
            )
        }
    }
}

/**
 * A composable function to display a list of web requests.
 * The filter header is now separate and fixed. This LazyColumn only contains the list items.
 *
 * @param requests A list of CustomWebViewRequestEntity to display.
 * @param onRequestItemClick Callback when a request item is clicked.
 */
@Composable
private fun RequestsList(
    requests: List<CustomWebViewRequestEntity>?,
    onRequestItemClick: (CustomWebViewRequestEntity) -> Unit
) {
    if (requests.isNullOrEmpty()) {
        EmptyListUi(R.string.no_requests_found)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) { // Fill available space
        // --- Before Authentication Section ---
        val beforeAuthRequests = requests.filter { !it.hasFullInternetAccess }
        if (beforeAuthRequests.isNotEmpty()) {
            item {
                ListSectionHeader(stringResource(R.string.before_authentication))
            }
            itemsIndexed(
                beforeAuthRequests,
                key = { _, item -> item.customWebViewRequestId }) { index, request ->
                RequestListItem(
                    onRequestItemClick = onRequestItemClick,
                    request = request
                )
                // Add divider after each item except the last one in this section
                if (index < beforeAuthRequests.size - 1) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp)) // Add horizontal padding to divider
                }
            }
        } else {
            item {
                ListSectionHeader(stringResource(R.string.before_authentication))
                Text(
                    text = stringResource(R.string.no_requests_found),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    ) // Consistent padding
                )
            }
        }


        // --- After Authentication Section ---
        val afterAuthRequests = requests.filter { it.hasFullInternetAccess }
        if (afterAuthRequests.isNotEmpty()) {
            // Add a visual separator if both sections have items
            if (beforeAuthRequests.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)) } // Space between sections
            }

            item {
                ListSectionHeader(stringResource(R.string.after_authentication))
            }
            itemsIndexed(
                afterAuthRequests,
                key = { _, item -> item.customWebViewRequestId }) { index, request ->
                RequestListItem(
                    onRequestItemClick = onRequestItemClick,
                    request = request
                )
                // Add divider after each item except the last one in this section
                if (index < afterAuthRequests.size - 1) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp)) // Add horizontal padding to divider
                }
            }
        } else {
            // Only show the "No requests found" message if the "Before" section also had no requests
            if (beforeAuthRequests.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)) } // Space between sections
            }
            item {
                ListSectionHeader(stringResource(R.string.after_authentication))
                Text(
                    text = stringResource(R.string.no_requests_found),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    ) // Consistent padding
                )
            }
        }
    }
}

/** Helper for section headers in lists */
@Composable
private fun ListSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 8.dp
        ) // Consistent padding
    )
}


/**
 * A composable that displays a [CustomWebViewRequestEntity]. No longer uses Card.
 * A divider is added *after* this item in the parent LazyColumn.
 *
 * @param onRequestItemClick A callback function invoked when the user clicks on the item.
 * @param request The [CustomWebViewRequestEntity] to display.
 */
@Composable
private fun RequestListItem(
    onRequestItemClick: (CustomWebViewRequestEntity) -> Unit,
    request: CustomWebViewRequestEntity
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRequestItemClick(request) }
            .padding(horizontal = 16.dp, vertical = 12.dp) // Adjust padding as needed
    ) {
        request.url?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.url_label), // Use label string resource
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(IntrinsicSize.Min) // Prevent label from taking too much space
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp)) // Space between rows
        }
        request.type?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.type_label), // Use label string resource
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp)) // Space between rows
        }
        RequestMethodView(request.method) // Assumes this component handles its own padding/layout internally

        Spacer(Modifier.height(4.dp)) // Space between rows

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.timestamp_label), // Use label string resource
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.width(4.dp))
            Text(formatDate(request.timestamp), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp)) // Space before hint
        HintTextWithIcon(
            hint = stringResource(R.string.hint_click_to_view_request_content),
            iconResId = R.drawable.tap
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    isBodyEmptyChecked: Boolean,
    onToggleIsBodyEmpty: () -> Unit,
    selectedMethods: List<Map<RequestMethod, Boolean>>,
    onToggleSelectedMethods: (RequestMethod) -> Unit,
    onResetFilters: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) // Keep expanded

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding() // Add padding for navigation bar
        ) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox to hide requests with empty body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleIsBodyEmpty() } // Make row clickable
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isBodyEmptyChecked,
                    onCheckedChange = { onToggleIsBodyEmpty() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hide_requests_with_empty_body))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Method selection
            Text(stringResource(R.string.select_method))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Add vertical spacing for wrapping
            ) {
                selectedMethods.forEach { methodMap ->
                    val method = methodMap.keys.first()
                    val isSelected = methodMap.values.first()
                    CustomChip(
                        label = method.name,
                        onClick = { onToggleSelectedMethods(method) },
                        isSelected = isSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // More space before button

            // Remove filters button
            GhostButton(
                onClick = {
                    onResetFilters()
                    // onDismiss() // Optionally dismiss after reset
                },
                buttonText = stringResource(R.string.remove_filters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp)) // Space at the bottom
        }
    }
}
// endregion

// region Content Tab Content
/**
 * A composable function to display a list of webpage content.
 * If the content list is empty, a message is displayed indicating no content is found.
 * Otherwise, each item in the content list is displayed in a card format.
 *
 * @param content A list of WebpageContentEntity to display.
 */
@Composable
private fun ContentList(
    content: List<WebpageContentEntity>?,
    onContentItemClick: (WebpageContentEntity) -> Unit
) {
    if (content.isNullOrEmpty()) {
        EmptyListUi(R.string.no_webpages_found)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) { // Add padding
        itemsIndexed(content, key = { _, item -> item.contentId }) { index, item ->
            ContentItem(item, onContentItemClick)
            if (index < content.size - 1) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp)) // Add divider between items
            }
        }
    }
}

@Composable
private fun ContentItem(
    item: WebpageContentEntity,
    onContentItemClick: (WebpageContentEntity) -> Unit,
) {
    // Removed the Card wrapper
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContentItemClick(item) } // Apply clickable to the Column
            .padding(horizontal = 16.dp, vertical = 12.dp) // Consistent padding like RequestListItem
    ) {
        item.url?.let { urlValue ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.url_label), // Use label string
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(IntrinsicSize.Min) // Prevent label taking too much space
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = urlValue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // Show only filenames for paths, using Row for consistency
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.html_path_label), // Use label string
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(IntrinsicSize.Min)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = item.htmlContentPath.substringAfterLast('/'), // Filename only
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.js_path_label), // Use label string
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(IntrinsicSize.Min)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = item.jsContentPath.substringAfterLast('/'), // Filename only
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.timestamp_label), // Use label string
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(IntrinsicSize.Min)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatDate(item.timestamp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(8.dp)) // Space before hint

        HintTextWithIcon(
            hint = stringResource(R.string.hint_click_to_view_content),
            iconResId = R.drawable.tap
        )
    }
    // Divider is added by the parent LazyColumn (ContentList)
}

// endregion

// region Screenshots Tab Content
/**
 * A composable function to display a list of screenshots.
 * If the list is empty, a message is displayed indicating no screenshots are found.
 * Otherwise, each screenshot item in the list is displayed in a card format.
 *
 * @param screenshots A list of ScreenshotEntity to display.
 * @param toggleScreenshotPrivacyOrToSrelated A function to call when the user wants
 * to toggle whether a screenshot is privacy-related or related to terms of service.
 */
@Composable
private fun ScreenshotsList(
    screenshots: List<ScreenshotEntity>?,
    toggleScreenshotPrivacyOrToSrelated: (ScreenshotEntity) -> Unit,
) {

    // Check if the screenshots list is empty and display a message if true
    if (screenshots.isNullOrEmpty()) {
        EmptyListUi(R.string.no_screenshots_found)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Display a hint text for selecting privacy-related images
        Text(
            text = stringResource(R.string.hint_select_privacy_images),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp) // Add padding
        )

        // LazyVerticalGrid to display screenshots in a grid layout
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp), // Adjust minSize if needed
            modifier = Modifier
                .weight(1f) // Ensure grid takes available space
                .padding(horizontal = 8.dp), // Padding around the grid
            contentPadding = PaddingValues(bottom = 16.dp), // Padding at the bottom of the grid
            verticalArrangement = Arrangement.spacedBy(8.dp), // Space between rows
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between columns
        ) {
            // Iterate over each screenshot and display it using ImageItem
            items(
                items = screenshots,
                key = { it.screenshotId } // Add key for better performance
            ) { screenshot ->
                ImageItem(
                    imagePath = screenshot.path,
                    isSelected = screenshot.isPrivacyOrTosRelated,
                    onImageClick = {
                        toggleScreenshotPrivacyOrToSrelated(screenshot)
                    }
                )
            }
        }
    }
}

/**
 * A composable function to display a single image with a border and a background
 * that can be clicked to toggle whether it is selected or not.
 *
 * @param imagePath The path of the image to display.
 * @param isSelected Whether the image is currently selected.
 * @param onImageClick A function to call when the image is clicked.
 */
@Composable
fun ImageItem(
    imagePath: String,
    isSelected: Boolean,
    onImageClick: () -> Unit // Simplified callback
) {
    // Using Card provides shape and elevation, good for grid items
    Card(
        modifier = Modifier
            .aspectRatio(1f) // Maintain square aspect ratio
            .clickable(onClick = onImageClick),
        shape = RoundedCornerShape(8.dp), // Softer corners
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke( // Use Card's border property
            width = if (isSelected) 3.dp else 0.dp, // Thicker border when selected
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) { // Box to layer image and overlay
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = imagePath.toUri())
                        .crossfade(true) // Add crossfade animation
                        .build()
                ),
                contentDescription = stringResource(R.string.screenshot_image), // Add content description
                modifier = Modifier.fillMaxSize(), // Fill the Card
                contentScale = ContentScale.Crop // Crop to fit
            )

            // Overlay with text for selected images
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Use scrim color for better contrast/theming
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tos_privacy_related_label), // Use string resource
                        color = MaterialTheme.colorScheme.onPrimary, // Ensure text is visible on scrim
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// endregion

// region Utility Composables (Hint, Empty List, etc.)
/**
 * A composable function to display a hint information box with a "Never see again" option.
 *
 * @param context The Android context for retrieving the "Never see again" state.
 * @param modifier The modifier to be applied to the AlertDialog.
 */
@Composable
private fun HintInfoBox(
    context: Context,
    modifier: Modifier,
) {
    var showInfoBox2 by remember { mutableStateOf(false) }

    // Launch a coroutine to collect the "Never see again" state from the DataStore
    LaunchedEffect(Unit) {
        AlertDialogState.getNeverSeeAgainState(context, "info_box_2")
            .collect { neverSeeAgain ->
                showInfoBox2 = !neverSeeAgain
            }
    }

    if (showInfoBox2) {
        NeverSeeAgainAlertDialog(
            title = stringResource(R.string.hint),
            message = stringResource(R.string.review_session_data),
            preferenceKey = "info_box_2",
            onDismiss = {
                showInfoBox2 = false
            },
            modifier = modifier
        )
    }
}

/**
 * A composable function to display an empty list message.
 *
 * @param stringRes The string resource to display.
 */
@Composable
private fun EmptyListUi(@StringRes stringRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Add padding
        contentAlignment = Alignment.Center // Center content
    ) {
        Text(
            stringResource(id = stringRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
// endregion

// region Preview Functions

// Helper function for mock data generation
private fun createMockSessionData(requests: List<CustomWebViewRequestEntity>): SessionData {
    return SessionData(
        session = NetworkSessionEntity(
            networkId = "mock-${System.currentTimeMillis()}", // Unique ID for preview
            ssid = "Preview WiFi",
            bssid = "AA:BB:CC:DD:EE:FF",
            timestamp = System.currentTimeMillis() - 100000,
            captivePortalUrl = "http://portal.example.com/login",
            ipAddress = "192.168.1.101",
            gatewayAddress = "192.168.1.1",
            pcapFilePath = "file:///preview/path/analyzer.pcap",
            isCaptiveLocal = true,
            isUploadedToRemoteServer = false
        ),
        requests = requests,
        screenshots = listOf(
            ScreenshotEntity(
                screenshotId = 1,
                sessionId = "1",
                path = "/preview/path/ss1.png",
                isPrivacyOrTosRelated = false,
                timestamp = System.currentTimeMillis() - 50000,
         size = "1024 KB",
         url = "http://example.com/screenshot1",
            ),
            ScreenshotEntity(
                screenshotId = 2,
                sessionId = "1",
                path = "/preview/path/ss2_privacy.png",
                isPrivacyOrTosRelated = true,
                timestamp = System.currentTimeMillis() - 40000,
                size = "1024 KB",
                url = "http://example.com/screenshot2",
            )
        ),
        webpageContent = listOf(
            WebpageContentEntity(
                contentId = 1,
                sessionId = "1",
                url = "http://example.com",
                htmlContentPath = "/preview/html1.html",
                jsContentPath = "/preview/js1.js",
                timestamp = System.currentTimeMillis() - 60000
            )
        ),
        requestsCount = requests.size,
        screenshotsCount = 2,
        webpageContentCount = 1
    )
}

private fun createMockRequests(): List<CustomWebViewRequestEntity> {
    return listOf(
        CustomWebViewRequestEntity(
            customWebViewRequestId = 10,
            sessionId = "1",
            type = "Document",
            url = "http://portal.example.com/login",
            method = RequestMethod.GET,
            body = null,
            headers = "Accept: text/html\nUser-Agent: Preview",
            timestamp = System.currentTimeMillis() - 90000,
            hasFullInternetAccess = false
        ),
        CustomWebViewRequestEntity(
            customWebViewRequestId = 11,
            sessionId = "1",
            type = "XHR",
            url = "https://analytics.example.com/track",
            method = RequestMethod.POST,
            body = "{\"event\":\"page_load\"}",
            headers = "Content-Type: application/json",
            timestamp = System.currentTimeMillis() - 85000,
            hasFullInternetAccess = false
        ),
        CustomWebViewRequestEntity(
            customWebViewRequestId = 12,
            sessionId = "1",
            type = "Image",
            url = "http://portal.example.com/logo.png",
            method = RequestMethod.GET,
            body = null,
            headers = "Accept: image/*",
            timestamp = System.currentTimeMillis() - 80000,
            hasFullInternetAccess = false
        ),
        CustomWebViewRequestEntity(
            customWebViewRequestId = 13,
            sessionId = "1",
            type = "Other",
            url = "https://connectivitycheck.gstatic.com/generate_204",
            method = RequestMethod.GET,
            body = null,
            headers = null,
            timestamp = System.currentTimeMillis() - 30000,
            hasFullInternetAccess = true // After hypothetical auth
        ),
        CustomWebViewRequestEntity(
            customWebViewRequestId = 14,
            sessionId = "1",
            type = "Script",
            url = "https://cdn.example.com/library.js",
            method = RequestMethod.GET,
            body = null,
            headers = "Accept: */*",
            timestamp = System.currentTimeMillis() - 25000,
            hasFullInternetAccess = true // After hypothetical auth
        )
    )
}


@Composable
@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    name = "Phone Light - Requests"
)
@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    name = "Phone Dark - Requests",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "Tablet Light - Requests"
)
fun SessionScreenContentPreview_Requests() {
    val mockRequests = createMockRequests()
    val mockSessionData = createMockSessionData(mockRequests)
    val mockSessionState =
        SessionState.NeverUploaded // Preview different states: Success, AlreadyUploaded

    val mockSessionUiData = SessionUiData(
        sessionData = mockSessionData,
        showFilteringBottomSheet = false, // Set to true to preview bottom sheet open
        isBodyEmptyChecked = false,
        selectedMethods = RequestMethod.entries.map { mapOf(it to true) }, // Start with all selected
        unfilteredRequests = mockRequests // Assuming unfiltered is same initially
    )
    AppTheme {
        // Need to wrap in Surface for theme colors
        Surface(color = MaterialTheme.colorScheme.background) {
            SessionScreenContent(
                sessionState = mockSessionState,
                sessionUiData = mockSessionUiData,
                isConnected = true,
                showToast = { _, _ -> },
                navigateToAutomaticAnalysis = {},
                navigateToWebpageContentScreen = {},
                navigateToRequestDetailsScreen = {},
                uploadSession = { _, _ -> },
                toggleScreenshotPrivacyOrToSrelated = {},
                toggleShowBottomSheet = {},
                toggleIsBodyEmpty = {},
                modifySelectedMethods = {},
                resetFilters = {},
                updateClickedContent = {},
                updateClickedRequest = {}
            )
        }
    }
}


@Composable
@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    name = "Phone Light - No Requests"
)
fun SessionScreenContentPreview_NoRequests() {
    val mockSessionData = createMockSessionData(emptyList()) // No requests
    val mockSessionState = SessionState.NeverUploaded

    val mockSessionUiData = SessionUiData(
        sessionData = mockSessionData,
        showFilteringBottomSheet = false,
        isBodyEmptyChecked = false,
        selectedMethods = RequestMethod.entries.map { mapOf(it to true) },
        unfilteredRequests = emptyList()
    )
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SessionScreenContent(
                sessionState = mockSessionState,
                sessionUiData = mockSessionUiData,
                isConnected = true,
                showToast = { _, _ -> },
                navigateToAutomaticAnalysis = {},
                navigateToWebpageContentScreen = {},
                navigateToRequestDetailsScreen = {},
                uploadSession = { _, _ -> },
                toggleScreenshotPrivacyOrToSrelated = {},
                toggleShowBottomSheet = {},
                toggleIsBodyEmpty = {},
                modifySelectedMethods = {},
                resetFilters = {},
                updateClickedContent = {},
                updateClickedRequest = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RequestListItemPreview() {
    val request = CustomWebViewRequestEntity(
        customWebViewRequestId = 1,
        sessionId = "1",
        type = "XHR Long Type Name Here To Test Wrapping",
        url = "https://really.long.url.that.might.need.to.be.ellipsized.com/api/v1/data?param1=value1¶m2=value2",
        method = RequestMethod.POST,
        body = "{\"key\":\"value\"}",
        headers = "Content-Type: application/json\nAuthorization: Bearer token",
        timestamp = System.currentTimeMillis() - 5000,
        hasFullInternetAccess = false
    )
    AppTheme {
        Surface {
            Column {
                RequestListItem(onRequestItemClick = {}, request = request)
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                RequestListItem(
                    onRequestItemClick = {},
                    request = request.copy(
                        customWebViewRequestId = 2,
                        method = RequestMethod.GET,
                        url = "http://short.url"
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageItemPreview() {
    AppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            ImageItem(
                imagePath = "mock", // Coil won't load this, but shows layout
                isSelected = true,
                onImageClick = {}
            )
            ImageItem(
                imagePath = "mock",
                isSelected = false,
                onImageClick = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun FilterBottomSheetPreview() {
    AppTheme {
        FilterBottomSheet(
            onDismiss = { },
            isBodyEmptyChecked = true,
            onToggleIsBodyEmpty = { },
            selectedMethods = RequestMethod.entries.mapIndexed { index, method -> mapOf(method to (index % 2 == 0)) }, // Select alternating methods
            onToggleSelectedMethods = { },
            onResetFilters = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionGeneralDetailsPreview() {
    AppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SessionGeneralDetails(createMockSessionData(emptyList()))
        }
    }
}

// endregion