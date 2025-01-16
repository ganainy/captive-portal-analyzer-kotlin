package com.example.captive_portal_analyzer_kotlin.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore


// Extension for DataStore
// For app preferences
val Context.preferencesDataStore by preferencesDataStore(name = "preferences")

// For alert dialog preferences
val Context.alertDialogDataStore by preferencesDataStore(name = "alert_dialog_prefs")

// For app settings preferences
val Context.settingsDataStore by preferencesDataStore(name = "settings")