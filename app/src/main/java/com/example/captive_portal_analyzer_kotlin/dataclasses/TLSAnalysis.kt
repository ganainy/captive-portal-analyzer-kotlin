package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable
import java.util.Date

class TLSAnalysis(
    var tlsVersion: String? = null,
    var supportedCipherSuites: List<String> = emptyList(),
    var hasWeakCiphers: Boolean = false,
    var certificateValid: Boolean = false,
    var certificateIssuer: String? = null,
    var certificateExpiry: Date? = null,
    var vulnerabilities: MutableList<String> = mutableListOf()
) : Serializable