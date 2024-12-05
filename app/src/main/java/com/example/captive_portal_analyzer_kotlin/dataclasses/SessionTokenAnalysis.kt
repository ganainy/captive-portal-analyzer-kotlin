package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable

class SessionTokenAnalysis(
    var entropy: Double = 0.0,
    var predictable: Boolean = false,
    var secureTransmission: Boolean = false,
    var cookieFlags: MutableMap<String, String>? = null,
    var vulnerabilities: MutableList<String>? = null
) : Serializable
