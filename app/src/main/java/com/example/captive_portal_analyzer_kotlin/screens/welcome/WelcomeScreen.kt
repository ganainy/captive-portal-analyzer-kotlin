package com.example.captive_portal_analyzer_kotlin.screens.welcome

import NetworkSessionRepository
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.BuildConfig
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

//todo use LongPressHintPopup on confusing buttons to show hints
/**
 * The first screen the user will see when opening the app.
 * This screen contains an introduction to the app, its goals and how it works.
 * It also contains a button to start the app.
 */
@Composable
fun WelcomeScreen(
    navigateToSetupPCAPDroidScreen: () -> Unit,
    navigateToManualConnectScreen: () -> Unit,
    skipSetup: Boolean,
    repository: NetworkSessionRepository
) {

    // for testing only: skip pcapdroid setup screen
    var effectiveSkipSetup = skipSetup
    if (BuildConfig.DEBUG_SKIP_PCAP_SETUP_SCREEN) {
        effectiveSkipSetup = true
    }

    // for testing only: insert mock session
    if (BuildConfig.DEBUG_ADD_MOCK_SESSION) {
        insertMockSession(repository)
    }


    Scaffold() { paddingValues ->
        WelcomeContent(
            paddingValues = paddingValues,
            navigateToSetupPCAPDroidScreen = navigateToSetupPCAPDroidScreen,
            navigateToManualConnectScreen = navigateToManualConnectScreen,
            skipSetup = effectiveSkipSetup
        )
    }
}

/**
 * Inserts a predefined mock NetworkSessionEntity into the database
 * via the provided repository instance. Useful for setting up test data.
 *
 * @param repository The instance of NetworkSessionRepository to use for insertion.
 * @param networkId Optional: Provide a specific networkId, otherwise a mock one is generated.
 * @param timestamp Optional: Provide a specific timestamp, otherwise the current time is used.
 */
