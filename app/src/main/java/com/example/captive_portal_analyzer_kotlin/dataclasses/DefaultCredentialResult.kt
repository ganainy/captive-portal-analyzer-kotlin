package com.example.captive_portal_analyzer_kotlin.dataclasses

import java.io.Serializable

class DefaultCredentialResult : Serializable {
    var portal: String? = null
    var username: String? = null
    var passwordHash: String? = null
    var accessible: Boolean = false
    var endpoint: String? = null
}