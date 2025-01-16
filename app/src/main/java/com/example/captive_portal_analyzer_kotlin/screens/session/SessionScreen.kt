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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionScreen(
    sharedViewModel: SharedViewModel,
    repository: NetworkSessionRepository,
    navigateToAutomaticAnalysis: () -> Unit,
) {

    val clickedSessionId by sharedViewModel.clickedSessionId.collectAsState()

    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            clickedSessionId = clickedSessionId!!,
        )
    )

    val sessionData by sessionViewModel.sessionData.collectAsState()

    val uploadState by sessionViewModel.uploadState.collectAsState()

    val isConnected by sharedViewModel.isConnected.collectAsState()


    val showToast = { message: String, style: ToastStyle ->
        sharedViewModel.showToast(
            message = message, style = style,
        )
    }

    Scaffold(

    ) { paddingValues ->

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
                    throw Exception("Unexpected upload state: $uploadState")
                }

            }


        }


    }

}


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


@OptIn(ExperimentalLayoutApi::class)
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
        Text(
            text = "Session Details",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Network Details
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

        // Requests, Content, and Screenshots in TabRow
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

        // Scrollable content
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

        SessionActionButtons(
            uploadState = uploadState,
            onUploadClick = uploadSession,
            onAnalysisClick = navigateToAutomaticAnalysis
        )
    }
}


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
                    onClick =onUploadClick,
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


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

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


@Composable
private fun ScreenshotsList(
    screenshots: List<ScreenshotEntity>,
    toggleScreenshotPrivacyOrToSrelated: (ScreenshotEntity) -> Unit,
) {

    if (screenshots.isEmpty()) {
        EmptyListUi(R.string.no_screenshots_found)
        return
    }


    Column() {

        Text(
            text = stringResource(R.string.hint_select_privacy_images),
            style = typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp), // Adjust number of columns as needed
                modifier = Modifier.padding(16.dp)
            ) {
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


@Composable
fun ImageItem(
    imagePath: String,
    isSelected: Boolean,
    onImageClick: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onImageClick(!isSelected) }
    ) {
        // Display the image
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

        // Display the filled box with text when selected
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

@Preview(showBackground = true)
@Composable
fun ImageItemPreview() {
    ImageItem(
        imagePath = "/storage/emulated/0/Android/data/com.example.captive_portal_analyzer_kotlin/files/fb045673-ebc5-42c7-b150-19557cf750be/screenshots/1735872934358.png",
        isSelected = true,
        onImageClick = {}
    )
}
