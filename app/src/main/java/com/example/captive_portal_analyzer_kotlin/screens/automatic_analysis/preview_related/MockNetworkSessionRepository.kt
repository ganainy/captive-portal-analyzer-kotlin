package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related


// Mock implementation of NetworkSessionRepository for testing/preview purposes only

import NetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.room.CustomWebViewRequestDao
import com.example.captive_portal_analyzer_kotlin.room.NetworkSessionDao
import com.example.captive_portal_analyzer_kotlin.room.ScreenshotDao
import com.example.captive_portal_analyzer_kotlin.room.WebpageContentDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


// This class remains simple, passing mock DAOs to the superclass.
class MockNetworkSessionRepository(
    sessionDao: NetworkSessionDao,
    requestDao: CustomWebViewRequestDao,
    screenshotDao: ScreenshotDao,
    webpageContentDao: WebpageContentDao
) : NetworkSessionRepository(sessionDao, requestDao, screenshotDao, webpageContentDao) {
    // No overrides needed here for this simple mock approach
}

// Factory function to create the Mock Repository with mock DAOs returning fixed data
fun mockNetworkSessionRepository(): NetworkSessionRepository {
    val mockSessionId = "fixed-mock-session-id"
    val currentTime = System.currentTimeMillis()

    // --- Fixed Mock Data Objects ---
    val fixedSession = NetworkSessionEntity(
        networkId = mockSessionId,
        ssid = "Fixed Mock SSID",
        bssid = "AA:BB:CC:DD:EE:FF",
        timestamp = currentTime - 100000,
        captivePortalUrl = "http://fixed.portal/login",
        ipAddress = "192.168.43.10",
        gatewayAddress = "192.168.43.1",
        isCaptiveLocal = true,
        isUploadedToRemoteServer = false,
        pcapFilePath = "/path/to/mock.pcap",
        pcapFileUrl = null
    )

    val fixedRequests = listOf(
        CustomWebViewRequestEntity(101, mockSessionId, "initial", "http://clients3.google.com/generate_204", RequestMethod.GET, null, "{}", false, currentTime - 50000),
        CustomWebViewRequestEntity(102, mockSessionId, "redirect", "http://fixed.portal/login", RequestMethod.GET, null, "{}", false, currentTime - 40000),
        CustomWebViewRequestEntity(103, mockSessionId, "submit", "http://fixed.portal/auth", RequestMethod.POST, "user=mock", "{\"Content-Type\":\"application/x-www-form-urlencoded\"}", false, currentTime - 30000),
        CustomWebViewRequestEntity(104, mockSessionId, "success", "http://example.com", RequestMethod.GET, null, "{}", true, currentTime - 20000)
    )

    val fixedScreenshots = listOf(
        ScreenshotEntity(201, mockSessionId, currentTime - 35000, "/mock/path/ss1.png", "95KB", "http://fixed.portal/login", false),
        ScreenshotEntity(202, mockSessionId, currentTime - 25000, "/mock/path/ss2_tos.png", "150KB", "http://fixed.portal/terms", true)
    )

    val fixedWebpageContents = listOf(
        WebpageContentEntity(301, mockSessionId, "http://fixed.portal/login", "/mock/html/login.html", "/mock/js/login.js", currentTime - 38000),
        WebpageContentEntity(302, mockSessionId, "http://fixed.portal/terms", "/mock/html/terms.html", "/mock/js/terms.js", currentTime - 28000)
    )


    // --- Mock NetworkSessionDao (Fixed Data) ---
    val sessionDao = object : NetworkSessionDao {
        override suspend fun insert(session: NetworkSessionEntity) { /* No-op */ }
        override suspend fun update(session: NetworkSessionEntity) { /* No-op */ }

        override suspend fun getSession(sessionId: String): NetworkSessionEntity? {
            return fixedSession // Always return the same fixed session
        }

        override suspend fun updatePortalUrl(sessionId: String, portalUrl: String) { /* No-op */ }
        override suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean) { /* No-op */ }

        override suspend fun getSessionBySsid(ssid: String?): NetworkSessionEntity? {
            return fixedSession // Always return the same fixed session
        }

        override fun getAllSessions(): Flow<List<NetworkSessionEntity>?> {
            return flowOf(listOf(fixedSession)) // Return flow of fixed list
        }

        override fun updateIsUploadedToRemoteServer(sessionId: String, isUploadedToRemoteServer: Boolean) { /* No-op */ }

        override fun getSessionFlow(sessionId: String): Flow<NetworkSessionEntity?> {
            return flowOf(fixedSession) // Return flow of the fixed session
        }

        override fun getSessionByNetworkId(networkId: String): NetworkSessionEntity? {
            return fixedSession // Always return the same fixed session
        }
    }

    // --- Mock CustomWebViewRequestDao (Fixed Data) ---
    val requestDao = object : CustomWebViewRequestDao {
        override suspend fun insert(customWebViewRequest: CustomWebViewRequestEntity) { /* No-op */ }
        override suspend fun update(customWebViewRequest: CustomWebViewRequestEntity) { /* No-op */ }
        override suspend fun delete(customWebViewRequest: CustomWebViewRequestEntity) { /* No-op */ }
        override fun getCustomWebViewRequest(customWebViewRequestId: Int): Flow<CustomWebViewRequestEntity> {
            return flowOf(fixedRequests.firstOrNull()) as Flow<CustomWebViewRequestEntity>
        }

        override fun getSessionRequestsList(sessionId: String): Flow<List<CustomWebViewRequestEntity>> {
            return flowOf(fixedRequests) // Always return the full fixed list
        }

        override fun getAllCustomWebViewRequest(): Flow<List<CustomWebViewRequestEntity>> {
            return flowOf(fixedRequests) // Always return the full fixed list
        }

        override suspend fun isRequestUnique(
            sessionId: String?,
            type: String?,
            url: String?,
            method: String?,
            body: String?,
            headers: String?
        ): Int {
            return 1
        }

        override suspend fun getSessionRequestsCount(sessionId: String): Int {
            return fixedRequests.size // Return count of the fixed list
        }
    }

    // --- Mock ScreenshotDao (Fixed Data) ---
    val screenshotDao = object : ScreenshotDao {
        override suspend fun insert(screenshot: ScreenshotEntity) { /* No-op */ }
        override suspend fun update(screenshot: ScreenshotEntity) { /* No-op */ }
        override suspend fun delete(screenshot: ScreenshotEntity) { /* No-op */ }
        override fun getScreenshot(screenshotId: String): Flow<ScreenshotEntity> {
            return flowOf(fixedScreenshots.firstOrNull()) as Flow<ScreenshotEntity>
        }

        override fun getSessionScreenshotsList(sessionId: String): Flow<List<ScreenshotEntity>> {
            return flowOf(fixedScreenshots) // Always return the full fixed list
        }

        override fun isScreenshotUnique(url: String?, size: String?, sessionId: String): Int { // Assuming Int return
            return 1 // Assume always unique
        }

        override fun getSessionScreenshotsCount(sessionId: String): Int { // Assuming Int return
            return fixedScreenshots.size // Return count of the fixed list
        }
    }

    // --- Mock WebpageContentDao (Fixed Data) ---
    val webpageContentDao = object : WebpageContentDao {
        override fun getAllContentStream(): Flow<List<WebpageContentEntity>> {
            return flowOf(fixedWebpageContents) // Always return the full fixed list
        }

        override fun getContentForUrlStream(url: String): Flow<WebpageContentEntity?> {
            // Return the first fixed content, or null if the list is empty
            return flowOf(fixedWebpageContents.firstOrNull())
        }

        override fun getLatestContentStream(): Flow<List<WebpageContentEntity>> {
            return flowOf(fixedWebpageContents) // Always return the full fixed list (simplistic mock)
        }

        override suspend fun insert(content: WebpageContentEntity) { /* No-op */ }
        override suspend fun delete(content: WebpageContentEntity) { /* No-op */ }
        override suspend fun deleteByUrl(url: String) { /* No-op */ }

        override fun getSessionWebpageContentList(sessionId: String): Flow<List<WebpageContentEntity>> {
            return flowOf(fixedWebpageContents) // Always return the full fixed list
        }

        override fun isWebpageContentUnique(htmlContentPath: String, jsContentPath: String, sessionId: String): Int { // Assuming Int return
            return 1 // Assume always unique
        }

        override fun getWebpageContentCountForSession(sessionId: String): Int { // Assuming Int return
            return fixedWebpageContents.size // Return count of the fixed list
        }
    }

    // --- Instantiate the Mock Repository ---
    return MockNetworkSessionRepository(sessionDao, requestDao, screenshotDao, webpageContentDao)
}