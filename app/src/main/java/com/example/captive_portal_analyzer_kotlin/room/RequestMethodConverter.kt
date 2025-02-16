package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.TypeConverter
import com.example.captive_portal_analyzer_kotlin.dataclasses.RequestMethod


/**
 * Converts [RequestMethod] to [String] and vice versa, for use with Room database.
 */
class RequestMethodConverter {
    /**
     * Converts [RequestMethod] to [String].
     *
     * @param method the [RequestMethod] to convert
     * @return the [String] representation of the [RequestMethod]
     */
    @TypeConverter
    fun fromRequestMethod(method: RequestMethod): String {
        return method.name
    }

    /**
     * Converts [String] to [RequestMethod].
     *
     * @param value the [String] representation of the [RequestMethod]
     * @return the [RequestMethod] parsed from the [String]
     */
    @TypeConverter
    fun toRequestMethod(value: String): RequestMethod {
        return try {
            RequestMethod.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RequestMethod.UNKNOWN
        }
    }
}