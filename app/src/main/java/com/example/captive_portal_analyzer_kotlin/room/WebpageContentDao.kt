package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import kotlinx.coroutines.flow.Flow
/**
 * DAO interface for database operations related to webpage content.
 */
@Dao
interface WebpageContentDao {
    /**
     * Retrieves a Flow of all webpage content in the database regardless of session.
     */
    @Query("SELECT * FROM webpage_content")
    fun getAllContentStream(): Flow<List<WebpageContentEntity>>

    /**
     * Retrieves a Flow of a webpage content for a given URL. If not found, emits a null value.
     */
    @Query("SELECT * FROM webpage_content WHERE url = :url")
    fun getContentForUrlStream(url: String): Flow<WebpageContentEntity?>

    /**
     * Retrieves a Flow of the latest webpage content in the database, sorted by timestamp.
     */
    @Query("SELECT * FROM webpage_content ORDER BY timestamp DESC")
    fun getLatestContentStream(): Flow<List<WebpageContentEntity>>

    /**
     * Inserts a webpage content into the database, overwriting any existing content with the same URL if
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: WebpageContentEntity)

    /**
     * Deletes a webpage content from the database.
     */
    @Delete
    suspend fun delete(content: WebpageContentEntity)

    /**
     * Deletes all webpage content associated with a given URL from the database.
     */
    @Query("DELETE FROM webpage_content WHERE url = :url")
    suspend fun deleteByUrl(url: String)


    /**
     * Retrieves a Flow of all webpage content associated with a given session from the database.
     */
    @Query("SELECT * FROM webpage_content WHERE sessionId = :sessionId")
    abstract fun getSessionWebpageContentList(sessionId: String): Flow<List<WebpageContentEntity>>

    /**
     * Returns a non-zero value if there is already an entry with the same HTML content, JS content, and
     * sessionId in the database.
     */
    @Query(
        "SELECT COUNT(*) FROM webpage_content WHERE htmlContent = :htmlContent AND jsContent = :jsContent AND sessionId = :sessionId"
    )
    abstract fun isWebpageContentUnique(htmlContent: String, jsContent: String, sessionId: String): Any


    /**
     * Returns the count of webpage content associated with a given session from the database.
     */
    @Query("SELECT COUNT(*) FROM webpage_content WHERE sessionId = :sessionId")
    abstract fun getWebpageContentCountForSession(sessionId: String): Int
}