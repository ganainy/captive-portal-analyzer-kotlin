package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup

import java.io.Serializable

/* API to integrate MitmAddon */
object MitmAPI {
    const val PACKAGE_NAME: String = "com.pcapdroid.mitm"

    const val MITM_SERVICE: String = PACKAGE_NAME + ".MitmService"
    const val MSG_ERROR: Int = -1
    const val MSG_START_MITM: Int = 1
    const val MSG_GET_CA_CERTIFICATE: Int = 2
    const val MSG_STOP_MITM: Int = 3
    const val MSG_DISABLE_DOZE: Int = 4
    const val MITM_CONFIG: String = "mitm_config"
    const val CERTIFICATE_RESULT: String = "certificate"
    const val SSLKEYLOG_RESULT: String = "sslkeylog"

    class MitmConfig : Serializable {
        var proxyPort: Int = 0 // the SOCKS5 port to use to accept mitm-ed connections
        var transparentMode: Boolean =
            false // true to use transparent proxy mode, false to use SOCKS5 proxy mode
        var sslInsecure: Boolean = false // true to disable upstream certificate check
        var dumpMasterSecrets: Boolean =
            false // true to enable the TLS master secrets dump messages (similar to SSLKEYLOG)
        var shortPayload: Boolean =
            false // if true, only the initial portion of the payload will be sent
        var proxyAuth: String? = null // SOCKS5 proxy authentication, "user:pass"
        var additionalOptions: String? = null // provide additional options to mitmproxy
    }
}
