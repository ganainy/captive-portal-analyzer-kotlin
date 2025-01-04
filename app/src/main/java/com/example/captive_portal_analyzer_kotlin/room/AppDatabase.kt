package com.example.captive_portal_analyzer_kotlin.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity


@Database(
    entities = [
        CustomWebViewRequestEntity::class,
        NetworkSessionEntity::class,
        WebpageContentEntity::class,
        ScreenshotEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customWebViewRequestDao(): CustomWebViewRequestDao
    abstract fun webpageContentDao(): WebpageContentDao
    abstract fun networkSessionDao(): NetworkSessionDao
    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

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