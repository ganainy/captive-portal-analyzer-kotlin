package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service

interface ConnectionsListener {
    fun connectionsChanges(num_connetions: Int)
    fun connectionsAdded(start: Int, conns: Array<ConnectionDescriptor?>?)
    fun connectionsRemoved(start: Int, conns: Array<ConnectionDescriptor?>?)
    fun connectionsUpdated(positions: IntArray?)
}
