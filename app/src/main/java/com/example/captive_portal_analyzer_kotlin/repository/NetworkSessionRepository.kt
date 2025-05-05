import android.util.Log
import androidx.room.Transaction
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.convertMethodEnumToString
import com.example.captive_portal_analyzer_kotlin.repository.SessionUploader
import com.example.captive_portal_analyzer_kotlin.repository.UploadStatus
import com.example.captive_portal_analyzer_kotlin.room.CustomWebViewRequestDao
import com.example.captive_portal_analyzer_kotlin.room.NetworkSessionDao
import com.example.captive_portal_analyzer_kotlin.room.ScreenshotDao
import com.example.captive_portal_analyzer_kotlin.room.WebpageContentDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * Repository class for handling Session related operations both locally and remotely.
 * This class is used to abstract away the underlying database operations from the UI.
 * It provides a single point of entry for all operations related to Session management.
 */
open class NetworkSessionRepository(
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
    private val webpageContentDao: WebpageContentDao,
    /**
     * The SessionUploader instance for handling session uploads to Firebase.
     */
    private val sessionUploader: SessionUploader?
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
     * Inserts a NetworkSessionEntity into the Room database.
     */
    suspend fun getSessionBySessionId(sessionId: String): NetworkSessionEntity? {
        return sessionDao.getSession(sessionId)
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
    fun getSessionRequests(sessionId: String): Flow<List<CustomWebViewRequestEntity>> =
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
            convertMethodEnumToString(request.method),
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
    internal fun getSessionScreenshots(sessionId: String): Flow<List<ScreenshotEntity>> =
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
            content.htmlContentPath,
            content.jsContentPath,
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


    /**
     * Initiates the upload of session data to the remote Firestore database and Storage.
     * Provides progress and status updates via a Flow.
     *
     * @param sessionId The ID of the session to be uploaded.
     * @return A Flow emitting UploadStatus updates throughout the upload process.
     */
    // Changed return type to Flow<UploadStatus>
    fun uploadSessionData(sessionId: String): Flow<UploadStatus> {
        // Delegate the entire upload process and flow emission to the SessionUploader instance
        // The SessionUploader's uploadSessionData function already returns the desired Flow

        if (sessionUploader == null) {
            throw IllegalStateException("SessionUploader is not initialized.")
        }

        return sessionUploader.uploadSessionData(sessionId)
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
            val allSessions =
                sessionDao.getAllSessions() // Assuming this returns Flow<List<NetworkSessionEntity>?>

            allSessions.collect { sessions ->
                val allSessionsData = sessions?.map { session ->
                    val requestsCount = requestDao.getSessionRequestsCount(session.networkId)
                    val screenshotsCount =
                        screenshotDao.getSessionScreenshotsCount(session.networkId)
                    val webpageContentCount =
                        webpageContentDao.getWebpageContentCountForSession(session.networkId)
                    SessionData(
                        session = session,
                        requestsCount = requestsCount,
                        screenshotsCount = screenshotsCount,
                        webpageContentCount = webpageContentCount,
                    )
                } ?: emptyList()

                emit(allSessionsData)
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


    /**
     * Deletes a session and all its related data (requests, screenshots, webpage content)
     * from the local Room database atomically.
     *
     * @param networkId The networkId (used as sessionId) of the session to delete.
     */
    @Transaction // Ensures atomicity: all deletions succeed or none do.
    open suspend fun deleteSessionAndRelatedData(networkId: String) {
        Log.d("Repository", "Attempting to delete data for session ID: $networkId")
        // 1. Delete related data first (using networkId as sessionId)
        val deletedRequests = requestDao.deleteRequestsBySessionId(networkId)
        Log.d("Repository", "Deleted $deletedRequests requests for session $networkId")

        val deletedScreenshots = screenshotDao.deleteScreenshotsBySessionId(networkId)
        Log.d("Repository", "Deleted $deletedScreenshots screenshots for session $networkId")

        val deletedWebpages = webpageContentDao.deleteWebpageContentBySessionId(networkId)
        Log.d("Repository", "Deleted $deletedWebpages webpage contents for session $networkId")

        // 2. Delete the main session entity
        val deletedSessions = sessionDao.deleteSessionByNetworkId(networkId)
        Log.d("Repository", "Deleted $deletedSessions session(s) for session $networkId")

        if (deletedSessions == 0) {
            Log.w(
                "Repository",
                "Session with ID $networkId was not found in the database during deletion."
            )
            // Or throw an exception if this case should be treated as an error
        }
    }

}



