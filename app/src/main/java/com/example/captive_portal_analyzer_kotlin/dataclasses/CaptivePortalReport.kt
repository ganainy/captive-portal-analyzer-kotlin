package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable
import java.security.cert.Certificate


data class CaptivePortalReport(
    // One-time portal-level information
    var portalUrl: String? = null,
    var usesHttps: Boolean = false,
    var isLocalCaptivePortal: Boolean = false, //the website for the portal is on the same network as the device

    var networkInformation: NetworkInformation? = null,
) : Serializable


