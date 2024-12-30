package com.example.captive_portal_analyzer_kotlin.room.network_session

// Repository
interface NetworkSessionRepository {
    suspend fun insertSession(session: NetworkSessionEntity)
    suspend fun updatePortalUrl(sessionId: String, portalUrl: String)
    suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean)
    suspend fun getSessionByBssid(bssid: String?): NetworkSessionEntity?
    suspend fun getAllSessions(): List<NetworkSessionEntity>?
}

class OfflineNetworkSessionRepository(private val networkSessionDao: NetworkSessionDao) :
    NetworkSessionRepository {

    override suspend fun insertSession(session: NetworkSessionEntity) {
        networkSessionDao.insertSession(session)
    }

    override suspend fun updatePortalUrl(sessionId: String, portalUrl: String) {
        networkSessionDao.updatePortalUrl(sessionId, portalUrl)
    }

    override suspend fun updateIsCaptiveLocal(sessionId: String, isLocal: Boolean) {
        networkSessionDao.updateIsCaptiveLocal(sessionId, isLocal)
    }

    override suspend fun getSessionByBssid(bssid: String?): NetworkSessionEntity? {
        return networkSessionDao.getSessionByBssid(bssid)
    }

    override suspend fun getAllSessions(): List<NetworkSessionEntity>? {
        return networkSessionDao.getAllSessions()
    }
}