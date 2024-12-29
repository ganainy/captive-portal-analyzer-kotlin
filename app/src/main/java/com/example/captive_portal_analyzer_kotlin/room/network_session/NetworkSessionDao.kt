package com.example.captive_portal_analyzer_kotlin.room.network_session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NetworkSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: NetworkSessionEntity)

    @Query("UPDATE network_sessions SET captivePortalUrl = :portalUrl WHERE sessionId = :sessionId")
    suspend fun updatePortalUrl(sessionId: String, portalUrl: String)

    @Query("UPDATE network_sessions SET isCaptiveLocal = :isLocal WHERE sessionId = :sessionId")
    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean)

    @Query("SELECT * FROM network_sessions WHERE bssid = :bssid ORDER BY timestamp DESC LIMIT 1")
    suspend fun getSessionByBssid(bssid: String?): NetworkSessionEntity?
}
