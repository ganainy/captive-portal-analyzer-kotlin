package com.example.captive_portal_analyzer_kotlin.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // Bright purple for visibility
    secondary = Color(0xFF03DAC5), // Teal for secondary elements
    background = Color(0xFF121212), // Standard dark background
    surface = Color(0xFF1E1E1E), // Slightly lighter than background
    error = Color(0xFFCF6679), // Soft red for errors
    onPrimary = Color.Black, // Text on primary color
    onSecondary = Color.Black, // Text on secondary color
    onBackground = Color(0xFFE0E0E0), // Light grey for better contrast
    onSurface = Color(0xFFE0E0E0), // Matches onBackground
    onError = Color.Black // Text on error color
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Deep purple for primary
    secondary = Color(0xFF03DAC5), // Teal for secondary
    background = Color(0xFFFDFDFD), // Very light grey to reduce strain
    surface = Color(0xFFFFFFFF), // Pure white surface
    error = Color(0xFFB00020), // Standard Material error red
    onPrimary = Color.White, // Text on primary color
    onSecondary = Color.Black, // Text on secondary color
    onBackground = Color(0xFF121212), // Dark text for contrast
    onSurface = Color(0xFF121212), // Matches onBackground
    onError = Color.White // Text on error color
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}