package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity class for storing webpage content
@Entity(tableName = "webpage_content")
data class WebpageContent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val html: String,
    val javascript: String,
    val timestamp: Long = System.currentTimeMillis()
)