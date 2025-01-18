package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import kotlinx.coroutines.flow.Flow
/**
 * Data Access Object for CustomWebViewRequestEntity. This is a Room DAO.
 * It provides functions to insert, update, delete and retrieve data from the database.
 */
@Dao
interface CustomWebViewRequestDao {

    /**
     * Inserts a CustomWebViewRequestEntity in the database. If the entity already exists,
     * it is ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customWebViewRequest: CustomWebViewRequestEntity)

    /**
     * Updates a CustomWebViewRequestEntity in the database.
     */
    @Update
    suspend fun update(customWebViewRequest: CustomWebViewRequestEntity)

    /**
     * Deletes a CustomWebViewRequestEntity from the database.
     */
    @Delete
    suspend fun delete(customWebViewRequest: CustomWebViewRequestEntity)


    /**
     * Retrieves a CustomWebViewRequestEntity with the given id.
     */
    @Query("SELECT * from custom_webview_request WHERE customWebViewRequestId = :customWebViewRequestId")
    fun getCustomWebViewRequest(customWebViewRequestId: Int): Flow<CustomWebViewRequestEntity>

    /**
     * Retrieves a list of CustomWebViewRequestEntity associated with a network session
     * from the database.
     */
    @Query("SELECT * from custom_webview_request  WHERE sessionId = :sessionId ORDER BY customWebViewRequestId ASC")
    fun getSessionRequestsList(sessionId: String): Flow<List<CustomWebViewRequestEntity>>

    /**
     * Retrieves all CustomWebViewRequestEntity from the database.
     */
    @Query("SELECT * from custom_webview_request ORDER BY customWebViewRequestId ASC")
    fun getAllCustomWebViewRequest(): Flow<List<CustomWebViewRequestEntity>>

    /**
     * Checks if a request is unique based on the following criteria:
     *  - sessionId
     *  - type
     *  - url
     *  - method
     *  - body
     *  - headers
     */
    @Query(
        "SELECT COUNT(*) FROM custom_webview_request WHERE sessionId = :sessionId" +
                " AND type = :type AND url = :url" +
                " AND method = :method AND body = :body" +
                " AND headers = :headers"
    )
    suspend fun isRequestUnique(
        sessionId: String?,
        type: String?,
        url: String?,
        method: String?,
        body: String?,
        headers: String?
    ): Int
}