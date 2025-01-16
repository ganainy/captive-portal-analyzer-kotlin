package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: NetworkSessionEntity)

    @Update
    suspend fun update(session: NetworkSessionEntity)

    @Query("SELECT * FROM network_sessions WHERE networkId = :sessionId")
    suspend fun getSession(sessionId: String):NetworkSessionEntity?

    @Query("UPDATE network_sessions SET captivePortalUrl = :portalUrl WHERE networkId = :sessionId")
    suspend fun updatePortalUrl(sessionId: String, portalUrl: String)

    @Query("UPDATE network_sessions SET isCaptiveLocal = :isLocal WHERE networkId = :sessionId")
    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean)

    @Query("SELECT * FROM network_sessions WHERE ssid = :ssid LIMIT 1")
    suspend fun getSessionBySsid(ssid: String?): NetworkSessionEntity?

    @Query("SELECT * FROM network_sessions")
    abstract fun getAllSessions(): List<NetworkSessionEntity>?

    @Query("UPDATE network_sessions SET isUploadedToRemoteServer = :isUploadedToRemoteServer WHERE networkId = :sessionId")
    abstract fun updateIsUploadedToRemoteServer(sessionId: String, isUploadedToRemoteServer: Boolean)

    @Query("SELECT * FROM network_sessions WHERE networkId = :sessionId")
    abstract fun getSessionFlow(sessionId: String): Flow<NetworkSessionEntity?>

    @Query("SELECT * FROM network_sessions WHERE networkId = :networkId")
    fun getSessionByNetworkId(networkId: String):NetworkSessionEntity?
}