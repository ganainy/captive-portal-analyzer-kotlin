package com.example.captive_portal_analyzer_kotlin.screens.session

import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.captive_portal_analyzer_kotlin.repository.MessageType
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.AppUtils.Companion.formatDate

// region Main Screen Composable
@Composable
fun SessionScreen(
    mainViewModel: MainViewModel,
    repository: NetworkSessionRepository,
    navigateToAutomaticAnalysis: () -> Unit,
    navigateToWebpageContentScreen: () -> Unit,
    navigateToRequestDetailsScreen: () -> Unit
) {
    val clickedSessionId by mainViewModel.clickedSessionId.collectAsState()

    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            clickedSessionId = clickedSessionId,
        )
    )

    val sessionUiData by sessionViewModel.sessionUiData.collectAsState()
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()
    val uploadHistory by sessionViewModel.uploadHistory.collectAsState()

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
        updateClickedRequest = mainViewModel::updateClickedRequest,
        uploadHistory = uploadHistory,
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
    uploadSession: (SessionData, (String, ToastStyle) -> Unit) -> Unit,
    toggleScreenshotPrivacyOrToSrelated: (ScreenshotEntity) -> Unit,
    toggleShowBottomSheet: () -> Unit,
    toggleIsBodyEmpty: () -> Unit,
    modifySelectedMethods: (RequestMethod) -> Unit,
    resetFilters: () -> Unit,
    updateClickedContent: (WebpageContentEntity) -> Unit,
    updateClickedRequest: (CustomWebViewRequestEntity) -> Unit,
    uploadHistory: List<Pair<String, MessageType>>
) {
    Scaffold { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            when (sessionState) {
                is SessionState.Uploading -> {
                    SessionUploadHistoryLog(uploadHistory)
                }

                SessionState.Loading -> {
                    LoadingIndicator(message = stringResource(R.string.loading_session))
                }

                is SessionState.ErrorUploading -> {
                    val errorMessage = sessionState.message
                    ErrorComponent(
                        error = errorMessage,
                        onRetryClick = {
                            uploadSession(
                                sessionUiData.sessionData!!,
                                showToast,
                            )
                        }
                    )
                }

                is SessionState.ErrorLoading -> {
                    val errorMessage = sessionState.message
                    ErrorComponent(
                        error = errorMessage,
                    )
                }

                else -> {
                    if (sessionState is SessionState.AlreadyUploaded || sessionState is SessionState.Success || sessionState is SessionState.NeverUploaded || sessionState is SessionState.ReUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            SessionDetail(
                                sessionUiData = sessionUiData,
                                uploadSession = {
                                    uploadSession(
                                        sessionUiData.sessionData!!,
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
                        throw IllegalStateException("Unexpected upload state: $sessionState")
                    }
                }
            }
        }
    }
}


/**
 * A composable to display the historical log of upload steps.
 *
 * @param uploadHistory The list of pairs containing messages and their corresponding MessageType.
 */
@Composable
fun SessionUploadHistoryLog(
    uploadHistory: List<Pair<String, MessageType>>,
) {
    // Helper function to get icon and color based on MessageType and item state
    @Composable
    fun getIconAndColorForType(
        type: MessageType,
        isLastEntry: Boolean
    ): Pair<ImageVector, Color> {
     return when (type) {
         MessageType.INFO -> Pair(Icons.Default.Info, MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLastEntry) 1f else 0.6f))
         MessageType.SUCCESS -> Pair(Icons.Default.CheckCircle, Color(0xFF4CAF50).copy(alpha = if (isLastEntry) 1f else 0.6f)) // Green
         MessageType.WARNING -> Pair(Icons.Default.Warning, MaterialTheme.colorScheme.tertiary.copy(alpha = if (isLastEntry) 1f else 0.6f)) // Warning color
         MessageType.ERROR -> Pair(Icons.Default.Warning, MaterialTheme.colorScheme.error.copy(alpha = if (isLastEntry) 1f else 0.6f)) // Error color
     }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f) // Take most of the width
            .wrapContentHeight()
            .padding(16.dp), // Padding around the card
        shape = MaterialTheme.shapes.medium, // Rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Add some shadow
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp) // Inner padding for the content
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Spacing between sections
        ) {

         Row(verticalAlignment = Alignment.CenterVertically) {
             CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
             Spacer(modifier = Modifier.width(8.dp))
             Text(
                 text = "Uploading...",
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurface
             )
         }

            // The log messages list or placeholder/loading
            if (uploadHistory.isEmpty() ) {
                // Show a placeholder message when the log is empty and not loading
                Text(
                    text = "Waiting to start upload...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp), // Allow more height if needed, but still scrollable
                    verticalArrangement = Arrangement.spacedBy(10.dp) // Spacing between log items
                ) {
                    itemsIndexed(uploadHistory) { index, (message, type) ->
                        val isLastEntry = index == uploadHistory.size - 1
                        val (icon, color) = getIconAndColorForType(type, isLastEntry)
                        val alpha = if (isLastEntry ) 1f else 0.6f // Fade out previous items slightly, keep last one prominent if loading

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between icon and text
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type.name, // Accessibility
                                tint = color,
                                modifier = Modifier.size(20.dp) // Icon size
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = color.copy(alpha = alpha), // Apply alpha to text as well
                                modifier = Modifier.weight(1f) // Allow text to take available space
                            )
                        }
                    }


                    // Also show spinner if history is empty but isLoading is true
                    if ( uploadHistory.isEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}


// endregion

// region Session Detail Structure
@OptIn(ExperimentalFoundationApi::class) // Needed for stickyHeader
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val requests = sessionUiData.sessionData?.requests ?: emptyList()
    val content = sessionUiData.sessionData?.webpageContent ?: emptyList()
    val screenshots = sessionUiData.sessionData?.screenshots ?: emptyList()

    // Main Column for layout distribution
    Column(modifier = Modifier.fillMaxSize()) {

        // Scrollable content area using LazyColumn
        LazyColumn(
            modifier = Modifier.weight(1f), // Takes available space, pushing button area down
            contentPadding = PaddingValues(
                horizontal = 4.dp,
                vertical = 4.dp
            ), // Keep content padding
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. General Details - This will scroll off screen
            item {
                SessionGeneralDetails(sessionUiData.sessionData)
            }

            // 2. Sticky Header - Contains Tabs and conditional Filters
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp // Optional elevation
                ) {
                    Column {
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
                                text = { Text("Images (${screenshots.size})") }
                            )
                        }
                        if (selectedTab == 0) {
                            FiltersHeader(
                                onToggleShowBottomSheet = onToggleShowBottomSheet,
                                // Padding adjusted to be within the Surface
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Divider()
                        }
                    }
                }
            }

            // 3. Tab Content - Placed directly within LazyColumn's scope
            when (selectedTab) {
                0 -> { // Requests Tab
                    if (requests.isEmpty()) {
                        item { EmptyListUi(R.string.no_requests_found) }
                    } else {
                        // --- Before Authentication Section ---
                        val beforeAuthRequests = requests.filter { !it.hasFullInternetAccess }
                        if (beforeAuthRequests.isNotEmpty()) {
                            item { ListSectionHeader(stringResource(R.string.before_authentication)) }
                            itemsIndexed(
                                items = beforeAuthRequests,
                                key = { _, item -> "before-${item.customWebViewRequestId}" } // Unique key
                            ) { index, request ->
                                RequestListItem(
                                    onRequestItemClick = onRequestItemClick,
                                    request = request
                                )
                                if (index < beforeAuthRequests.size - 1) {
                                    Divider() // Use full width divider from LazyColumn
                                }
                            }
                        } else {
                            item {
                                ListSectionHeader(stringResource(R.string.before_authentication))
                                Text(
                                    text = stringResource(R.string.no_requests_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(
                                        vertical = 8.dp,
                                        horizontal = 12.dp
                                    ) // Added padding
                                )
                            }
                        }

                        // --- After Authentication Section ---
                        val afterAuthRequests = requests.filter { it.hasFullInternetAccess }
                        if (afterAuthRequests.isNotEmpty()) {
                            if (beforeAuthRequests.isNotEmpty()) {
                                item { Spacer(Modifier.height(16.dp)) } // Space between sections
                            }
                            item { ListSectionHeader(stringResource(R.string.after_authentication)) }
                            itemsIndexed(
                                items = afterAuthRequests,
                                key = { _, item -> "after-${item.customWebViewRequestId}" } // Unique key
                            ) { index, request ->
                                RequestListItem(
                                    onRequestItemClick = onRequestItemClick,
                                    request = request
                                )
                                if (index < afterAuthRequests.size - 1) {
                                    Divider()
                                }
                            }
                        } else {
                            if (beforeAuthRequests.isNotEmpty()) {
                                item { Spacer(Modifier.height(16.dp)) }
                            }
                            item {
                                ListSectionHeader(stringResource(R.string.after_authentication))
                                Text(
                                    text = stringResource(R.string.no_requests_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(
                                        vertical = 8.dp,
                                        horizontal = 12.dp
                                    ) // Added padding
                                )
                            }
                        }
                    }
                }

                1 -> { // Content Tab
                    if (content.isEmpty()) {
                        item { EmptyListUi(R.string.no_webpages_found) }
                    } else {
                        itemsIndexed(
                            items = content,
                            key = { _, item -> item.contentId }
                        ) { index, item ->
                            ContentItem(item, onContentItemClick)
                            if (index < content.size - 1) {
                                Divider()
                            }
                        }
                    }
                }

                2 -> { // Screenshots Tab
                    item { // Place the entire ScreenshotsList composable within a single item
                        ScreenshotsList(
                            screenshots = screenshots,
                            toggleScreenshotPrivacyOrToSrelated = switchScreenshotPrivacyOrToSrealted,
                        )
                    }
                }
            }

        } // End LazyColumn (Scrollable Content)


        // --- Fixed Bottom Action Area ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SessionActionButtons(
                uploadState = uploadState,
                onUploadClick = uploadSession,
                onAnalysisClick = navigateToAutomaticAnalysis,
                // Add padding within the Surface for the buttons
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        // --- End Fixed Bottom Action Area ---

    } // End Main Column

    // Bottom Sheet remains outside the main layout Column
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


