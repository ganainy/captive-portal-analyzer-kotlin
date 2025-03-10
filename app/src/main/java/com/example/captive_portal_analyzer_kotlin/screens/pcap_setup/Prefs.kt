package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup

import MitmAddon
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.collection.ArraySet
import androidx.preference.PreferenceManager

object Prefs {
    const val DUMP_NONE: String = "none"
    const val DUMP_HTTP_SERVER: String = "http_server"
    const val DUMP_UDP_EXPORTER: String = "udp_exporter"
    const val DUMP_PCAP_FILE: String = "pcap_file"
    const val DEFAULT_DUMP_MODE: String = DUMP_NONE

    const val IP_MODE_IPV4_ONLY: String = "ipv4"
    const val IP_MODE_IPV6_ONLY: String = "ipv6"
    const val IP_MODE_BOTH: String = "both"
    const val IP_MODE_DEFAULT: String = IP_MODE_IPV4_ONLY

    const val BLOCK_QUIC_MODE_NEVER: String = "never"
    const val BLOCK_QUIC_MODE_ALWAYS: String = "always"
    const val BLOCK_QUIC_MODE_TO_DECRYPT: String = "to_decrypt"
    const val BLOCK_QUIC_MODE_DEFAULT: String = BLOCK_QUIC_MODE_NEVER

    const val PAYLOAD_MODE_NONE: String = "none"
    const val PAYLOAD_MODE_MINIMAL: String = "minimal"
    const val PAYLOAD_MODE_FULL: String = "full"
    const val DEFAULT_PAYLOAD_MODE: String = PAYLOAD_MODE_MINIMAL

    // used to initialize the whitelist with some safe defaults
    const val FIREWALL_WHITELIST_INIT_VER: Int = 1

    const val PREF_COLLECTOR_IP_KEY: String = "collector_ip_address"
    const val PREF_COLLECTOR_PORT_KEY: String = "collector_port"
    const val PREF_SOCKS5_PROXY_IP_KEY: String = "socks5_proxy_ip_address"
    const val PREF_SOCKS5_PROXY_PORT_KEY: String = "socks5_proxy_port"
    const val PREF_CAPTURE_INTERFACE: String = "capture_interface"
    const val PREF_MALWARE_DETECTION: String = "malware_detection"
    const val PREF_FIREWALL: String = "firewall"
    const val PREF_TLS_DECRYPTION_KEY: String = "tls_decryption"
    const val PREF_APP_FILTER: String = "app_filter"
    const val PREF_HTTP_SERVER_PORT: String = "http_server_port"
    const val PREF_PCAP_DUMP_MODE: String = "pcap_dump_mode_v2"
    const val PREF_IP_MODE: String = "ip_mode"
    const val PREF_APP_LANGUAGE: String = "app_language"
    const val PREF_ROOT_CAPTURE: String = "root_capture"
    const val PREF_VISUALIZATION_MASK: String = "vis_mask"
    const val PREF_MALWARE_WHITELIST: String = "malware_whitelist"
    const val PREF_DUMP_EXTENSIONS: String = "dump_extensions"
    const val PREF_BLOCKLIST: String = "bl"
    const val PREF_FIREWALL_WHITELIST_MODE: String = "firewall_wl_mode"
    const val PREF_FIREWALL_WHITELIST_INIT_VER: String = "firewall_wl_init"
    const val PREF_FIREWALL_WHITELIST: String = "firewall_whitelist"
    const val PREF_DECRYPTION_LIST: String = "decryption_list"
    const val PREF_START_AT_BOOT: String = "start_at_boot"
    const val PREF_SNAPLEN: String = "snaplen"
    const val PREF_MAX_PKTS_PER_FLOW: String = "max_pkts_per_flow"
    const val PREF_MAX_DUMP_SIZE: String = "max_dump_size"
    const val PREF_SOCKS5_ENABLED_KEY: String = "socks5_enabled"
    const val PREF_SOCKS5_AUTH_ENABLED_KEY: String = "socks5_auth_enabled"
    const val PREF_SOCKS5_USERNAME_KEY: String = "socks5_username"
    const val PREF_SOCKS5_PASSWORD_KEY: String = "socks5_password"
    const val PREF_TLS_DECRYPTION_SETUP_DONE: String = "tls_decryption_setup_ok"
    const val PREF_CA_INSTALLATION_SKIPPED: String = "ca_install_skipped"
    const val PREF_FULL_PAYLOAD: String = "full_payload"
    const val PREF_BLOCK_QUIC: String = "block_quic_mode"
    const val PREF_AUTO_BLOCK_PRIVATE_DNS: String = "auto_block_private_dns"
    const val PREF_APP_VERSION: String = "appver"
    const val PREF_LOCKDOWN_VPN_NOTICE_SHOWN: String = "vpn_lockdown_notice"
    const val PREF_VPN_EXCEPTIONS: String = "vpn_exceptions"
    const val PREF_PORT_MAPPING: String = "port_mapping"
    const val PREF_PORT_MAPPING_ENABLED: String = "port_mapping_enabled"
    const val PREF_BLOCK_NEW_APPS: String = "block_new_apps"
    const val PREF_PAYLOAD_NOTICE_ACK: String = "payload_notice"
    const val PREF_REMOTE_COLLECTOR_ACK: String = "remote_collector_notice"
    const val PREF_MITMPROXY_OPTS: String = "mitmproxy_opts"
    const val PREF_DNS_SERVER_V4: String = "dns_v4"
    const val PREF_DNS_SERVER_V6: String = "dns_v6"
    const val PREF_USE_SYSTEM_DNS: String = "system_dns"
    const val PREF_PCAPNG_ENABLED: String = "pcapng_format"
    const val PREF_RESTART_ON_DISCONNECT: String = "restart_on_disconnect"
    const val PREF_IGNORED_MITM_VERSION: String = "ignored_mitm_version"

