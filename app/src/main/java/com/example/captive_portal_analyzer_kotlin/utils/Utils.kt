package com.example.captive_portal_analyzer_kotlin.utils

import android.annotation.SuppressLint
import android.net.DhcpInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import android.net.wifi.WifiConfiguration


class Utils {

    companion object {

        fun convertBSSIDToFileName(bssid: String): String {
            return bssid.replace(":", "_") // Replacing colons with underscores
        }

        fun convertFileNameToBSSID(fileName: String): String {
            return fileName.replace("_", ":") // Reverting underscores back to colons
        }


        // Static-like method to hash a password
        @JvmStatic
        fun hashPassword(password: String): String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(password.toByteArray())
                val hexString = StringBuilder()
                for (b in hash) {
                    val hex = Integer.toHexString(0xff and b.toInt())
                    if (hex.length == 1) hexString.append('0')
                    hexString.append(hex)
                }
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Error hashing password", e)
            }
        }

        // Helper function to format IP address
         fun formatIpAddress(ip: Int?): String? {
            return if (ip == null || ip == 0) null
            else InetAddress.getByAddress(
                byteArrayOf(
                    (ip and 0xFF).toByte(),
                    (ip shr 8 and 0xFF).toByte(),
                    (ip shr 16 and 0xFF).toByte(),
                    (ip shr 24 and 0xFF).toByte()
                )
            ).hostAddress
        }

        // Helper function to get DNS servers
         fun getDnsServers(dhcpInfo: DhcpInfo?): List<String>? {
            if (dhcpInfo == null) return null
            return listOfNotNull(
                formatIpAddress(dhcpInfo.dns1),
                formatIpAddress(dhcpInfo.dns2)
            )
        }

        // Helper function to calculate the Wi-Fi channel from frequency
         fun calculateChannel(frequency: Int?): Int? {
            if (frequency != null) {
                return when (frequency) {
                    in 2412..2484 -> (frequency - 2407) / 5 // 2.4 GHz
                    in 5160..5865 -> (frequency - 5000) / 5 // 5 GHz
                    else -> null
                }
            }
            return null
        }

        // Helper function to detect the security type of the connected Wi-Fi network
         fun getSecurityType(wifiManager: WifiManager, wifiInfo: WifiInfo): String? {
            // Fallback: Look for clues in SSID or BSSID (but limited in accuracy for modern Android)
            val ssid = wifiInfo.ssid ?: return "Unknown"

            return when {
                ssid.contains("WPA3", ignoreCase = true) -> "WPA3"
                ssid.contains("WPA2", ignoreCase = true) -> "WPA2"
                ssid.contains("WPA", ignoreCase = true) -> "WPA"
                ssid.contains("WEP", ignoreCase = true) -> "WEP"
                else -> "Open"
            }
        }
        //todo replace with the other networksecure check method
        // Helper function to determine if the network is secure
         fun isNetworkSecure(securityType: String?): Boolean {
            return securityType != null && securityType != "Open" && securityType != "WEP"
        }

    }
}