package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Data class to represent a webview request.
 * This class is used by the webview to store the requests that are made.
 * The requests are stored in the database with a unique id.
 *
 * @param customWebViewRequestId The unique ID of the request.
 * @param sessionId The session ID to group requests made by the same session (same network).
 * @param type The type of the request.
 * @param url The URL of the request.
 * @param method The method of the request (e.g. GET, POST, etc.).
 * @param body The body of the request (e.g. the data that is sent with the request).
 * @param headers The headers of the request (e.g. the headers that are sent with the request).
 * @param hasFullInternetAccess A boolean indicating whether the request was made while having full internet access.
 * @param timestamp The time when the request was made.
 */
@Entity(tableName = "custom_webview_request")
data class CustomWebViewRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val customWebViewRequestId: Int = 0,
    val sessionId: String?,
    val type: String? = null,
    val url: String?,
    val method: RequestMethod = RequestMethod.UNKNOWN,
    val body: String? = null,
    val headers: String?, // Stored as JSON string
    val hasFullInternetAccess: Boolean = false, // Whether the request has full internet access,
    // if true it means its done after successful login otherwise false
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Converts a method string to its corresponding enum value.
 *
 * @param method the method as a string (e.g. "GET", "POST", etc.)
 * @return the enum value corresponding to the method string
 */
fun convertMethodStringToEnum(method: String): RequestMethod {
    return when (method.uppercase(Locale.getDefault())) {
        "GET" -> RequestMethod.GET
        "POST" -> RequestMethod.POST
        "PUT" -> RequestMethod.PUT
        "DELETE" -> RequestMethod.DELETE
        "PATCH" -> RequestMethod.PATCH
        "HEAD" -> RequestMethod.HEAD
        "OPTIONS" -> RequestMethod.OPTIONS
        else -> RequestMethod.UNKNOWN
    }
}

/**
 * Converts a method enum to its corresponding string value.
 *
 * @param method the enum value of the method (e.g. RequestMethod.GET, RequestMethod.POST, etc.)
 * @return the string representation of the method
 */
fun convertMethodEnumToString(method: RequestMethod): String {
    return method.name
}

enum class RequestMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, UNKNOWN
}