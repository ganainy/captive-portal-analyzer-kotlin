package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.datastore.alertDialogDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch



/**
 * [AlertDialogState] handles storing and retrieving the "Never see again" state
 * of an [AlertDialog] using the [alertDialogDataStore] DataStore.
 */
object AlertDialogState {
    /**
     * Returns a Flow of Boolean values representing the "Never see again"
     * state for a specific preferenceKey.
     *
     * @param context the Android Context
     * @param preferenceKey the key to store the value in the DataStore
     * @return a Flow of Boolean values
     */
    fun getNeverSeeAgainState(context: Context, preferenceKey: String): Flow<Boolean> {
        val key = booleanPreferencesKey(preferenceKey)
        return context.alertDialogDataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    /**
     * Sets the "Never see again" state for a specific preferenceKey.
     *
     * @param context the Android Context
     * @param preferenceKey the key to store the value in the DataStore
     * @param value the value to store
     */
    suspend fun setNeverSeeAgainState(context: Context, preferenceKey: String, value: Boolean) {
        val key = booleanPreferencesKey(preferenceKey)
        context.alertDialogDataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}

/**
 * A composable function to display an [AlertDialog] with the option to
 * never show it again.
 *
 * @param title the title of the AlertDialog
 * @param message the message of the AlertDialog
 * @param preferenceKey the key to store the "Never see again" state in the DataStore
 * @param onDismiss the callback to be invoked when the AlertDialog is dismissed
 * @param modifier the modifier to be applied to the AlertDialog
 */
@Composable
fun NeverSeeAgainAlertDialog(
    title: String,
    message: String,
    preferenceKey: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State from DataStore for the specific preferenceKey
    val neverSeeAgainState by AlertDialogState.getNeverSeeAgainState(context, preferenceKey)
        .collectAsState(initial = false)

    var neverSeeAgain by remember { mutableStateOf(neverSeeAgainState) }
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // Handle back button/outside touch
                if (neverSeeAgain) {
                    scope.launch {
                        AlertDialogState.setNeverSeeAgainState(context, preferenceKey, true)
                    }
                }
                showDialog = false
                onDismiss()
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = neverSeeAgain,
                            onCheckedChange = { checked ->
                                neverSeeAgain = checked
                            }
                        )
                        Text(
                            text = stringResource(R.string.never_show_again),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (neverSeeAgain) {
                            scope.launch {
                                AlertDialogState.setNeverSeeAgainState(context, preferenceKey, true)
                            }
                        }
                        showDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            modifier = modifier
        )
    }
}