package com.example.captive_portal_analyzer_kotlin.room

import android.content.Context
import androidx.compose.runtime.remember
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [
        CustomWebViewRequest::class,
        WebpageContent::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customWebViewRequestDao(): CustomWebViewRequestDao
    abstract fun webpageContentDao(): WebpageContentDao

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