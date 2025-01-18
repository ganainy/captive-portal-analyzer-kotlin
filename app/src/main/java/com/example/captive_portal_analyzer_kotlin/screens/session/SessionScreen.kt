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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.example.captive_portal_analyzer_kotlin.R

import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.AnimatedNoInternetBanner
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.formatDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable function to display the session details of a network with captive portal.
 *
 * @param sharedViewModel The shared view model containing the clicked session ID and
 * connectivity status.
 * @param repository The repository to access network session data.
 * @param navigateToAutomaticAnalysis A function that navigates to the automatic analysis screen.
 */
@Composable
fun SessionScreen(
    sharedViewModel: SharedViewModel,
    repository: NetworkSessionRepository,
    navigateToAutomaticAnalysis: () -> Unit,
) {
    // Collect the clicked session ID from the shared view model.
    val clickedSessionId by sharedViewModel.clickedSessionId.collectAsState()

    // Create a view model for the session data.
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            clickedSessionId = clickedSessionId!!,
        )
    )

    // Collect the session data from the view model.
    val sessionData by sessionViewModel.sessionData.collectAsState()

    // Collect the upload state from the view model.
    val uploadState by sessionViewModel.uploadState.collectAsState()

    // Collect the connectivity status from the shared view model.
    val isConnected by sharedViewModel.isConnected.collectAsState()

    // A function to show a toast message that will be passed to viewModel to show toast when needed.
    val showToast = { message: String, style: ToastStyle ->
        sharedViewModel.showToast(
            message = message, style = style,
        )
    }
    /**
     * The main composable function for the session screen.
     *
     * It uses a scaffold to display the session details and handles the upload state.
     *
     * @param paddingValues The padding values for the scaffold.
     */
    Scaffold(

    ) { paddingValues ->

        /**
         * Handles the upload state.
         *
         * If the upload state is [UploadState.Uploading], it displays a loading indicator.
         *
         * If the upload state is [UploadState.Loading], it displays a loading indicator.
         *
         * If the upload state is [UploadState.Error], it displays an error component with a retry button.
         *
         * If the upload state is [UploadState.AlreadyUploaded], [UploadState.Success], or [UploadState.NeverUploaded],
         * it displays the session details with only the button changed based on the three different states.
         */
        when (uploadState) {
            UploadState.Uploading -> {
                LoadingIndicator(message = stringResource(R.string.uploading_information_to_be_analyzed))
            }
            UploadState.Loading -> {
                LoadingIndicator(message = stringResource(R.string.loading_session))
            }

            is UploadState.Error -> {
                val errorMessage = (uploadState as UploadState.Error).message
                ErrorComponent(
                    error = errorMessage,
                    onRetryClick = {
                        sessionViewModel.uploadSession(
                            sessionData!!,
                            showToast,
                        )
                    }
                )
            }
            else -> {
                // This branch is for all other UploadState values
                // If the upload state is UploadState.AlreadyUploaded, UploadState.Success, or UploadState.NeverUploaded,
                // it displays the session details with only the button changed based on the three different states.
                if (uploadState is UploadState.AlreadyUploaded || uploadState is UploadState.Success || uploadState is UploadState.NeverUploaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {

                        SessionDetail(
                            clickedSessionData = sessionData!!,
                            uploadSession = {
                                sessionViewModel.uploadSession(
                                    sessionData!!,
                                    showToast,
                                )
                            },
                            switchScreenshotPrivacyOrToSrealted = sessionViewModel::toggleScreenshotPrivacyOrToSrelated,
                            navigateToAutomaticAnalysis = navigateToAutomaticAnalysis,
                            uploadState = uploadState
                        )


                        HintInfoBox(
                            context = LocalContext.current,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        AnimatedNoInternetBanner(isConnected = isConnected)

                    }

                } else {
                    // If the upload state is not recognized, throw an exception
                    throw Exception("Unexpected upload state: $uploadState")
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
 * @param clickedSessionData The SessionData of the clicked session.
 * @param uploadSession A function to upload the session to the remote server.
 * @param uploadState The state of the upload process.
 * @param switchScreenshotPrivacyOrToSrealted A function to switch the privacy of a screenshot or to
 * its related screenshot (as a reaction to user clicking the image).
 * @param navigateToAutomaticAnalysis A function to navigate to the automatic AI analysis screen.
 */
@Composable
fun SessionDetail(
    clickedSessionData: SessionData,
    uploadSession: () -> Unit,
    uploadState: UploadState,
    switchScreenshotPrivacyOrToSrealted: (ScreenshotEntity) -> Unit,
    navigateToAutomaticAnalysis: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title for the session details section
        Text(
            text = "Session Details",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Card displaying network details
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                clickedSessionData.session.ssid?.let { Text("SSID: $it") }
                clickedSessionData.session.bssid?.let { Text("BSSID: $it") }
                clickedSessionData.session.ipAddress?.let { Text("IP: $it") }
                clickedSessionData.session.gatewayAddress?.let { Text("Gateway: $it") }
                clickedSessionData.session.securityType?.let { Text("Security: $it") }
                clickedSessionData.session.captivePortalUrl?.let { Text("Portal URL: $it") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TabRow for navigating between requests, content, and screenshots
        var selectedTab by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Requests (${clickedSessionData.requests.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Content (${clickedSessionData.webpageContent.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Screenshots (${clickedSessionData.screenshots.size})") }
            )
        }

        // Box for displaying scrollable content based on selected tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> RequestsList(clickedSessionData.requests)
                1 -> ContentList(clickedSessionData.webpageContent)
                2 -> ScreenshotsList(
                    clickedSessionData.screenshots,
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
    uploadState: UploadState,
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
            UploadState.AlreadyUploaded -> {
                RoundCornerButton(
                    onClick = { },
                    buttonText = stringResource(R.string.already_uploaded),
                    enabled = false,
                    fillWidth = false
                )
            }

            UploadState.Success -> {
                RoundCornerButton(
                    onClick = { },
                    buttonText = stringResource(R.string.thanks_for_uploading),
                    enabled = false,
                    fillWidth = false
                )
            }

            UploadState.NeverUploaded -> {
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
            text = stringResource(R.string.automatic_analysis_button)
        )
    }
}
/**
 * A preview of the SessionDetail composable.
 *
 * This is a preview of what the SessionDetail composable will look like in different
 * devices and screen sizes.
 *
 * @author Akshay Chordiya
 * @since 1.0
 */
@Preview(name = "Pixel 5", device = "id:pixel_5", showBackground = true)
@Preview(name = "Tablet", device = "id:Nexus 7", showBackground = true)
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SessionDetailPreview() {
    SessionDetail(
        clickedSessionData = SessionData(
            session = NetworkSessionEntity(
                ssid = "test ssid",
                bssid = "test bssid",
                ipAddress = "test ip",
                gatewayAddress = "test gateway",
                securityType = "test security",
                captivePortalUrl = "test portal url",
                timestamp = Date().time,
                networkId = 1.toString(),
                isCaptiveLocal = true,
                isUploadedToRemoteServer = false,
            ),
            requests = emptyList()/*listOf(
                CustomWebViewRequestEntity(
                    sessionId = 11.toString(),
                    type = "GET",
                    url = "https://www.example.com",
                    method = "GET",
                    body = "{}",
                    headers = "{}",
                )
            )*/,
            screenshots = listOf(
                ScreenshotEntity(
                    sessionId = "11",
                    timestamp = Date().time,
                    path = "test path",
                    size = "100KB",
                    url = "test url"
                )
            ),
            webpageContent = listOf(
                WebpageContentEntity(
                    sessionId = 11.toString(),
                    url = "https://www.example.com",
                    htmlContent = "<html><body>This is a test.</body></html>",
                    timestamp = Date().time,
                    contentId = 1234,
                    jsContent = "TODO()",
                )
            )
        ),
        uploadSession = { },
        switchScreenshotPrivacyOrToSrealted = { },
        navigateToAutomaticAnalysis = { },
        uploadState = UploadState.NeverUploaded,

    )
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
private fun RequestsList(requests: List<CustomWebViewRequestEntity>) {

    if (requests.isEmpty()) {
        EmptyListUi(R.string.no_requests_found)
        return
    }

    LazyColumn {
        items(requests) { request ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    request.url?.let { Text("URL: $it") }
                    request.method?.let { Text("Method: $it") }
                    request.type?.let { Text("Type: $it") }
                }
            }
        }
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
            style = MaterialTheme.typography.bodyLarge
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
private fun ContentList(content: List<WebpageContentEntity>) {

    if (content.isEmpty()) {
        EmptyListUi(R.string.no_webpages_found)
        return
    }

    LazyColumn {
        items(content) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    item.url?.let { Text("URL: $it") }
                    Text("Timestamp: ${formatDate(item.timestamp)}")
                }
            }
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
    screenshots: List<ScreenshotEntity>,
    toggleScreenshotPrivacyOrToSrelated: (ScreenshotEntity) -> Unit,
) {

    // Check if the screenshots list is empty and display a message if true
    if (screenshots.isEmpty()) {
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
                    style = MaterialTheme.typography.bodySmall,
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