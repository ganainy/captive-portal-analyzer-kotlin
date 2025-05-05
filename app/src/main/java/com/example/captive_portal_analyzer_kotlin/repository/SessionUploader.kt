package com.example.captive_portal_analyzer_kotlin.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionData
import com.example.captive_portal_analyzer_kotlin.room.CustomWebViewRequestDao
import com.example.captive_portal_analyzer_kotlin.room.NetworkSessionDao
import com.example.captive_portal_analyzer_kotlin.room.ScreenshotDao
import com.example.captive_portal_analyzer_kotlin.room.WebpageContentDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.File

// ===  Status Definitions ===

enum class MessageType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

sealed class UploadStatus {
    data class Progress(val step: UploadStep, val message: String, val messageType: MessageType) :
        UploadStatus()

    object Complete : UploadStatus()
    data class Failed(val exception: Throwable, val message: String) : UploadStatus()
}

enum class UploadStep {
    STARTING,
    COLLECTING_DATA,
    UPLOADING_SCREENSHOTS,
    UPLOADING_WEBPAGE_CONTENT,
    UPLOADING_PCAP,
    PROCESSING_DATA,
    UPLOADING_FIRESTORE,
}

class SessionUploader(
    private val sessionDao: NetworkSessionDao,
    private val requestDao: CustomWebViewRequestDao,
    private val screenshotDao: ScreenshotDao,
    private val webpageContentDao: WebpageContentDao,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore
) {

    /**
     * Uploads all data for a session and provides status updates via a Flow.
     * Emits distinct messages for each step.
     *
     * @param sessionId The ID of the session to upload.
     * @return A Flow emitting UploadStatus updates.
     */
    fun uploadSessionData(sessionId: String): Flow<UploadStatus> = flow {
        emit(
            UploadStatus.Progress(
                UploadStep.STARTING,
                "Starting upload for session $sessionId...",
                MessageType.INFO
            )
        )

        try {
            // 1. Collect all session data
            emit(
                UploadStatus.Progress(
                    UploadStep.COLLECTING_DATA,
                    "Gathering session data from local storage...",
                    MessageType.INFO
                )
            )
            var session = sessionDao.getSession(sessionId)
                ?: run {
                    val error = Exception("Session not found: $sessionId")
                    emit(UploadStatus.Failed(error, "Session data not found locally."))
                    throw error
                }

            val requests = withTimeout(30_000) {
                requestDao.getSessionRequestsList(sessionId).first()
            }
            val screenshots = withTimeout(30_000) {
                screenshotDao.getSessionScreenshotsList(sessionId).first()
            }
            val webpageContent = withTimeout(30_000) {
                webpageContentDao.getSessionWebpageContentList(sessionId).first()
            }
            emit(
                UploadStatus.Progress(
                    UploadStep.COLLECTING_DATA,
                    "Collected ${requests.size} requests, ${screenshots.size} screenshots, ${webpageContent.size} webpage parts.",
                    MessageType.INFO
                )
            )

            // 2. Upload screenshots to Storage and get URLs
            if (screenshots.isNotEmpty()) {
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_SCREENSHOTS,
                        "Uploading ${screenshots.size} screenshots...",
                        MessageType.INFO
                    )
                )
                val uploadedScreenshots = screenshots.map { screenshot ->
                    val uploadResult = uploadScreenshot(screenshot, sessionId)
                    if (uploadResult.isFailure) {
                        Log.e(
                            "Upload",
                            "Failed to upload screenshot ${screenshot.screenshotId}: ${uploadResult.exceptionOrNull()?.localizedMessage}"
                        )
                    }
                    screenshot.copy(url = uploadResult.getOrNull())
                }
                val successfulScreenshotUploads = uploadedScreenshots.count { it.url != null }
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_SCREENSHOTS,
                        "Screenshots upload finished ($successfulScreenshotUploads/${screenshots.size} successful).",
                        MessageType.SUCCESS
                    )
                )
            } else {
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_SCREENSHOTS,
                        "No screenshots to upload.",
                        MessageType.WARNING
                    )
                )
            }

            // 3. Upload webpage content (HTML and JS files) and update URLs
            if (webpageContent.isNotEmpty()) {
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_WEBPAGE_CONTENT,
                        "Uploading ${webpageContent.size} webpage content entries...",
                        MessageType.INFO
                    )
                )
                val uploadedWebpageContent = webpageContent.map { content ->
                    val htmlUploadResult = uploadWebpageFile(
                        filePath = content.htmlContentPath,
                        sessionId = sessionId,
                        fileType = "html",
                        fileName = "${content.contentId}_html"
                    )
                    if (htmlUploadResult.isFailure) {
                        Log.e(
                            "Upload",
                            "Failed to upload HTML for ${content.contentId}: ${htmlUploadResult.exceptionOrNull()?.localizedMessage}"
                        )
                    }

                    val jsUploadResult = uploadWebpageFile(
                        filePath = content.jsContentPath,
                        sessionId = sessionId,
                        fileType = "js",
                        fileName = "${content.contentId}_js"
                    )
                    if (jsUploadResult.isFailure) {
                        Log.e(
                            "Upload",
                            "Failed to upload JS for ${content.contentId}: ${jsUploadResult.exceptionOrNull()?.localizedMessage}"
                        )
                    }

                    content.copy(
                        htmlContentPath = htmlUploadResult.getOrNull() ?: content.htmlContentPath,
                        jsContentPath = jsUploadResult.getOrNull() ?: content.jsContentPath
                    )
                }
                val successfulWebpageUploads = uploadedWebpageContent.count {
                    it.htmlContentPath.startsWith("http") || it.jsContentPath.startsWith("http")
                }
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_WEBPAGE_CONTENT,
                        "Webpage content upload finished ($successfulWebpageUploads/${webpageContent.size} successful).",
                        MessageType.SUCCESS
                    )
                )
            } else {
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_WEBPAGE_CONTENT,
                        "No webpage content to upload.",
                        MessageType.WARNING
                    )
                )
            }

            // 4. Upload the .pcap file
            emit(
                UploadStatus.Progress(
                    UploadStep.UPLOADING_PCAP,
                    "Checking for network activity recording (PCAP)...",
                    MessageType.INFO
                )
            )
            if (session.pcapFilePath != null && session.pcapFilePath.isNotEmpty()) {
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_PCAP, "Uploading PCAP file...",
                        MessageType.INFO
                    )
                )
                val result = uploadPcapFile(session.pcapFilePath, sessionId)
                if (result.isFailure) {
                    Log.e(
                        "Upload",
                        "Failed to upload PCAP file for session $sessionId",
                        result.exceptionOrNull()
                    )
                    emit(
                        UploadStatus.Progress(
                            UploadStep.UPLOADING_PCAP,
                            "PCAP upload failed: ${result.exceptionOrNull()?.localizedMessage}. Continuing with other data.",
                            MessageType.ERROR
                        )
                    )
                } else {
                    val downloadUrl = result.getOrNull()
                    if (downloadUrl != null) {
                        try {
                            val updatedSessionWithPcapUrl = session.copy(pcapFileUrl = downloadUrl)
                            sessionDao.update(updatedSessionWithPcapUrl)
                            session = updatedSessionWithPcapUrl
                            Log.d(
                                "Upload",
                                "PCAP file uploaded successfully for session $sessionId: $downloadUrl"
                            )
                            emit(
                                UploadStatus.Progress(
                                    UploadStep.UPLOADING_PCAP,
                                    "PCAP file uploaded successfully.",
                                    MessageType.SUCCESS
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "Upload",
                                "Failed to update local session for $sessionId with PCAP URL: $downloadUrl",
                                e
                            )
                            emit(
                                UploadStatus.Progress(
                                    UploadStep.UPLOADING_PCAP,
                                    "Failed to update local DB with PCAP URL: ${e.localizedMessage}. PCAP was uploaded but may not show in app state.",
                                    MessageType.ERROR
                                )
                            )
                        }
                    } else {
                        Log.w(
                            "Upload",
                            "PCAP file upload succeeded for session $sessionId but returned null URL."
                        )
                        emit(
                            UploadStatus.Progress(
                                UploadStep.UPLOADING_PCAP,
                                "PCAP upload succeeded but returned null URL.",
                                MessageType.WARNING
                            )
                        )
                    }
                }
            } else {
                Log.w(
                    "Upload",
                    "No PCAP file path found for session $sessionId, skipping PCAP upload."
                )
                emit(
                    UploadStatus.Progress(
                        UploadStep.UPLOADING_PCAP,
                        "No PCAP file found to upload.",
                        MessageType.WARNING
                    )
                )
            }


            // 5. Create report - uses the potentially updated 'session' variable
            emit(
                UploadStatus.Progress(
                    UploadStep.PROCESSING_DATA,
                    "Preparing session summary for server...",
                    MessageType.INFO
                )
            )
            // Re-collect uploaded counts based on the successful uploads
            val successfulScreenshotUploads = screenshots.count { it.url != null }
            val successfulWebpageUploads = webpageContent.count {
                it.htmlContentPath.startsWith("http") || it.jsContentPath.startsWith("http")
            }

            val sessionData = SessionData(
                session = session,
                requests = requests,
                screenshots = screenshots,
                webpageContent = webpageContent,
                requestsCount = requests.size,
                screenshotsCount = successfulScreenshotUploads,
                webpageContentCount = successfulWebpageUploads,
            )
            emit(
                UploadStatus.Progress(
                    UploadStep.PROCESSING_DATA, "Session summary prepared.",
                    MessageType.SUCCESS
                )
            )


            // 6. Upload session data to Firestore
            emit(
                UploadStatus.Progress(
                    UploadStep.UPLOADING_FIRESTORE,
                    "Sending session summary to server...",
                    MessageType.INFO
                )
            )
            val sessionRef = firestore.collection("sessions").document(sessionId)

            withTimeout(60_000) {
                sessionRef.set(sessionData, SetOptions.merge()).await()
                Log.d("Upload", "Session data uploaded successfully to Firestore for $sessionId")
            }
            emit(
                UploadStatus.Progress(
                    UploadStep.UPLOADING_FIRESTORE,
                    "Session summary sent successfully.",
                    MessageType.SUCCESS
                )
            )


            // 7. Final state
            emit(UploadStatus.Complete)

        } catch (e: Exception) {
            Log.e("Upload", "Failed to upload session data for session $sessionId", e)
            emit(
                UploadStatus.Failed(
                    e,
                    "Upload failed during step: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                )
            )
            // No re-throw
        }
    }


    /**
     * Uploads a single webpage content file (HTML or JS) to Firebase Storage and returns the download URL.
     * Stays as a suspend fun returning Result as it's a helper internal task.
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
                // Log this specific failure
                Log.w("Upload", "Webpage file not found: $filePath")
                return Result.failure(Exception("File not found: $filePath"))
            }

            val storageRef = storage.reference
                .child("sessions/$sessionId/webpage_content/$fileName.$fileType")

            withTimeout(30_000) {
                storageRef.putFile(Uri.fromFile(file)).await()
            }

            val downloadUrl = withTimeout(30_000) {
                storageRef.downloadUrl.await().toString()
            }

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("Upload", "Failed to upload webpage file $filePath", e)
            Result.failure(e)
        }
    }


    /**
     * Uploads a single screenshot to the Storage and returns the download URL.
     * Stays as a suspend fun returning Result as it's a helper internal task.
     */
    private suspend fun uploadScreenshot(
        screenshot: ScreenshotEntity,
        sessionId: String
    ): Result<String> {
        return try {
            val imageFile = File(screenshot.path)
            if (!imageFile.exists()) {
                // Log this specific failure
                Log.w("Upload", "Screenshot file not found: ${screenshot.path}")
                return Result.failure(Exception("Screenshot file not found: ${screenshot.path}"))
            }
            val imageUri = Uri.fromFile(imageFile)
            val storageRef = storage.reference
                .child("images/$sessionId/${screenshot.screenshotId}.jpg")

            withTimeout(60_000) {
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await()
                downloadUrl.toString()
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(
                "Upload",
                "Failed to upload screenshot ${screenshot.screenshotId} from path ${screenshot.path}",
                e
            )
            Result.failure(e)
        }
    }

    /**
     * Uploads a single .pcap file to Firebase Storage and returns the download URL.
     * Stays as a suspend fun returning Result as it's a helper internal task.
     */
    private suspend fun uploadPcapFile(
        pcapFilePath: String?, // URI of the .pcap file
        sessionId: String // Session ID to organize storage
    ): Result<String> {
        return try {
            if (pcapFilePath.isNullOrEmpty()) {
                Log.w("Upload", "PCAP file path is null or empty.")
                return Result.failure(IllegalArgumentException("PCAP file path is null or empty"))
            }

            val pcapUri = pcapFilePath.toUri()
            // Assuming the URI path directly points to a file in your app's accessible storage
            val pcapFile = File(pcapUri.path ?: throw IllegalArgumentException("Invalid URI path"))

            if (!pcapFile.exists()) {
                Log.w("Upload", "PCAP file not found at path: ${pcapUri.path}")
                return Result.failure(Exception("PCAP file not found at path: ${pcapUri.path}"))
            }

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
            Log.e("Upload", "Failed to upload PCAP file from path $pcapFilePath", e)
            Result.failure(e) // Return failure with the exception
        }
    }
}