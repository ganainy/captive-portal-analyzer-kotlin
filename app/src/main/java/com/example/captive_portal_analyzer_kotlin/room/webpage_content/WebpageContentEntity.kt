package com.example.captive_portal_analyzer_kotlin.room.webpage_content

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity class for storing webpage content
@Entity(tableName = "webpage_content")
data class WebpageContentEntity(
    @PrimaryKey(autoGenerate = true)
    val webpageContentId: Long = 0,
    val sessionId: String? =null,
    val url: String? =null,
    val html: String? =null,
    val javascript: String? =null,
    val timestamp: Long = System.currentTimeMillis()
)