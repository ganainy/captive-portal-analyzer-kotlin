package com.example.captive_portal_analyzer_kotlin.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Extension functions for [Context] to easily access the DataStore instances used in the app.
 *
 * There are three instances of DataStore used in the app:
 * 1. [preferencesDataStore] - for storing app preferences (like show welcome screen on first app open, etc.)
 * 2. [alertDialogDataStore] - for storing alert dialog preferences (like "never see this again" for certain dialogs)
 * 3. [settingsDataStore] - for storing app settings preferences (language, dark mode, etc.)
 *
 * The names of the DataStore instances are specified in the [preferencesDataStore] delegate.
 */
val Context.preferencesDataStore by preferencesDataStore(name = "preferences")

val Context.alertDialogDataStore by preferencesDataStore(name = "alert_dialog_prefs")

val Context.settingsDataStore by preferencesDataStore(name = "settings")