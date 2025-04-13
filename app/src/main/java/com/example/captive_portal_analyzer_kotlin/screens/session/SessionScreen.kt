package com.example.captive_portal_analyzer_kotlin.screens.session


import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
    Scaffold(

    ) { paddingValues ->

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

        Box(Modifier.padding(paddingValues)){
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
                        throw Exception("Unexpected upload state: $sessionState")
                    }

                }

            }
        }


    }
}

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
            .padding(16.dp)
    ) {
        // Card displaying network details
        SessionGeneralDetails(sessionUiData.sessionData)

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
                text = { Text("Screenshots (${sessionUiData.sessionData?.screenshots?.size ?: 0})") }
            )
        }

        // Box for displaying scrollable content based on selected tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> RequestsList(
                    sessionUiData.sessionData?.requests,
                    onRequestItemClick,
                    sessionUiData.showFilteringBottomSheet,
                    sessionUiData.isBodyEmptyChecked,
                    sessionUiData.selectedMethods,
                    onToggleShowBottomSheet,
                    onToggleIsBodyEmpty,
                    onModifySelectedMethods,
                    onResetFilters
                )

                1 -> ContentList(sessionUiData.sessionData?.webpageContent, onContentItemClick)
                2 -> ScreenshotsList(
                    sessionUiData.sessionData?.screenshots,
                    switchScreenshotPrivacyOrToSrealted,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons for upload and analysis functionalities
        SessionActionButtons(
            uploadState = uploadState,
            onUploadClick = uploadSession,
            onAnalysisClick = navigateToAutomaticAnalysis
        )
    }
}

