package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related


import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow // Return empty flow for simplicity
import kotlinx.coroutines.flow.map

// Simple mock for previews - returns empty preferences and does nothing on update
class MockDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences>
        get() = emptyFlow() // Or flowOf(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        // No-op for preview
        // You could potentially hold a simple map in memory if needed,
        // but for most previews, emptyFlow is fine.
        return androidx.datastore.preferences.core.emptyPreferences()
    }
}