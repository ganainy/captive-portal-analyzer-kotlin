package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable
import java.security.cert.Certificate


class CaptivePortalReport : Serializable {
    var portalUrl: String? = null
    var redirectChain: List<String>? = null
    var headers: Map<String, String>? = null
    var formFields: List<String>? = null
    var usesHttps: Boolean = false
    var sslCertificates: List<Certificate>? = null
    var javascriptAnalysis: String? = null
    var cookiesFound: Map<String, String>? = null
    var potentialVulnerabilities: List<String>? = null
    var networkItem: NetworkItem? = null
    var networkInformation: NetworkInformation? = null
    var tlsAnalysis: TLSAnalysis? = null
    var securityHeaders: SecurityHeadersAnalysis? = null
    var defaultCredentials: List<DefaultCredentialResult>? = null
    var bypassVulnerabilities: List<BypassVulnerability>? = null
    var infoDisclosure: InformationDisclosureAnalysis? = null
    var sessionTokens: SessionTokenAnalysis? = null
    var csrfAnalysis: CSRFAnalysis? = null

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
}