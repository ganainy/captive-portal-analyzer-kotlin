package com.example.captive_portal_analyzer_kotlin.room.custom_webview_request

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomWebViewRequestDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customWebViewRequest: CustomWebViewRequestEntity)

    @Update
    suspend fun update(customWebViewRequest: CustomWebViewRequestEntity)

    @Delete
    suspend fun delete(customWebViewRequest: CustomWebViewRequestEntity)


    @Query("SELECT * from custom_webview_request WHERE customWebViewRequestId = :customWebViewRequestId")
    fun getCustomWebViewRequest(customWebViewRequestId: Int): Flow<CustomWebViewRequestEntity>

    @Query("SELECT * from custom_webview_request  WHERE sessionId = :sessionId ORDER BY customWebViewRequestId ASC")
    fun getSessionCustomWebViewRequest(sessionId: String): Flow<List<CustomWebViewRequestEntity>>

    @Query("SELECT * from custom_webview_request ORDER BY customWebViewRequestId ASC")
    fun getAllCustomWebViewRequest(): Flow<List<CustomWebViewRequestEntity>>
}