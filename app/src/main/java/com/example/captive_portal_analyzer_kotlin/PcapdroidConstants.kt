package com.example.captive_portal_analyzer_kotlin


object PcapdroidConstants {
    const val PCAPDROID_PACKAGE = "com.emanuelef.remote_capture"
    const val PCAPDROID_CAPTURE_CTRL_ACTIVITY = "com.emanuelef.remote_capture.activities.CaptureCtrl"

    // Actions
    const val ACTION_START = "start"
    const val ACTION_STOP = "stop"
    const val ACTION_GET_STATUS = "get_status"

    // Intent Extras (Keys)
    const val EXTRA_ACTION = "action"
    const val EXTRA_PCAP_DUMP_MODE = "pcap_dump_mode"
    const val EXTRA_APP_FILTER = "app_filter"
    const val EXTRA_COLLECTOR_IP = "collector_ip_address"
    const val EXTRA_COLLECTOR_PORT = "collector_port"
    const val EXTRA_DUMP_EXTENSIONS = "dump_extensions" // Recommended for app info
    const val EXTRA_BROADCAST_RECEIVER = "broadcast_receiver" // Optional: For stop notifications

    // Intent Extras (Values)
    const val DUMP_MODE_UDP_EXPORTER = "udp_exporter"

    // Status Result Extras
    const val RESULT_EXTRA_RUNNING = "running"
    const val RESULT_EXTRA_VERSION_NAME = "version_name"
    const val RESULT_EXTRA_VERSION_CODE = "version_code"
    // Add other stats extras if needed (bytes_sent, etc.)

    // Broadcast Action
    const val BROADCAST_ACTION_STATUS = "com.emanuelef.remote_capture.CaptureStatus"

    // Localhost IP for UDP Exporter
    const val LOCALHOST_IP = "127.0.0.1"
    // Choose a port for your app to listen on
    const val UDP_LISTENER_PORT = 5123
}