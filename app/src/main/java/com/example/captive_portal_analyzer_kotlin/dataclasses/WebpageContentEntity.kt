package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a webpage content entity (mainly HTML and JS content of the webpage).
 *
 * @property contentId the primary key for each entity
 * @property sessionId the session id this content belongs to
 * @property url the url of the webpage the content was captured from
 * @property htmlContent the HTML content of the webpage
 * @property jsContent the JS content of the webpage
 * @property timestamp the timestamp when the content was captured
 */
@Entity(tableName = "webpage_content")
data class WebpageContentEntity(
    @PrimaryKey(autoGenerate = true) val contentId: Int = 0,
    val sessionId: String,
    val url: String,
    val htmlContent: String,
    val jsContent: String,
    val timestamp: Long,
)