    fun getDumpMode(pref: String): DumpMode {
        return when (pref) {
            DUMP_HTTP_SERVER -> DumpMode.HTTP_SERVER
            DUMP_PCAP_FILE -> DumpMode.PCAP_FILE
            DUMP_UDP_EXPORTER -> DumpMode.UDP_EXPORTER
            else -> DumpMode.NONE
        }
    }

    fun getIPMode(pref: String): IpMode {
        return when (pref) {
            IP_MODE_IPV6_ONLY -> IpMode.IPV6_ONLY
            IP_MODE_BOTH -> IpMode.BOTH
            else -> IpMode.IPV4_ONLY
        }
    }

    fun getBlockQuicMode(pref: String): BlockQuicMode {
        return when (pref) {
            BLOCK_QUIC_MODE_ALWAYS -> BlockQuicMode.ALWAYS
            BLOCK_QUIC_MODE_TO_DECRYPT -> BlockQuicMode.TO_DECRYPT
            else -> BlockQuicMode.NEVER
        }
    }

    fun getPayloadMode(pref: String): PayloadMode {
        return when (pref) {
            PAYLOAD_MODE_MINIMAL -> PayloadMode.MINIMAL
            PAYLOAD_MODE_FULL -> PayloadMode.FULL
            else -> PayloadMode.NONE
        }
    }

    fun getAppVersion(p: SharedPreferences): Int {
        return p.getInt(PREF_APP_VERSION, 0)
    }

    fun refreshAppVersion(p: SharedPreferences) {
        TODO()
       // p.edit().putInt(PREF_APP_VERSION, BuildConfig.VERSION_CODE).apply()
    }

    fun setLockdownVpnNoticeShown(p: SharedPreferences) {
        p.edit().putBoolean(PREF_LOCKDOWN_VPN_NOTICE_SHOWN, true).apply()
    }

    fun setFirewallWhitelistInitialized(p: SharedPreferences) {
        p.edit().putInt(PREF_FIREWALL_WHITELIST_INIT_VER, FIREWALL_WHITELIST_INIT_VER).apply()
    }

