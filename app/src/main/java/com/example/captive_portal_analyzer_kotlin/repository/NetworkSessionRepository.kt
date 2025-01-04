import android.net.Uri
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.room.CustomWebViewRequestDao
import com.example.captive_portal_analyzer_kotlin.room.NetworkSessionDao
import com.example.captive_portal_analyzer_kotlin.room.ScreenshotDao
import com.example.captive_portal_analyzer_kotlin.room.WebpageContentDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File

class NetworkSessionRepository(
    private val sessionDao: NetworkSessionDao,
    private val requestDao: CustomWebViewRequestDao,
    private val screenshotDao: ScreenshotDao,
    private val webpageContentDao: WebpageContentDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Network Session Operations
    suspend fun insertSession(session: NetworkSessionEntity) {
        sessionDao.insert(session)
    }

    suspend fun updateSession(session: NetworkSessionEntity) {
        sessionDao.update(session)
    }

    suspend fun updatePortalUrl(sessionId: String, portalUrl: String) {
        sessionDao.updatePortalUrl(sessionId, portalUrl)
    }

    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean) {
        sessionDao.updateIsCaptiveLocal(sessionId, isLocal)
    }

    suspend fun getSessionByBssid(bssid: String?): NetworkSessionEntity? {
        return sessionDao.getSessionByBssid(bssid)
    }

    suspend fun getAllSessions(): List<NetworkSessionEntity>? {
        return sessionDao.getAllSessions()
    }

    // Custom WebView Request Operations
    fun getAllRequests(): Flow<List<CustomWebViewRequestEntity>> =
        requestDao.getAllCustomWebViewRequest()

    fun getSessionRequests(sessionId: String):  Flow<List<CustomWebViewRequestEntity>> =
        requestDao.getSessionRequestsList(sessionId)

    suspend fun insertRequest(request: CustomWebViewRequestEntity) {
        val isUnique = requestDao.isRequestUnique(
            request.sessionId,
            request.type,
            request.url,
            request.method,
            request.body,
            request.headers
        ) == 0
        if (isUnique) {
            requestDao.insert(request)
        }
    }


    suspend fun deleteRequest(request: CustomWebViewRequestEntity) =
        requestDao.delete(request)

    // Screenshot Operations
    suspend fun insertScreenshot(screenshot: ScreenshotEntity) {
        val isUnique = screenshotDao.isScreenshotUnique(
            screenshot.url,
            screenshot.size,
            screenshot.sessionId
        ) == 0
        if (isUnique) {
            screenshotDao.insert(screenshot)
        }
    }

    suspend fun updateScreenshot(screenshotEntity: ScreenshotEntity) =
        screenshotDao.update(screenshotEntity)

    fun getScreenshot(screenshotId: String): Flow<ScreenshotEntity?> =
        screenshotDao.getScreenshot(screenshotId)

    fun getSessionScreenshots(sessionId: String): Flow<List<ScreenshotEntity>> =
        screenshotDao.getSessionScreenshotsList(sessionId)

    // Webpage Content Operations
    suspend fun insertWebpageContent(content: WebpageContentEntity) {
        val isUnique = webpageContentDao.isWebpageContentUnique(
            content.htmlContent,
            content.jsContent,
            content.sessionId
        ) == 0
        if (isUnique) {
            webpageContentDao.insert(content)
        }
    }

    fun getSessionWebpageContent(sessionId: String): Flow<List<WebpageContentEntity>> =
        webpageContentDao.getSessionWebpageContentList(sessionId)

    // Remote Operations
    suspend fun uploadSessionData(sessionId: String): Result<Unit> {
        return try {
            // 1. Collect all session data
            val session = sessionDao.getSession(sessionId)
                ?: return Result.failure(Exception("Session not found"))

            val requests = requestDao.getSessionRequestsList(sessionId).last()
            val screenshots = screenshotDao.getSessionScreenshotsList(sessionId).last()
            val webpageContent = webpageContentDao.getSessionWebpageContentList(sessionId).last()

            // 2. Upload screenshots to Storage and get URLs
            val uploadedScreenshots = screenshots.map { screenshot ->
                val uploadResult = uploadScreenshot(screenshot, sessionId)
                screenshot.copy(url = uploadResult.getOrNull())
            }

            // 3. Create report
            val sessionData = SessionData(
                session = session,
                requests = requests,
                screenshots = uploadedScreenshots,
                webpageContent = webpageContent
            )

            // 4. Upload to Firestore
            firestore.collection("sessions")
                .document(sessionId)
                .set(sessionData)
                .await()

            // 5. Update local status
            sessionDao.updateIsUploadedToRemoteServer(sessionId, true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadScreenshot(
        screenshot: ScreenshotEntity,
        sessionId: String
    ): Result<String> {
        return try {
            val imageFile = File(screenshot.path)
            val imageUri = Uri.fromFile(imageFile)
            val storageRef = storage.reference
                .child("images/$sessionId/${screenshot.screenshotId}.jpg")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getCompleteSessionDataList(): Flow<List<SessionData>> = flow {
        try {
            val allSessions = sessionDao.getAllSessions() // Assuming this returns List<Session>

            if (allSessions.isNullOrEmpty()) {
                emit(emptyList())
                return@flow
            }

            // Convert list of sessions into list of SessionData
            val allSessionsData = allSessions.map { session ->
                getCompleteSessionData(session.sessionId)
            }

            // Combine all flows into a single flow of list
            combine(allSessionsData) { sessionDataArray ->
                sessionDataArray.toList()
            }.collect { combinedList ->
                emit(combinedList)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    // Utility function to get complete session data
    suspend fun getCompleteSessionData(sessionId: String): Flow<SessionData> = flow {
        try {
            val session = sessionDao.getSession(sessionId) ?: throw Exception("Session not found")

            combine(
                (getSessionRequests(sessionId)),
                (getSessionScreenshots(sessionId)),
                (getSessionWebpageContent(sessionId))
            ) { requests: List<CustomWebViewRequestEntity>,
                screenshots: List<ScreenshotEntity>,
                content: List<WebpageContentEntity> ->
                SessionData(
                    session = session,
                    requests = requests,
                    screenshots = screenshots,
                    webpageContent = content
                )
            }.collect { sessionData ->
                emit(sessionData)
            }
        } catch (e: Exception) {
            throw e
        }
    }

}

// Data class to hold complete session data
data class SessionData(
    val session: NetworkSessionEntity,
    val requests: List<CustomWebViewRequestEntity>,
    val screenshots: List<ScreenshotEntity>,
    val webpageContent: List<WebpageContentEntity>
)



