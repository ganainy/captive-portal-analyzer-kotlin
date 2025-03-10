package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup

interface MitmListener {
    // NOTE: for fragments, this may be called when their context is null
    fun onMitmGetCaCertificateResult(ca_pem: String?)
    fun onMitmServiceConnect()
    fun onMitmServiceDisconnect()
}