    fun setPortMappingEnabled(p: SharedPreferences, enabled: Boolean) {
        p.edit().putBoolean(PREF_PORT_MAPPING_ENABLED, enabled).apply()
    }

    /* Prefs with defaults */
    fun getCollectorIp(p: SharedPreferences): String {
        return (p.getString(PREF_COLLECTOR_IP_KEY, "127.0.0.1")!!)
    }

    fun getCollectorPort(p: SharedPreferences): Int {
        return (p.getString(PREF_COLLECTOR_PORT_KEY, "1234")!!.toInt())
    }

    fun getDumpMode(p: SharedPreferences): DumpMode {
        return (getDumpMode(p.getString(PREF_PCAP_DUMP_MODE, DEFAULT_DUMP_MODE)!!))
    }

    fun getHttpServerPort(p: SharedPreferences): Int {
        return (p.getString(PREF_HTTP_SERVER_PORT, "8080")!!.toInt())
    }

    fun getTlsDecryptionEnabled(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_TLS_DECRYPTION_KEY, false))
    }

    fun getSocks5Enabled(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_SOCKS5_ENABLED_KEY, false))
    }

    fun getSocks5ProxyHost(p: SharedPreferences): String {
        return (p.getString(PREF_SOCKS5_PROXY_IP_KEY, "0.0.0.0")!!)
    }

    fun getSocks5ProxyPort(p: SharedPreferences): Int {
        return (p.getString(PREF_SOCKS5_PROXY_PORT_KEY, "8080")!!.toInt())
    }

    fun isSocks5AuthEnabled(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_SOCKS5_AUTH_ENABLED_KEY, false))
    }

    fun getSocks5Username(p: SharedPreferences): String {
        return (p.getString(PREF_SOCKS5_USERNAME_KEY, "")!!)
    }

    fun getSocks5Password(p: SharedPreferences): String {
        return (p.getString(PREF_SOCKS5_PASSWORD_KEY, "")!!)
    }

    fun getAppFilter(p: SharedPreferences): Set<String> {
        return (getStringSet(p, PREF_APP_FILTER))
    }

    fun getIPMode(p: SharedPreferences): IpMode {
        return (getIPMode(p.getString(PREF_IP_MODE, IP_MODE_DEFAULT)!!))
    }

    fun getBlockQuicMode(p: SharedPreferences): BlockQuicMode {
        return (getBlockQuicMode(p.getString(PREF_BLOCK_QUIC, BLOCK_QUIC_MODE_DEFAULT)!!))
    }

    fun useEnglishLanguage(p: SharedPreferences): Boolean {
        return ("english" == p.getString(PREF_APP_LANGUAGE, "system"))
    }

    fun isRootCaptureEnabled(p: SharedPreferences): Boolean {
        return false
    }

    fun isPcapdroidMetadataEnabled(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_DUMP_EXTENSIONS, false))
    }

    fun getCaptureInterface(p: SharedPreferences): String {
        return (p.getString(PREF_CAPTURE_INTERFACE, "@inet")!!)
    }

    fun isMalwareDetectionEnabled(ctx: Context?, p: SharedPreferences): Boolean {
        return true
    }

    fun isFirewallEnabled(ctx: Context?, p: SharedPreferences): Boolean {
        // NOTE: firewall can be disabled at runtime
        return true
    }

    fun isPcapngEnabled(ctx: Context?, p: SharedPreferences): Boolean {
        return true
    }

    fun startAtBoot(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_START_AT_BOOT, false))
    }

    fun restartOnDisconnect(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_RESTART_ON_DISCONNECT, false))
    }

    fun isTLSDecryptionSetupDone(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_TLS_DECRYPTION_SETUP_DONE, false))
    }

    fun getFullPayloadMode(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_FULL_PAYLOAD, false))
    }

    fun isPrivateDnsBlockingEnabled(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_AUTO_BLOCK_PRIVATE_DNS, true))
    }

    fun lockdownVpnNoticeShown(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_LOCKDOWN_VPN_NOTICE_SHOWN, false))
    }

    fun blockNewApps(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_BLOCK_NEW_APPS, false))
    }

    fun isFirewallWhitelistMode(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_FIREWALL_WHITELIST_MODE, false))
    }

    fun isFirewallWhitelistInitialized(p: SharedPreferences): Boolean {
        return (p.getInt(PREF_FIREWALL_WHITELIST_INIT_VER, 0) == FIREWALL_WHITELIST_INIT_VER)
    }

    fun getMitmproxyOpts(p: SharedPreferences): String {
        return (p.getString(PREF_MITMPROXY_OPTS, "")!!)
    }

    fun isPortMappingEnabled(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_PORT_MAPPING_ENABLED, true))
    }

    fun useSystemDns(p: SharedPreferences): Boolean {
        return (p.getBoolean(PREF_USE_SYSTEM_DNS, true))
    }

    fun getDnsServerV4(p: SharedPreferences): String {
        return (p.getString(PREF_DNS_SERVER_V4, "1.1.1.1")!!)
    }

    fun getDnsServerV6(p: SharedPreferences): String {
        return (p.getString(PREF_DNS_SERVER_V6, "2606:4700:4700::1111")!!)
    }

    fun isIgnoredMitmVersion(p: SharedPreferences, v: String): Boolean {
        return p.getString(PREF_IGNORED_MITM_VERSION, "") == v
    }

    // Gets a StringSet from the prefs
    // The preference should either be a StringSet or a String
    // An empty set is returned as the default value
    @SuppressLint("MutatingSharedPrefs")
    fun getStringSet(p: SharedPreferences, key: String?): Set<String> {
        var rv: MutableSet<String>? = null

        try {
            rv = p.getStringSet(key, null)
        } catch (e: ClassCastException) {
            // retry with string
            val s = p.getString(key, "")!!

            if (!s.isEmpty()) {
                rv = ArraySet()
                rv.add(s)
            }
        }

        if (rv == null) rv = ArraySet()

        return rv
    }

    fun asString(ctx: Context): String {
        val p = PreferenceManager.getDefaultSharedPreferences(ctx)

        // NOTE: possibly sensitive info like the collector IP address not shown
        return """
            DumpMode: ${getDumpMode(p)}
            FullPayload: ${getFullPayloadMode(p)}
            TLSDecryption: ${getTlsDecryptionEnabled(p)}
            TLSSetupOk: ${isTLSDecryptionSetupDone(p)}
            CAInstallSkipped: ${MitmAddon.isCAInstallationSkipped(ctx)}
            BlockQuic: ${getBlockQuicMode(p)}
            RootCapture: ${isRootCaptureEnabled(p)}
            Socks5: ${getSocks5Enabled(p)}
            BlockPrivateDns: ${isPrivateDnsBlockingEnabled(p)}
            CaptureInterface: ${getCaptureInterface(p)}
            MalwareDetection: ${isMalwareDetectionEnabled(ctx, p)}
            Firewall: ${isFirewallEnabled(ctx, p)}
            PCAPNG: ${isPcapngEnabled(ctx, p)}
            BlockNewApps: ${blockNewApps(p)}
            TargetApps: ${getAppFilter(p)}
            IpMode: ${getIPMode(p)}
            DumpExtensions: ${isPcapdroidMetadataEnabled(p)}
            StartAtBoot: ${startAtBoot(p)}
            """.trimIndent()
    }

    enum class DumpMode {
        NONE,
        HTTP_SERVER,
        PCAP_FILE,
        UDP_EXPORTER
    }

    enum class IpMode {
        IPV4_ONLY,
        IPV6_ONLY,
        BOTH,
    }

    enum class BlockQuicMode {
        NEVER,
        ALWAYS,
        TO_DECRYPT
    }

    enum class PayloadMode {
        NONE,
        MINIMAL,
        FULL
    }
}