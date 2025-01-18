package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey
/**
 * Data class to represent a  webview request.
 * This class is used by the  webview to store the requests that are made.
 * The requests are stored in the database with a unique id.
 * The session id is used to group requests that are made by the same session (same network).
 * The url is the url of the request.
 * The method is the method of the request (e.g. GET, POST, etc.).
 * The body is the body of the request (e.g. the data that is sent with the request).
 * The headers is the headers of the request (e.g. the headers that are sent with the request).
 */
@Entity(tableName = "custom_webview_request")
data class CustomWebViewRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val customWebViewRequestId: Int = 0,
    val sessionId: String?,
    val type: String?= null,
    val url: String?,
    val method: String?,
    val body: String?= null,
    val headers: String? // Stored as JSON string
)