@Composable
private fun SessionGeneralDetails(clickedSessionData: SessionData?) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxHeight = 85.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Expand/Collapse Icon Button in top-right corner
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            // Content with max height constraint
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(end = 48.dp) // Add padding to prevent text from going under the icon
                    .then(
                        if (!isExpanded) {
                            Modifier.heightIn(max = maxHeight)
                        } else {
                            Modifier
                        }
                    )
            ) {
                clickedSessionData?.session?.apply {
                    ssid?.let {
                        Text(
                            stringResource(R.string.ssid, it),
                            style = typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
               pcapFilePath?.let {
                   Row(
                       verticalAlignment = Alignment.CenterVertically,
                       modifier = Modifier.padding(vertical = 2.dp)
                   ) {
                       Text(
                           stringResource(R.string.pcap_file, it),
                           style = typography.bodyMedium,
                           maxLines = 1,
                           overflow = TextOverflow.Ellipsis,
                           modifier = Modifier.weight(1f)
                       )
                       Spacer(modifier = Modifier.width(4.dp))
                       Icon(
                           painter = painterResource(id = R.drawable.fiber_new_24px),
                           contentDescription = null,
                           modifier = Modifier.size(16.dp),
                           tint = MaterialTheme.colorScheme.primary
                       )

                   }
               }
                    bssid?.let {
                        Text(
                            stringResource(R.string.bssid, it),
                            style = typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    ipAddress?.let {
                        Text(
                            stringResource(R.string.ip, it),
                            style = typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    gatewayAddress?.let {
                        Text(
                            stringResource(R.string.gateway, it),
                            style = typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    captivePortalUrl?.let {
                        Text(
                            stringResource(R.string.portal_url, it),
                            style = typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable function to display two buttons for a session detail page.
 *
 * One button is for uploading the session to the remote server for analysis, and the other
 * button is for navigating to the automatic AI analysis screen.
 *
 * The button for uploading the session is only enabled if the session has not been uploaded
 * before. If the session is already uploaded, the button is disabled and shows a message
 * saying so. If the session is uploaded successfully, the button is also disabled and shows
 * a message declaring that too.
 *
 * @param uploadState The state of the upload process.
 * @param onUploadClick A function to upload the session to the remote server.
 * @param onAnalysisClick A function to navigate to the automatic AI analysis screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionActionButtons(
    uploadState: SessionState,
    onUploadClick: () -> Unit,
    onAnalysisClick: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        maxItemsInEachRow = Int.MAX_VALUE,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (uploadState) {
            SessionState.AlreadyUploaded -> {
                RoundCornerButton(
                    onClick = { },
                    buttonText = stringResource(R.string.already_uploaded),
                    enabled = false,
                    fillWidth = false
                )
            }

            SessionState.Success -> {
                RoundCornerButton(
                    onClick = { },
                    buttonText = stringResource(R.string.thanks_for_uploading),
                    enabled = false,
                    fillWidth = false
                )
            }

            SessionState.NeverUploaded -> {
                RoundCornerButton(
                    onClick = onUploadClick,
                    buttonText = stringResource(R.string.upload_session_for_analysis),
                    enabled = true,
                    fillWidth = false
                )
            }

            else -> {
                throw IllegalStateException("Unexpected upload state: $uploadState")
            }

        }

        GhostButton(
            onClick = onAnalysisClick,
            buttonText = stringResource(R.string.automatic_analysis_button),
            modifier = Modifier
                .fillMaxWidth()
                .padding( vertical = 8.dp),
        )
    }
}


/**
 * A composable function to display a list of web requests.
 *
 * If the list is empty, it shows a UI component indicating no requests are found.
 * Otherwise, it displays each request in a card format, including URL, method, and type.
 *
 * @param requests A list of CustomWebViewRequestEntity to display.
 */
@Composable
private fun RequestsList(
    requests: List<CustomWebViewRequestEntity>?,
    onRequestItemClick: (CustomWebViewRequestEntity) -> Unit,
    showFilteringBottomSheet: Boolean,
    isBodyEmptyChecked: Boolean,
    selectedMethods: List<Map<RequestMethod, Boolean>>,
    onToggleShowBottomSheet: () -> Unit,
    onToggleIsBodyEmpty: () -> Unit,
    onToggleSelectedMethods: (RequestMethod) -> Unit,
    onResetFilters: () -> Unit
) {


    LazyColumn {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.filters), style = typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleShowBottomSheet) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.filter),
                        contentDescription = if (showFilteringBottomSheet) "Collapse filters" else "Expand filters"
                    )
                }
            }
        }
        if (requests.isNullOrEmpty()) {
            item {
                EmptyListUi(R.string.no_requests_found)
            }
        } else {
            // Group requests based on whether they have full internet access and show each group
            // separately with a header

            // Before Authentication Requests
            item {
                Text(
                    text = stringResource(R.string.before_authentication),
                    style = typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val beforeAuthRequests = requests.filter { !it.hasFullInternetAccess }
            if (beforeAuthRequests.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_requests_found),
                        style = typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(beforeAuthRequests) { request ->
                    RequestListItem(onRequestItemClick, request)
                }
            }

            // After Authentication Requests
            item {
                Text(
                    text = stringResource(R.string.after_authentication),
                    style = typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val afterAuthRequests = requests.filter { it.hasFullInternetAccess }
            if (afterAuthRequests.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_requests_found),
                        style = typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(afterAuthRequests) { request ->
                    RequestListItem(onRequestItemClick, request)
                }
            }
        }
    }

    //Bottom Sheet shown only when user clicks on filter icon
    if (showFilteringBottomSheet)
        FilterBottomSheet(
            onDismiss = onToggleShowBottomSheet,
            isBodyEmptyChecked = isBodyEmptyChecked,
            onToggleIsBodyEmpty = onToggleIsBodyEmpty,
            selectedMethods = selectedMethods,
            onToggleSelectedMethods = onToggleSelectedMethods,
            onResetFilters = onResetFilters
        )
}

/**
 * A composable that displays a [CustomWebViewRequestEntity] in a card layout.
 *
 * @param onRequestItemClick A callback function invoked when the user clicks on the card.
 * @param request The [CustomWebViewRequestEntity] to display.
 */
@Composable
private fun RequestListItem(
    onRequestItemClick: (CustomWebViewRequestEntity) -> Unit,
    request: CustomWebViewRequestEntity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onRequestItemClick(request) }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            request.url?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.url),
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            request.type?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.type),
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(it)
                }
            }
            RequestMethodView(request.method)

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.timestamp_no_param),
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(formatDate(request.timestamp))
            }
            HintTextWithIcon(
                hint = stringResource(R.string.hint_click_to_view_request_content),
                iconResId = R.drawable.tap
            )
        }
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

    val sheetState = rememberModalBottomSheetState()


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.filters), style = typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox to hide requests with empty body
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            // Dropdown to select GET or PUT methods
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.select_method))
                Spacer(modifier = Modifier.width(8.dp))

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    selectedMethods.forEach { method ->
                        CustomChip(
                            label = method.keys.first().name,
                            onClick = { onToggleSelectedMethods(method.keys.first()) },
                            isSelected = method.values.first()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Remove filters button
        GhostButton(
            onClick = onResetFilters,
            buttonText = stringResource(R.string.remove_filters),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

    }
}

//Preview functions

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun SessionScreenContentPreview_Requests() {
    // Provide mock data and a dummy lambda for the preview

    val mockRequests = listOf(
        CustomWebViewRequestEntity(
            customWebViewRequestId = 0,
            sessionId = null,
            type = "API Request",
            url = "https://example.com",
            method = RequestMethod.GET,
            body = null,
            headers = null
        ),
/*        CustomWebViewRequestEntity(
            customWebViewRequestId = 1,
            sessionId = null,
            type = "Form Submission",
            url = "https://another-example.com",
            method = RequestMethod.POST,
            body = "name=value&foo=bar",
            headers = "Content-Type: application/x-www-form-urlencoded"
        ),
        CustomWebViewRequestEntity(
            customWebViewRequestId = 2,
            sessionId = null,
            type = "Data Update",
            url = "https://yet-another-example.com",
            method = RequestMethod.PUT,
            body = "{\"key\": \"value\"}",
            headers = "Content-Type: application/json"
        ),*/
        CustomWebViewRequestEntity(
            customWebViewRequestId = 3,
            sessionId = null,
            type = "Data Update",
            url = "https://yet-another-example.com",
            method = RequestMethod.PUT,
            body = "{\"key\": \"value\"}",
            headers = "Content-Type: application/json",
            hasFullInternetAccess = true
        )
    )

    val mockSessionData = SessionData(
        session = NetworkSessionEntity(
            networkId = "mock",
            ssid = "Captive Portal Check",
            bssid = "00:00:00:00:00:00",
            timestamp = System.currentTimeMillis(),
            captivePortalUrl = null,
            ipAddress = "192.168.1.1",
            gatewayAddress = "192.168.1.254",
            pcapFilePath = "file:///storage/emulated/0/Android/data/com.example.captive_portal_analyzer_kotlin/files/copied_captive_portal_analyzer.pcap",
            isCaptiveLocal = null,
            isUploadedToRemoteServer = false
        ),
        requests = mockRequests,
        screenshots = listOf(),
        webpageContent = listOf(),
        requestsCount = 0,
        screenshotsCount = 0,
        webpageContentCount = 0
    )
    // Create a mock SessionState
    val mockSessionState = SessionState.Success

    val mockSessionUiData = SessionUiData(
        sessionData = mockSessionData,
        showFilteringBottomSheet = false,
        isBodyEmptyChecked = false,
        selectedMethods = emptyList(),
        unfilteredRequests = emptyList()
    )
    AppTheme {
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

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun SessionScreenContentPreview_NoRequests() {
    // Provide mock data and a dummy lambda for the preview


    val mockSessionData = SessionData(
        session = NetworkSessionEntity(
            networkId = "mock",
            ssid = "Captive Portal Check",
            bssid = "00:00:00:00:00:00",
            timestamp = System.currentTimeMillis(),
            captivePortalUrl = null,
            ipAddress = "192.168.1.1",
            gatewayAddress = "192.168.1.254",
            pcapFilePath = "file:///storage/emulated/0/Android/data/com.example.captive_portal_analyzer_kotlin/files/copied_captive_portal_analyzer.pcap",
            isCaptiveLocal = null,
            isUploadedToRemoteServer = false
        ),
        requests = listOf(),
        screenshots = listOf(),
        webpageContent = listOf(),
        requestsCount = 0,
        screenshotsCount = 0,
        webpageContentCount = 0
    )
    // Create a mock SessionState
    val mockSessionState = SessionState.Success

    val mockSessionUiData = SessionUiData(
        sessionData = mockSessionData,
        showFilteringBottomSheet = false,
        isBodyEmptyChecked = false,
        selectedMethods = emptyList(),
        unfilteredRequests = emptyList()
    )
    AppTheme {
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



/**
 * A composable function to display an empty list message.
 *
 * This function takes in a string resource and displays it in a
 * centered box, filling the maximum size of its parent.
 *
 * @param stringRes The string resource to display.
 */
@Composable
private fun EmptyListUi(@StringRes stringRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            stringResource(id = stringRes),
            style = typography.bodyLarge
        )
    }
}

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

    LazyColumn {
        items(content) { item ->
            ContentItem(item, onContentItemClick)
        }
    }
}

@Composable
private fun ContentItem(
    item: WebpageContentEntity,
    onContentItemClick: (WebpageContentEntity) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onContentItemClick(item) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            item.url?.let { Text("URL: $it") }
            Text(stringResource(R.string.html_path, item.htmlContentPath))
            Text(stringResource(R.string.js_path, item.jsContentPath))
            Text(stringResource(R.string.timestamp, formatDate(item.timestamp)))
            HintTextWithIcon(
                hint = stringResource(R.string.hint_click_to_view_content),
                iconResId = R.drawable.tap
            )
        }
    }
}

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

    Column {

        // Display a hint text for selecting privacy-related images
        Text(
            text = stringResource(R.string.hint_select_privacy_images),
            style = typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        // Card to contain the grid of screenshots
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // LazyVerticalGrid to display screenshots in a grid layout
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                // Iterate over each screenshot and display it using ImageItem
                items(
                    items = screenshots,
                    key = { it.screenshotId } // Add key for better performance
                ) { screenshot ->
                    val imagePath = screenshot.path
                    ImageItem(
                        imagePath = imagePath,
                        isSelected = screenshot.isPrivacyOrTosRelated,
                        onImageClick = {
                            toggleScreenshotPrivacyOrToSrelated(screenshot)
                        }
                    )
                }
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
 * @param onImageClick A function to call when the image is clicked, passing the new
 * value of isSelected.
 */
@Composable
fun ImageItem(
    imagePath: String,
    isSelected: Boolean,
    onImageClick: (Boolean) -> Unit
) {
    Box(
        // Outer container with aspect ratio and padding
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onImageClick(!isSelected) }
    ) {
        // Image display
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(data = imagePath.toUri())
                    .build()
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay with text for selected images
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Text indicating the image is related to ToS/Privacy
                Text(
                    text = "ToS/Privacy related screenshot",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A preview of the ImageItem composable function.
 */
@Preview(showBackground = true)
@Composable
fun ImageItemPreview() {
    ImageItem(
        imagePath = "/storage/emulated/0/Android/data/com.example.captive_portal_analyzer_kotlin/files/fb045673-ebc5-42c7-b150-19557cf750be/screenshots/1735872934358.png",
        isSelected = true,
        onImageClick = {}
    )
}