package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


// DataStore instance
val Context.dataStore by preferencesDataStore("info_box_prefs")

object InfoBoxState {

    // Get the "Never see again" state for a specific preferenceKey
    fun getNeverSeeAgainState(context: Context, preferenceKey: String): Flow<Boolean> {
        val key = booleanPreferencesKey(preferenceKey)
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    // Set the "Never see again" state for a specific preferenceKey
    suspend fun setNeverSeeAgainState(context: Context, preferenceKey: String, value: Boolean) {
        val key = booleanPreferencesKey(preferenceKey)
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}


@Composable
fun InfoBox(
    title: String,
    message: String,
    preferenceKey: String, // Unique key to track this info box's state
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State from DataStore for the specific preferenceKey
    val neverSeeAgainState by InfoBoxState.getNeverSeeAgainState(context, preferenceKey)
        .collectAsState(initial = false)

    var neverSeeAgain by remember { mutableStateOf(neverSeeAgainState) }
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = neverSeeAgain,
                        onCheckedChange = { checked ->
                            neverSeeAgain = checked
                        }
                    )
                    Text(text = "Never see again")
                }
                Button(onClick = {
                    if (neverSeeAgain) {
                        scope.launch {
                            InfoBoxState.setNeverSeeAgainState(context, preferenceKey, neverSeeAgain)
                        }
                    }
                    isVisible = false // Dismiss the InfoBox
                    onDismiss()
                }) {
                    Text(text = "Dismiss")
                }
            }
        }
    }
}
