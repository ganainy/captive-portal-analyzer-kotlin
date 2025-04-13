package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Simple mock for previews - always returns CONNECTED
class MockNetworkConnectivityObserver : NetworkConnectivityObserver {
    private val statusFlow = MutableStateFlow(NetworkConnectivityObserver.Status.CONNECTED)
    override fun observe(): Flow<NetworkConnectivityObserver.Status> = statusFlow
}

// Interface definition if you don't have it explicitly (needed for the mock)
interface NetworkConnectivityObserver {
    fun observe(): Flow<Status>
    enum class Status { CONNECTED, UNAVAILABLE, LOSING, LOST }
}