package com.example.captive_portal_analyzer_kotlin.room

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
    suspend fun insert(customWebViewRequest: CustomWebViewRequest)

    @Update
    suspend fun update(customWebViewRequest: CustomWebViewRequest)

    @Delete
    suspend fun delete(customWebViewRequest: CustomWebViewRequest)


    @Query("SELECT * from custom_webview_request WHERE id = :id")
    fun getCustomWebViewRequest(id: Int): Flow<CustomWebViewRequest>

    @Query("SELECT * from custom_webview_request  WHERE bssid = :bssid ORDER BY id ASC")
    fun getAllDomainCustomWebViewRequest(bssid: String): Flow<List<CustomWebViewRequest>>

    @Query("SELECT * from custom_webview_request ORDER BY id ASC")
    fun getAllCustomWebViewRequest(): Flow<List<CustomWebViewRequest>>
}