@Composable
private fun SessionGeneralDetails(clickedSessionData: SessionData?, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxHeight = 85.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(bottom = 8.dp)
    ) {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(end = 48.dp)
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
                            text = "${stringResource(R.string.pcap_file_label)} ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = it.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}


@Composable
private fun SessionActionButtons(
    uploadState: SessionState,
    onUploadClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
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

                // for testing only
                SessionState.ReUploading -> {
                    RoundCornerButton(
                        onClick = onUploadClick,
                        buttonText = "Allow reupload for testing",
                        enabled = true,
                        fillWidth = false,
                    )
                }

                is SessionState.ErrorLoading -> {}
                is SessionState.ErrorUploading -> {}
                is SessionState.Uploading -> {}
                is SessionState.Loading -> {}
            }

            GhostButton(
                onClick = onAnalysisClick,
                buttonText = stringResource(R.string.automatic_analysis_button),
                fillWidth = false,
            )
        }
    }
}
// endregion

// region Requests Tab Content
@Composable
private fun FiltersHeader(onToggleShowBottomSheet: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(R.string.filters),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleShowBottomSheet) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.filter),
                contentDescription = stringResource(R.string.show_filters)
            )
        }
    }
}


@Composable
private fun ListSectionHeader(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}


@Composable
private fun RequestListItem(
    onRequestItemClick: (CustomWebViewRequestEntity) -> Unit,
    request: CustomWebViewRequestEntity
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRequestItemClick(request) }
            .padding(vertical = 12.dp)
    ) {
        request.url?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.url_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(IntrinsicSize.Min)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        request.type?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.type_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
        }
        RequestMethodView(request.method)

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.timestamp_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.width(4.dp))
            Text(formatDate(request.timestamp), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        HintTextWithIcon(
            hint = stringResource(R.string.hint_click_to_view_request_content),
            iconResId = R.drawable.tap,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleIsBodyEmpty() }
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

            Text(stringResource(R.string.select_method))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            GhostButton(
                onClick = {
                    onResetFilters()
                },
                buttonText = stringResource(R.string.remove_filters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
// endregion

// region Content Tab Content

@Composable
private fun ContentItem(
    item: WebpageContentEntity,
    onContentItemClick: (WebpageContentEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContentItemClick(item) }
            // Padding applied within the item
            .padding(vertical = 12.dp)
    ) {
        item.url?.let { urlValue ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.url_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(IntrinsicSize.Min)
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.html_path_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(IntrinsicSize.Min)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = item.htmlContentPath.substringAfterLast('/'),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.js_path_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(IntrinsicSize.Min)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = item.jsContentPath.substringAfterLast('/'),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.timestamp_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(IntrinsicSize.Min)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatDate(item.timestamp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(8.dp))

        HintTextWithIcon(
            hint = stringResource(R.string.hint_click_to_view_content),
            iconResId = R.drawable.tap,
        )
    }
}
// endregion

// region Screenshots Tab Content
@Composable
private fun ScreenshotsList(
    screenshots: List<ScreenshotEntity>?,
    toggleScreenshotPrivacyOrToSrelated: (ScreenshotEntity) -> Unit,
) {
    if (screenshots.isNullOrEmpty()) {
        EmptyListUi(R.string.no_screenshots_found)
        return
    }

    // Column needed to place the hint text *above* the grid within the single item scope
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.hint_select_privacy_images),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            // Adjusted padding as it's inside the parent LazyColumn's item padding
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // LazyVerticalGrid scrolls internally within the LazyColumn item
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxWidth() // Grid takes full width within the item
                .heightIn(max = 600.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = screenshots,
                key = { it.screenshotId }
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


@Composable
fun ImageItem(
    imagePath: String,
    isSelected: Boolean,
    onImageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onImageClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 0.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = imagePath.toUri())
                        .crossfade(true)
                        .build()
                ),
                contentDescription = stringResource(R.string.screenshot_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tos_privacy_related_label),
                        color = MaterialTheme.colorScheme.onPrimary,
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
@Composable
private fun HintInfoBox(
    context: Context,
    modifier: Modifier,
) {
    var showInfoBox2 by remember { mutableStateOf(false) }

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

@Composable
private fun EmptyListUi(@StringRes stringRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth() // Changed from fillMaxSize
            .padding(32.dp), // Increased padding for visibility
        contentAlignment = Alignment.Center
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

// --- Preview Composable ---
/**
 * Preview for SessionUploadHistoryLog composable with mock data.
 */
@Composable
@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    name = "Phone Light - SessionUploadHistoryLog"
)
fun PreviewSessionUploadHistoryLog() {
    val history = listOf(
        "Initializing upload..." to MessageType.INFO,
        "Connecting to server..." to MessageType.INFO,
        "Uploading file chunk 1/10..." to MessageType.INFO,
        "Uploading file chunk 2/10..." to MessageType.INFO,
        "Server received chunk 2..." to MessageType.SUCCESS,
        "Uploading file chunk 3/10..." to MessageType.INFO // This will be the last one shown with spinner if isLoading is true
    )

    val historyCompleted = listOf(
        "Initializing upload..." to MessageType.INFO,
        "Connecting to server..." to MessageType.INFO,
        "Uploading file chunk 10/10..." to MessageType.INFO,
        "Upload finished successfully." to MessageType.SUCCESS
    )

    val historyError = listOf(
        "Initializing upload..." to MessageType.INFO,
        "Connecting to server..." to MessageType.INFO,
        "Uploading file chunk 1/10..." to MessageType.INFO,
        "Connection lost." to MessageType.ERROR
    )

    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Uploading State", style = MaterialTheme.typography.titleLarge)
            SessionUploadHistoryLog(uploadHistory = history,)

            Divider()

            Text("Completed State", style = MaterialTheme.typography.titleLarge)
            SessionUploadHistoryLog(uploadHistory = historyCompleted, ) // isLoading false when finished

            Divider()

            Text("Error State", style = MaterialTheme.typography.titleLarge)
            SessionUploadHistoryLog(uploadHistory = historyError) // isLoading false when finished

            Divider()

            Text("Empty/Waiting State (not loading)", style = MaterialTheme.typography.titleLarge)
            SessionUploadHistoryLog(uploadHistory = emptyList(),)

            Divider()

            Text("Loading with Empty History", style = MaterialTheme.typography.titleLarge)
            SessionUploadHistoryLog(uploadHistory = emptyList(),) // Example: waiting for first log entry
        }
    }
}

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
    val mockSessionState = SessionState.NeverUploaded

    val mockSessionUiData = SessionUiData(
        sessionData = mockSessionData,
        showFilteringBottomSheet = false,
        isBodyEmptyChecked = false,
        selectedMethods = RequestMethod.entries.map { mapOf(it to true) },
        unfilteredRequests = mockRequests
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
                updateClickedRequest = {},
                uploadHistory = listOf(
                    "Upload started" to MessageType.INFO,
                    "Processing data" to MessageType.INFO,
                    "Uploading files" to MessageType.INFO
                )
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
                updateClickedRequest = {},
                uploadHistory = listOf(
                    "Upload started" to MessageType.INFO,
                    "Processing data" to MessageType.INFO,
                    "Uploading files" to MessageType.INFO
                )
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
            // Wrap in LazyColumn for realistic context if needed, or just Column
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                item { RequestListItem(onRequestItemClick = {}, request = request) }
                item { Divider() }
                item {
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
}

@Preview(showBackground = true)
@Composable
fun ImageItemPreview() {
    AppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(16.dp)
                .height(150.dp) // Added height for preview
        ) {
            ImageItem(
                imagePath = "mock",
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
        Box(Modifier.fillMaxSize()) {
            FilterBottomSheet(
                onDismiss = { },
                isBodyEmptyChecked = true,
                onToggleIsBodyEmpty = { },
                selectedMethods = RequestMethod.entries.mapIndexed { index, method -> mapOf(method to (index % 2 == 0)) },
                onToggleSelectedMethods = { },
                onResetFilters = { }
            )
        }
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