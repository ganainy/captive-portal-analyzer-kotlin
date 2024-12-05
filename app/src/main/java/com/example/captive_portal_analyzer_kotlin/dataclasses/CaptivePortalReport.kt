package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable
import java.security.cert.Certificate


class CaptivePortalReport : Serializable {
    // One-time portal-level information
    var portalUrl: String? = null
    var usesHttps: Boolean = false

    // Network-level information (collected once)
    var networkItem: NetworkItem? = null
    var networkInformation: NetworkInformation? = null

    // Security configuration (collected once)
    var tlsAnalysis: TLSAnalysis? = null
    var sslCertificates: List<Certificate>? = null
    var securityHeaders: SecurityHeadersAnalysis? = null
    var defaultCredentials: List<DefaultCredentialResult>? = null
    var bypassVulnerabilities: List<BypassVulnerability>? = null

    // Per-page analysis collection
    var pageAnalyses: MutableList<PageAnalysis> = mutableListOf()

    // Additional portal-wide vulnerability information
    var infoDisclosure: InformationDisclosureAnalysis? = null

    // Nested class for page-specific analysis
    class PageAnalysis(
        var pageUrl: String? = null,
        var headers: Map<String, String>? = null,
        var formFields: List<String>? = null,
        var javascriptAnalysis: String? = null,
        var cookiesFound: Map<String, String>? = null,
        var potentialVulnerabilities: List<String>? = null,
        var sessionTokens: SessionTokenAnalysis? = null,
        var csrfAnalysis: CSRFAnalysis? = null,
        var timestamp: Long = System.currentTimeMillis()
    ) : Serializable

    // Method to add page analysis
    fun addPageAnalysis(pageAnalysis: PageAnalysis) {
        pageAnalyses.add(pageAnalysis)
    }
}












// Inner classes for structured reporting
//todo use or delete
class InformationDisclosureAnalysis : Serializable {
    var sensitiveDataFound: List<String>? = null
    var exposedEndpoints: List<String>? = null
    var serverInfo: Map<String, String>? = null
    var exposedFiles: List<String>? = null
}

//todo use or delete
class CSRFAnalysis : Serializable {
    var tokensPresent: Boolean = false
    var tokenValidationPresent: Boolean = false
    var vulnerableEndpoints: List<String>? = null
    var protectedEndpoints: List<String>? = null
}