fun insertMockSession(
    repository: NetworkSessionRepository,
    networkId: String = "mock_network_123",
    timestamp: Long = System.currentTimeMillis()
) {
    // Use a coroutine dispatcher to handle the suspend functionality
    GlobalScope.launch(Dispatchers.IO) {
        // Create a NetworkSessionEntity instance with comprehensive mock data
        val mockSession = NetworkSessionEntity(
            networkId = networkId,
            ssid = "MockWiFi_SSID",
            bssid = "00:1A:2B:3C:4D:5E", // Example MAC address format
            timestamp = timestamp,
            captivePortalUrl = "http://mock.captive.portal/login?id=$networkId", // Example URL
            ipAddress = "192.168.1.101", // Example private IP
            gatewayAddress = "192.168.1.1", // Example gateway IP
            isCaptiveLocal = false, // Example value
            isUploadedToRemoteServer = false, // Typically false on initial insertion
            pcapFilePath = BuildConfig.DEBUG_LARGE_PCAP_FILE_PATH,
            pcapFileUrl = null // Typically null until uploaded
            // Or set a mock URL if testing uploaded state:
            // pcapFileUrl = "https://your.server.com/uploads/$networkId.pcap"
        )

        // Use the repository's actual insert function to add the mock session
        repository.insertSession(mockSession)

        // --- 2. Insert related ScreenshotEntities ---

        val screenshotUrls = listOf(
            "http://example.com/screenshot1.png",
            "http://example.com/screenshot2.png",
            "http://example.com/screenshot3.png"
        )

        screenshotUrls.forEach { url ->
            val mockScreenshot = ScreenshotEntity(
                screenshotId = 1,
                url = url,
                size = "1024 * 100", // 100KB example size
                sessionId = networkId.toString(),
                timestamp = System.currentTimeMillis(),
                path = "/storage/emulated/0/DCIM/Screenshots/Screenshot_2025-04-04-13-57-39-990_com.google.android.gm.jpg", // Example local path
                isPrivacyOrTosRelated = false
            )
            repository.insertScreenshot(mockScreenshot)
            println("Inserted mock screenshot for session $networkId: $url")
        }

        // --- 3. Insert related CustomWebViewRequestEntities ---

        val requestData = listOf(
            CustomWebViewRequestEntity(
                customWebViewRequestId = 1,
                sessionId = networkId, // Use the SAME session ID!
                type = "document",
                url = "http://example.com/",
                method = RequestMethod.GET,
                body = null,
                headers = "{ \"User-Agent\": \"MockBrowser\" }"
            ),
            CustomWebViewRequestEntity(
                customWebViewRequestId = 2,
                sessionId = networkId, // Use the SAME session ID!
                type = "script",
                url = "http://example.com/script.js",
                method = RequestMethod.GET,
                body = null,
                headers = null
            ),
            CustomWebViewRequestEntity(
                customWebViewRequestId = 3,
                sessionId = networkId, // Use the SAME session ID!
                type = "image",
                url = "http://example.com/image.png",
                method = RequestMethod.GET,
                body = null,
                headers = null
            ),
            CustomWebViewRequestEntity(
                customWebViewRequestId = 4,
                sessionId = networkId, // Use the SAME session ID!
                type = "POST",
                url = "http://example.com/post_url",
                method = RequestMethod.POST,
                body = "{ \"key\": \"value\" }",
                headers = null,
                hasFullInternetAccess = true,
            )
        )

        requestData.forEach { request ->
            repository.insertRequest(request)
            println("Inserted mock request for session $networkId: ${request.url}")
        }

        // --- 4. Insert related WebpageContentEntities ---

        val webpageContentData = listOf(
            WebpageContentEntity(
                contentId = 1,
                sessionId = networkId,  // Use the SAME session ID!
                url = "http://example.com/",
                htmlContentPath = "/data/data/com.yourapp/files/html/$networkId.html",
                jsContentPath = "/data/data/com.yourapp/files/js/$networkId.js",
                timestamp = timestamp
            ),
            WebpageContentEntity(
                contentId = 2,
                sessionId = networkId,  // Use the SAME session ID!
                url = "http://example.com/page2",
                htmlContentPath = "/data/data/com.yourapp/files/html/$networkId-page2.html",
                jsContentPath = "/data/data/com.yourapp/files/js/$networkId-page2.js",
                timestamp = timestamp
            )
        )

        webpageContentData.forEach { content ->
            repository.insertWebpageContent(content)
            println("Inserted mock webpage content for session $networkId: ${content.url}")
        }
    }

    Log.d("MockData", "Inserted mock session with networkId: $networkId")
}


/**
 * The content of the welcome screen.
 * This composable contains the app title, its goal and how it works.
 * It also contains a button to start the app.
 */
@Composable
private fun WelcomeContent(
    paddingValues: PaddingValues,
    navigateToSetupPCAPDroidScreen: () -> Unit,
    navigateToManualConnectScreen: () -> Unit,
    skipSetup: Boolean,
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .background(color = colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = stringResource(R.string.welcome_to_captive_portal_analyzer),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        //App goal
        Text(
            text = stringResource(R.string.this_app_is_designed_to_assist_in_the_collection_of_data_related_to_captive_portals_with_the_aim_of_enhancing_user_security_and_ensuring_data_privacy),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        // App how to use description
        Text(
            text = stringResource(R.string.start_by_creating_a_session_and_login_to_your_network_with_a_captive_portal_we_ll_guide_you_step_by_step),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Start app button
        RoundCornerButton(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = if (skipSetup) navigateToManualConnectScreen else navigateToSetupPCAPDroidScreen,
            buttonText = stringResource(R.string.start),
        )


    }

}

/**
 * Preview for the welcome content.
 * This preview shows the welcome content with a light background and a dark background.
 */
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun WelcomeContentPreview() {
    AppTheme {
        WelcomeContent(
            paddingValues = PaddingValues(0.dp),
            navigateToSetupPCAPDroidScreen = {},
            navigateToManualConnectScreen = {},
            skipSetup = false,
        )
    }
}