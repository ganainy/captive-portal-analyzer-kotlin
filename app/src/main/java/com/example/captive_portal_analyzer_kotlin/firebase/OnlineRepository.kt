package com.example.captive_portal_analyzer_kotlin.firebase

import android.net.Uri
import com.example.captive_portal_analyzer_kotlin.dataclasses.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File


class OnlineRepository  {

    // Firestore instance
    val firestore = FirebaseFirestore.getInstance()

    // Firebase Storage instance
    val storage = FirebaseStorage.getInstance()

    // Upload report to Firestore
    suspend fun uploadReport(report: Report): Result<Unit> {
        return try {
            firestore.collection("reports")
                .document(report.session.sessionId)
                .set(report)
                .await()      // Waits for the operation to complete
            Result.success(Unit)  // Return success
        } catch (e: Exception) {
            Result.failure(e)     // Return failure with the exception
        }
    }

    // Upload image to Firebase Storage
    suspend fun uploadImage(imagePath: String, sessionId: String, screenshotId: String): Result<String> {
        return try {
            val imageFile = File(imagePath)
            val imageUri = Uri.fromFile(imageFile)
            val storageRef = storage.reference.child("images/$sessionId/$screenshotId.jpg")

            // Upload the file to Firebase Storage
            val uploadTask = storageRef.putFile(imageUri).await()

            // Get the download URL after successful upload
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())  // Return the download URL as the result
        } catch (e: Exception) {
            Result.failure(e)  // Return failure with the exception
        }
    }



}


