package com.example.captive_portal_analyzer_kotlin.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.ThemeMode
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import java.util.Locale

/**
 * SettingsScreen is a composable that displays a column with two sections.
 * The first section is for selecting the language of the app and the second
 * section is for selecting the theme of the app.
 *
 * @param onLocalChanged a callback that is called when the language is changed
 * @param onThemeChange a callback that is called when the theme is changed
 * @param themeMode the current theme mode
 * @param currentLanguage the current language
 */
@Composable
fun SettingsScreen(
    onLocalChanged: (Locale) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onPacketCapturePreferenceChange: (Boolean) -> Unit,
    packetCapturePreference: Boolean, // true if packet capture is enabled
    themeMode: ThemeMode,
    currentLanguage: String
) {
    var selectedLanguage by remember { mutableStateOf(Locale.getDefault()) }
    val supportedLanguages = remember {
        listOf(
            Locale("en") to "English",
            Locale("de") to "Deutsch",
        )
    }


Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    // Language Section
    Text(
        text = stringResource(id = R.string.language_settings),
        style = MaterialTheme.typography.titleLarge
    )

    // Language Dropdown
    LanguageDropdown(
        currentLanguage = currentLanguage,
        onLocalChanged = onLocalChanged
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Theme Section
    Text(
        text = stringResource(id = R.string.theme_settings),
        style = MaterialTheme.typography.titleLarge
    )

    // Theme selector
    ThemeDropDown(themeMode = themeMode, onThemeModeChanged = onThemeChange)

    Spacer(modifier = Modifier.height(24.dp))

    // Packet Capture Section
    Text(
        text = stringResource(id = R.string.packet_capture_setting),
        style = MaterialTheme.typography.titleLarge
    )

    // Packet Capture Switch
    PacketCaptureSwitch(
        packetCaptureEnabled = packetCapturePreference,
        onPacketCapturePreferenceChange = onPacketCapturePreferenceChange
    )
}
}

/**
 * PacketCaptureSwitch is a composable that displays a switch for enabling/disabling packet capture.
 *
 * @param packetCaptureEnabled function returning whether packet capture is currently enabled
 * @param onPacketCapturePreferenceChange callback for when the packet capture preference changes
 */
@Composable
fun PacketCaptureSwitch(
    packetCaptureEnabled:  Boolean,
    onPacketCapturePreferenceChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.enable_packet_capture),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = packetCaptureEnabled,
            onCheckedChange = { onPacketCapturePreferenceChange(it) }
        )
    }
}


/**
 * ThemeDropDown is a composable that displays a dropdown menu for selecting the theme.
 *
 * @param themeMode the current theme mode
 * @param onThemeModeChanged a callback that is called when the theme is changed
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ThemeDropDown(themeMode: ThemeMode, onThemeModeChanged: (ThemeMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = when (themeMode) {
                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
            },
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(R.string.select_theme)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ThemeMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            }
                        )
                    },
                    onClick = {
                        onThemeModeChanged(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * LanguageDropdown is a composable that displays a dropdown menu for selecting the language.
 *
 * @param currentLanguage the current language
 * @param onLocalChanged a callback that is called when the language is changed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    currentLanguage: String,
    onLocalChanged: (Locale) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val supportedLanguages = remember {
        listOf(
            Locale("en") to "English",
            Locale("de") to "Deutsch",
        )
    }

    val selectedLanguageName = remember(currentLanguage) {
        supportedLanguages.find { it.first.language == currentLanguage }?.second ?: "English"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLanguageName,
            onValueChange = { },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            label = { Text(stringResource(id = R.string.select_language)) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedLanguages.forEach { (locale, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onLocalChanged(locale)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            onLocalChanged = { },
            onThemeChange = { },
            themeMode = ThemeMode.SYSTEM,
            currentLanguage = "en",
            onPacketCapturePreferenceChange = { },
            packetCapturePreference =  true
        )
    }
}