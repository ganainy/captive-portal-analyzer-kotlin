package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable

class Credential(var username: String, var password: String) : Serializable
class NetworkInfo : Serializable {
    var ssid: String? = null
    var bssid: String? = null
    var gatewayIP: String? = null
    var dnsServers: String? = null
}
