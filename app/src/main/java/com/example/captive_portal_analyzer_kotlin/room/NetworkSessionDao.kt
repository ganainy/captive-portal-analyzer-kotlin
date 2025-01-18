package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface for [NetworkSessionEntity] operations.
 *
 * The DAO is used by the [NetworkSessionRepository] to interact with the Room database.
 */
@Dao
interface NetworkSessionDao {

    /**
     * Inserts a [NetworkSessionEntity] into the database.
     *
     * If the session already exists, it will be replaced.
     *
     * @param session The session to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: NetworkSessionEntity)

    /**
     * Updates a [NetworkSessionEntity] in the database.
     *
     * @param session The session to be updated.
     */
    @Update
    suspend fun update(session: NetworkSessionEntity)

    /**
     * Retrieves a [NetworkSessionEntity] from the database by its networkId.
     *
     * @param sessionId The networkId of the session to be retrieved.
     * @return The session with the given networkId or null if no such session exists.
     */
    @Query("SELECT * FROM network_sessions WHERE networkId = :sessionId")
    suspend fun getSession(sessionId: String):NetworkSessionEntity?

    /**
     * Updates the portalUrl of a [NetworkSessionEntity] in the database.
     *
     * @param sessionId The networkId of the session to be updated.
     * @param portalUrl The new portalUrl.
     */
    @Query("UPDATE network_sessions SET captivePortalUrl = :portalUrl WHERE networkId = :sessionId")
    suspend fun updatePortalUrl(sessionId: String, portalUrl: String)

    /**
     * Updates the isCaptiveLocal flag of a [NetworkSessionEntity] in the database.
     *
     * @param sessionId The networkId of the session to be updated.
     * @param isLocal The new value of the isCaptiveLocal flag.
     */
    @Query("UPDATE network_sessions SET isCaptiveLocal = :isLocal WHERE networkId = :sessionId")
    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean)

    /**
     * Retrieves a [NetworkSessionEntity] from the database by its ssid.
     *
     * @param ssid The ssid of the session to be retrieved.
     * @return The session with the given ssid or null if no such session exists.
     */
    @Query("SELECT * FROM network_sessions WHERE ssid = :ssid LIMIT 1")
    suspend fun getSessionBySsid(ssid: String?): NetworkSessionEntity?

    /**
     * Retrieves all [NetworkSessionEntity]s from the database.
     *
     * @return A list of all sessions in the database.
     */
    @Query("SELECT * FROM network_sessions")
    abstract fun getAllSessions(): List<NetworkSessionEntity>?

    /**
     * Updates the isUploadedToRemoteServer flag of a [NetworkSessionEntity] in the database.
     *
     * @param sessionId The networkId of the session to be updated.
     * @param isUploadedToRemoteServer The new value of the isUploadedToRemoteServer flag.
     */
    @Query("UPDATE network_sessions SET isUploadedToRemoteServer = :isUploadedToRemoteServer WHERE networkId = :sessionId")
    abstract fun updateIsUploadedToRemoteServer(sessionId: String, isUploadedToRemoteServer: Boolean)

    /**
     * Retrieves a [NetworkSessionEntity] from the database by its networkId.
     *
     * This method returns a Flow which emits the session with the given networkId.
     *
     * @param sessionId The networkId of the session to be retrieved.
     * @return A Flow emitting the session with the given networkId or null if no such session exists.
     */
    @Query("SELECT * FROM network_sessions WHERE networkId = :sessionId")
    abstract fun getSessionFlow(sessionId: String): Flow<NetworkSessionEntity?>

    /**
     * Retrieves a [NetworkSessionEntity] from the database by its networkId.
     *
     * @param networkId The networkId of the session to be retrieved.
     * @return The session with the given networkId or null if no such session exists.
     */
    @Query("SELECT * FROM network_sessions WHERE networkId = :networkId")
    fun getSessionByNetworkId(networkId: String):NetworkSessionEntity?
}