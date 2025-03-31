
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.convertMethodEnumToString
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
            convertMethodEnumToString( request.method),
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
            var session = sessionDao.getSession(sessionId)
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

            // 3. Upload webpage content (HTML and JS files) and update URLs
            val uploadedWebpageContent = webpageContent.map { content ->
                val htmlUploadResult = uploadWebpageFile(
                    filePath = content.htmlContentPath,
                    sessionId = sessionId,
                    fileType = "html",
                    fileName = "${content.contentId}_html"
                )

                val jsUploadResult = uploadWebpageFile(
                    filePath = content.jsContentPath,
                    sessionId = sessionId,
                    fileType = "js",
                    fileName = "${content.contentId}_js"
                )

                content.copy(
                    htmlContentPath = htmlUploadResult.getOrNull() ?: content.htmlContentPath,
                    jsContentPath = jsUploadResult.getOrNull() ?: content.jsContentPath
                )
            }

            // 4. Upload the .pcap file to firebase storage and update the session object
            // We will update the 'session' variable itself if successful
            if (session.pcapFilePath != null && session.pcapFilePath.isNotEmpty()) { // Check if there's a path
                val result = uploadPcapFile(session.pcapFilePath, sessionId)
                if (result.isFailure) {
                    // If .pcap file upload fails, log the error but continue with uploading other data
                    Log.e("Upload", "Failed to upload PCAP file for session $sessionId", result.exceptionOrNull())
                } else {
                    val downloadUrl = result.getOrNull()
                    if (downloadUrl != null) {
                        try {
                            // Create the updated session copy
                            val updatedSessionWithPcapUrl = session.copy(pcapFileUrl = downloadUrl)

                            // Update the local session with the download URL
                            sessionDao.update(updatedSessionWithPcapUrl) // Update DB first

                            //  Update the session variable to reflect the new state with the pcap URL
                            session = updatedSessionWithPcapUrl

                            Log.d("Upload", "PCAP file uploaded successfully for session $sessionId: $downloadUrl")
                        } catch (e: Exception) {
                            Log.e("Upload", "Failed to update local session for $sessionId with PCAP URL: $downloadUrl", e)
                        }
                    } else {
                        Log.w("Upload", "PCAP file upload succeeded for session $sessionId but returned null URL.")
                    }
                }
            } else {
                Log.w("Upload", "No PCAP file path found for session $sessionId, skipping PCAP upload.")
            }


            // 5. Create report - uses the potentially updated 'session' variable
            val sessionData = SessionData(
                session = session,
                requests = requests,
                screenshots = uploadedScreenshots,
                webpageContent = uploadedWebpageContent
            )

            // 6. Upload session data to Firestore
            val firestore = FirebaseFirestore.getInstance()
            val sessionRef = firestore.collection("sessions").document(sessionId)

            withTimeout(60_000) {
                sessionRef.set(sessionData).await()
                Log.d("Upload", "Session data uploaded successfully to Firestore for $sessionId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Upload", "Failed to upload session data for session $sessionId", e)
            Result.failure(e)
        }
    }
    /**
     * Uploads a single webpage content file (HTML or JS) to Firebase Storage and returns the download URL.
     *
     * @param filePath The local path of the file to be uploaded.
     * @param sessionId The ID of the session to which the file belongs.
     * @param fileType The type of the file (either "html" or "js").
     * @param fileName The name of the file without extension.
     * @return A Result wrapping the download URL of the uploaded file or an Exception in case of failure.
     */
    private suspend fun uploadWebpageFile(
        filePath: String,
        sessionId: String,
        fileType: String,
        fileName: String
    ): Result<String> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("File not found: $filePath"))
            }

            val storageRef = FirebaseStorage.getInstance().reference
            val fileRef = storageRef.child("sessions/$sessionId/webpage_content/$fileName.$fileType")

            withTimeout(30_000) {
                fileRef.putFile(Uri.fromFile(file))
                    .await()
            }

            val downloadUrl = withTimeout(30_000) {
                fileRef.downloadUrl.await().toString()
            }

            Result.success(downloadUrl)
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
     * Uploads a single .pcap file to Firebase Storage and returns the download URL.
     *
     * @param pcapFilePath The URI of the .pcap file to be uploaded.
     * @param sessionId The ID of the session to which the .pcap file belongs.
     * @return A Result wrapping the download URL of the uploaded .pcap file or an Exception in case of failure.
     */
    private suspend fun uploadPcapFile(
        pcapFilePath: String?, // URI of the .pcap file
        sessionId: String // Session ID to organize storage
    ): Result<String> {
        return try {
            val pcapUri = pcapFilePath?.toUri() ?: throw IllegalArgumentException("Invalid URI path")
            // Get the file from the URI (assuming it's in app storage)
            val pcapFile = File(pcapUri.path ?: throw IllegalArgumentException("Invalid URI path"))
            val fileUri = Uri.fromFile(pcapFile)

            // Reference to Firebase Storage location
            val storageRef = storage.reference
                .child("pcap_files/$sessionId/${pcapFile.nameWithoutExtension}_${System.currentTimeMillis()}.pcap")

            // Upload the file with a 60-second timeout
            withTimeout(60_000) {
                storageRef.putFile(fileUri).await() // Upload the .pcap file
                val downloadUrl = storageRef.downloadUrl.await() // Get the download URL
                downloadUrl.toString() // Return the URL as a string
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e) // Return failure with the exception
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
            val allSessions = sessionDao.getAllSessions() // Assuming this returns Flow<List<NetworkSessionEntity>?>

            allSessions.collect { sessions ->
                val allSessionsData = sessions?.map { session ->
                    val requestsCount = requestDao.getSessionRequestsCount(session.networkId)
                    val screenshotsCount = screenshotDao.getSessionScreenshotsCount(session.networkId)
                    val webpageContentCount = webpageContentDao.getWebpageContentCountForSession(session.networkId)
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


}



