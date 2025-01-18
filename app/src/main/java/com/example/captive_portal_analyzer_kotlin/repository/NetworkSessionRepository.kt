import android.net.Uri
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.room.CustomWebViewRequestDao
import com.example.captive_portal_analyzer_kotlin.room.NetworkSessionDao
import com.example.captive_portal_analyzer_kotlin.room.ScreenshotDao
import com.example.captive_portal_analyzer_kotlin.room.WebpageContentDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Repository class for handling Session related operations both locally and remotely.
 * This class is used to abstract away the underlying database operations from the UI.
 * It provides a single point of entry for all operations related to Session management.
 */
class NetworkSessionRepository(
    /**
     * The DAO for handling NetworkSession database operations.
     */
    private val sessionDao: NetworkSessionDao,
    /**
     * The DAO for handling CustomWebViewRequest database operations.
     */
    private val requestDao: CustomWebViewRequestDao,
    /**
     * The DAO for handling Screenshot database operations.
     */
    private val screenshotDao: ScreenshotDao,
    /**
     * The DAO for handling WebpageContent database operations.
     */
    private val webpageContentDao: WebpageContentDao
) {
    /**
     * Firebase Firestore instance for storing/retrieving data on the Firebase server.
     */
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Firebase Storage instance for storing/retrieving files (images) on the Firebase server.
     */
    private val storage = FirebaseStorage.getInstance()

    // Network Session Operations
    // ---------------------------

    /**
     * Inserts a NetworkSessionEntity into the Room database.
     */
    suspend fun insertSession(session: NetworkSessionEntity) {
        sessionDao.insert(session)
    }

    /**
     * Retrieves a NetworkSessionEntity from the Room database by its networkId.
     */
    suspend fun getSessionByNetworkId(networkId: String): NetworkSessionEntity? {
        return sessionDao.getSessionByNetworkId(networkId)
    }

    /**
     * Updates a NetworkSessionEntity in the Room database.
     */
    suspend fun updateSession(session: NetworkSessionEntity) {
        sessionDao.update(session)
    }

    /**
     * Updates the portalUrl of a NetworkSessionEntity in the Room database.
     */
    suspend fun updatePortalUrl(sessionId: String, portalUrl: String) {
        sessionDao.updatePortalUrl(sessionId, portalUrl)
    }

    /**
     * Updates the isCaptiveLocal flag of a NetworkSessionEntity in the Room database.
     */
    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean) {
        sessionDao.updateIsCaptiveLocal(sessionId, isLocal)
    }


    /**
     * Retrieves a list of CustomWebViewRequestEntity associated with a network session
     * from the Room database.
     */
    fun getSessionRequests(sessionId: String):  Flow<List<CustomWebViewRequestEntity>> =
        requestDao.getSessionRequestsList(sessionId)

    /**
     * Inserts a CustomWebViewRequestEntity into the Room database, but only if it is
     * unique based on the following criteria:
     *  - request.sessionId
     *  - request.type
     *  - request.url
     *  - request.method
     *  - request.body
     *  - request.headers
     */
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

    // Screenshot Operations
    // ---------------------
    // These functions manage the ScreenshotEntity dataclass in the Room database.

    /**
     * Inserts a screenshot into the Room database if it is unique.
     * A screenshot is considered unique if there is no existing entry with the same URL, size, and sessionId.
     *
     * @param screenshot The screenshot entity to be inserted.
     */
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

    /**
     * Updates an existing screenshot in the Room database.
     *
     * @param screenshotEntity The screenshot entity with updated information.
     */
    suspend fun updateScreenshot(screenshotEntity: ScreenshotEntity) =
        screenshotDao.update(screenshotEntity)

    /**
     * Retrieves a list of screenshots associated with a specific session from the Room database.
     *
     * @param sessionId The ID of the session for which screenshots are to be retrieved.
     * @return A Flow emitting a list of screenshots for the specified session.
     */
    private fun getSessionScreenshots(sessionId: String): Flow<List<ScreenshotEntity>> =
        screenshotDao.getSessionScreenshotsList(sessionId)

    // Webpage Content Operations
    // --------------------------
    // These functions manage the WebpageContentEntity dataclass in the Room database.

    /**
     * Inserts a webpage content (HTML and JS) into the Room database if it is unique.
     * A webpage content is considered unique if there is no existing entry with the same HTML content,
     * JS content, and sessionId.
     *
     * @param content The webpage content entity to be inserted.
     */
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

    /**
     * Retrieves a list of webpage content associated with a specific session from the Room database.
     *
     * @param sessionId The ID of the session for which webpage content are to be retrieved.
     * @return A Flow emitting a list of webpage content for the specified session.
     */
    fun getSessionWebpageContent(sessionId: String): Flow<List<WebpageContentEntity>> =
        webpageContentDao.getSessionWebpageContentList(sessionId)

    // Remote Operations
    // -----------------
    // These functions manage the communication with the Firebase Remote database.
    // They take care of uploading the session data/screenshots to the remote database.

    /**
     * Uploads session data to the remote Firestore database.
     *
     * @param sessionId The ID of the session to be uploaded.
     * @return A Result wrapping a Unit in case of success or an Exception in case of failure.
     */
    suspend fun uploadSessionData(sessionId: String): Result<Unit> {
        return try {
            // 1. Collect all session data
            val session = sessionDao.getSession(sessionId)
                ?: return Result.failure(Exception("Session not found"))

            val requests = withTimeout(30_000) {
                requestDao.getSessionRequestsList(sessionId).first()
            }
            val screenshots = withTimeout(30_000) {
                screenshotDao.getSessionScreenshotsList(sessionId).first()
            }
            val webpageContent = withTimeout(30_000) {
                webpageContentDao.getSessionWebpageContentList(sessionId).first()
            }

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
            withTimeout(60_000) {
                firestore.collection("sessions")
                    .document(sessionId)
                    .set(sessionData)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a single screenshot to the Storage and returns the download URL.
     *
     * @param screenshot The screenshot entity to be uploaded.
     * @param sessionId The ID of the session to which the screenshot belongs.
     * @return A Result wrapping the download URL of the uploaded screenshot or an Exception in case of failure.
     */
    private suspend fun uploadScreenshot(
        screenshot: ScreenshotEntity,
        sessionId: String
    ): Result<String> {
        return try {
            val imageFile = File(screenshot.path)
            val imageUri = Uri.fromFile(imageFile)
            val storageRef = storage.reference
                .child("images/$sessionId/${screenshot.screenshotId}.jpg")

            withTimeout(60_000) {
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await()
                downloadUrl.toString()
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a Flow of List of SessionData that contains all session data from the local Room database.
     *
     * The Flow is a combination of all the flows returned by [getCompleteSessionData] for each session.
     *
     * @return A Flow of List of SessionData
     */
    suspend fun getCompleteSessionDataList(): Flow<List<SessionData>> = flow {
        try {
            val allSessions = sessionDao.getAllSessions() // Assuming this returns List<Session>

            if (allSessions.isNullOrEmpty()) {
                emit(emptyList())
                return@flow
            }

            // Convert list of sessions into list of SessionData
            val allSessionsData = allSessions.map { session ->
                getCompleteSessionData(session.networkId)
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

    /**
     * Returns a Flow of Complete SessionData/Request/Screenshot/WebpageContent for the given session.
     *
     * The Flow is a combination of the flows returned by:
     * - [getSessionRequests]
     * - [getSessionScreenshots]
     * - [getSessionWebpageContent]
     *
     * @param sessionId The ID of the session
     * @return A Flow of SessionData
     */
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



