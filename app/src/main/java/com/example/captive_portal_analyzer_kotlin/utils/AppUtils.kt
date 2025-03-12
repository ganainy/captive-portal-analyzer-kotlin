package com.example.captive_portal_analyzer_kotlin.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utility class for common functions used by different parts of the app.*/
class AppUtils {

    companion object {

        /**
         * Formats a given timestamp into a human-readable format.
         * @param timestamp The timestamp to format. Measured in milliseconds since epoch.
         * @return A string representation of the timestamp in the format "MMM dd, yyyy HH:mm".
         */
        fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        }


    }
}