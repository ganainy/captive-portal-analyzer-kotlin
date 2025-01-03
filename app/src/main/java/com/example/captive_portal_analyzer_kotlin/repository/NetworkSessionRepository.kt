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

    suspend fun updatePortalUrl(sessionId: String, portalUrl: String) {
        sessionDao.updatePortalUrl(sessionId, portalUrl)
    }

    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean) {
        sessionDao.updateIsCaptiveLocal(sessionId, isLocal)
    }

    suspend fun updateIsUploadedToRemoteServer(sessionId: String, isUploadedToRemoteServer: Boolean) {
        sessionDao.updateIsUploadedToRemoteServer(sessionId, isUploadedToRemoteServer)
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

    fun getSessionRequests(sessionId: String): List<CustomWebViewRequestEntity> =
        requestDao.getSessionRequestsList(sessionId)

    suspend fun insertRequest(request: CustomWebViewRequestEntity) =
        requestDao.insert(request)

    suspend fun deleteRequest(request: CustomWebViewRequestEntity) =
        requestDao.delete(request)

    // Screenshot Operations
    suspend fun insertScreenshot(screenshot: ScreenshotEntity) {
        screenshotDao.insert(screenshot)
    }

    fun getScreenshot(screenshotId: String): Flow<ScreenshotEntity?> =
        screenshotDao.getScreenshot(screenshotId)

    fun getSessionScreenshots(sessionId: String): List<ScreenshotEntity> =
        screenshotDao.getSessionScreenshotsList(sessionId)

    // Webpage Content Operations
    suspend fun insertWebpageContent(content: WebpageContentEntity) {
        webpageContentDao.insert(content)
    }

    fun getSessionWebpageContent(sessionId: String): List<WebpageContentEntity> =
        webpageContentDao.getSessionWebpageContentList(sessionId)

    // Remote Operations
    suspend fun uploadSessionData(sessionId: String): Result<Unit> {
        return try {
            // 1. Collect all session data
            val session = sessionDao.getSession(sessionId) ?:
            return Result.failure(Exception("Session not found"))

            val requests = requestDao.getSessionRequestsList(sessionId)
            val screenshots = screenshotDao.getSessionScreenshotsList(sessionId)
            val webpageContent = webpageContentDao.getSessionWebpageContentList(sessionId)

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




    // Utility function to get complete session data
   /* suspend fun getCompleteSessionData(sessionId: String): Flow<SessionData> = flow {
        try {
            val session = sessionDao.getSession(sessionId)
            if (session != null) {
                combine(
                    getSessionRequests(sessionId),
                    getSessionScreenshots(sessionId),
                    getSessionWebpageContent(sessionId)
                ) { requests, screenshots, content ->
                    SessionData(
                        session = session,
                        requests = requests,
                        screenshots = screenshots,
                        webpageContent = content
                    )
                }.collect { sessionData ->
                    emit(sessionData)
                }
            } else {
                throw Exception("Session not found")
            }
        } catch (e: Exception) {
            throw e
        }
    }*/


}

// Data class to hold complete session data
data class SessionData(
    val session: NetworkSessionEntity,
    val requests: List<CustomWebViewRequestEntity>,
    val screenshots: List<ScreenshotEntity>,
    val webpageContent: List<WebpageContentEntity>
)



