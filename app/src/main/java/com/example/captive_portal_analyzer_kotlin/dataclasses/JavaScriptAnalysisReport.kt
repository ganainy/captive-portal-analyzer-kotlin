package com.example.captive_portal_analyzer_kotlin.dataclasses

/**
 * Data class to represent JavaScript analysis results
 */
data class JavaScriptAnalysisReport(
    var scriptCount: Int = 0,
    var inlineScripts: List<String> = listOf(),
    var externalScripts: List<String> = listOf(),
    var securityRisks: MutableList<String> = mutableListOf()
)