package com.example.captive_portal_analyzer_kotlin.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity


/**
 * A singleton class that provides access to the Room database.
 */
@Database(
    entities = [
        CustomWebViewRequestEntity::class,
        NetworkSessionEntity::class,
        WebpageContentEntity::class,
        ScreenshotEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(RequestMethodConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides a Data Access Object (DAO) for interacting with the CustomWebViewRequest
     * table in the Room database.
     */
    abstract fun customWebViewRequestDao(): CustomWebViewRequestDao

    /**
     * Provides a Data Access Object (DAO) for interacting with the WebpageContent table
     * in the Room database.
     */
    abstract fun webpageContentDao(): WebpageContentDao

    /**
     * Provides a Data Access Object (DAO) for interacting with the NetworkSession table
     * in the Room database.
     */
    abstract fun networkSessionDao(): NetworkSessionDao

    /**
     * Provides a Data Access Object (DAO) for interacting with the Screenshot table
     * in the Room database.
     */
    abstract fun screenshotDao(): ScreenshotDao

    /**
     * A companion object that provides a global instance of the AppDatabase.
     */
    companion object {
        /**
         * A volatile instance of the AppDatabase that can be accessed from any thread.
         */
        @Volatile
        private var Instance: AppDatabase? = null

        /**
         * A factory method that returns the singleton instance of the AppDatabase.
         * The method is thread-safe and provides a fallback to destructive migration
         * If the database schema changes
         *
         * (DELETE ALL CURRENT DB DATA IF VERSION NUMBER IS CHANGED).
         */
        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}