package com.example.captive_portal_analyzer_kotlin.room.webpage_content

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// DAO interface for database operations
@Dao
interface WebpageContentDao {
    @Query("SELECT * FROM webpage_content")
    fun getAllContentStream(): Flow<List<WebpageContentEntity>>

    @Query("SELECT * FROM webpage_content WHERE url = :url")
    fun getContentForUrlStream(url: String): Flow<WebpageContentEntity?>

    @Query("SELECT * FROM webpage_content ORDER BY timestamp DESC")
    fun getLatestContentStream(): Flow<List<WebpageContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: WebpageContentEntity)

    @Delete
    suspend fun delete(content: WebpageContentEntity)

    @Query("DELETE FROM webpage_content WHERE url = :url")
    suspend fun deleteByUrl(url: String)


    @Query("SELECT * FROM webpage_content WHERE sessionId = :sessionId")
    abstract fun getAllContentForSessionId(sessionId: String): Flow<List<WebpageContentEntity>>
}