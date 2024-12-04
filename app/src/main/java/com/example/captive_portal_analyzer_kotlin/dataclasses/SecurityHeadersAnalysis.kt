package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable

class SecurityHeadersAnalysis : Serializable {
    var presentHeaders: Map<String, String>? = null
    var missingHeaders: List<String>? = null
    var weakHeaders: List<String>? = null
    var score: SecurityHeaderScore? = null

    class SecurityHeaderScore {
        var total: Int = 0
        var grade: String? = null // A+ to F
        var recommendations: List<String>? = null
    }
}