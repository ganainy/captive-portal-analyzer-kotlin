package com.example.captive_portal_analyzer_kotlin.mock_data

import com.example.captive_portal_analyzer_kotlin.dataclasses.BypassVulnerability
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.dataclasses.DefaultCredentialResult
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkInformation
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionTokenAnalysis
import com.example.captive_portal_analyzer_kotlin.dataclasses.TLSAnalysis
import java.util.Date

fun generateMockReport(): CaptivePortalReport {
    val captivePortalReport = CaptivePortalReport()

    captivePortalReport.portalUrl = "https://example.com"
    captivePortalReport.networkInformation = NetworkInformation(
        ssid = "MyNetwork",
        bssid = "00:14:22:01:23:45",
        ipAddress = "192.168.1.1",
        gatewayAddress = "192.168.1.254",
        linkSpeed = 100,
        rssi = -50,
        macAddress = "00:14:22:01:23:45",
        frequency = 2400,
        channel = 11,
        securityType = "WPA2",
        dnsServers = listOf("8.8.8.8", "8.8.4.4"),
        isSecure = true
    )

    captivePortalReport.tlsAnalysis = TLSAnalysis(
        tlsVersion = "TLS 1.2",
        supportedCipherSuites = listOf("TLS_RSA_WITH_AES_128_CBC_SHA"),
        hasWeakCiphers = false,
        certificateValid = true,
        certificateIssuer = "Example CA",
        certificateExpiry = Date(System.currentTimeMillis() + 3600000),
        vulnerabilities = mutableListOf("SSL Vulnerability A", "SSL Vulnerability B")
    )

    captivePortalReport.defaultCredentials = listOf(
        DefaultCredentialResult(
            portal = "ExamplePortal",
            username = "admin",
            passwordHash = "5f4dcc3b5aa765d61d8327deb882cf99"
        )
        )

    captivePortalReport.pageAnalyses = mutableListOf(
        CaptivePortalReport.PageAnalysis(
            pageUrl = "https://example.com/login",
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0",
                "Content-Type" to "application/x-www-form-urlencoded"
            ),
            cookiesFound = mapOf("sessionId" to "12345"),
            potentialVulnerabilities = listOf("XSS", "CSRF"),
            sessionTokens = SessionTokenAnalysis(
                entropy = 0.95,
                predictable = false,
                secureTransmission = true,
                cookieFlags = mutableMapOf("Secure" to "true", "HttpOnly" to "true"),
                vulnerabilities = mutableListOf("Weak randomness", "Predictable token generation")
            )
        )
    )

    captivePortalReport.tlsAnalysis = TLSAnalysis(
        tlsVersion = "TLS 1.2",
        supportedCipherSuites = listOf("TLS_RSA_WITH_AES_128_CBC_SHA"),
        hasWeakCiphers = false,
        certificateValid = true,
        certificateIssuer = "Example CA",
        certificateExpiry = Date(System.currentTimeMillis() + 3600000),
        vulnerabilities = mutableListOf("SSL Vulnerability A", "SSL Vulnerability B")
    )

    captivePortalReport.bypassVulnerabilities = listOf(
        BypassVulnerability(
            type = "SQL Injection",
            description = "An attacker can bypass authentication by exploiting an SQL injection vulnerability.",
            severity = "High",
            mitigation = "Use parameterized queries.",
            exploitable = true
        ),
        BypassVulnerability(
            type = "Privilege Escalation",
            description = "An attacker can gain elevated privileges.",
            severity = "High",
            mitigation = "Apply latest security patch.",
            exploitable = true
        )
    )

    return captivePortalReport
}