package com.example.captive_portal_analyzer_kotlin.screens.welcome

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * The first screen the user will see when opening the app.
 * This screen contains an introduction to the app, its goals and how it works.
 * It also contains a button to start the app.
 */
@Composable
fun WelcomeScreen(
    navigateToNetworkList: () -> Unit,
    navigateToPCAPSetupScreen: () -> Unit,

    ) {
    Scaffold() { paddingValues ->
        WelcomeContent(
            paddingValues = paddingValues,
            navigateToNetworkList = navigateToNetworkList,
            navigateToPCAPSetupScreen = navigateToPCAPSetupScreen,
        )
    }
}

/**
 * The content of the welcome screen.
 * This composable contains the app title, its goal and how it works.
 * It also contains a button to start the app.
 */
@Composable
private fun WelcomeContent(
    paddingValues: PaddingValues,
    navigateToNetworkList: () -> Unit,
    navigateToPCAPSetupScreen : () -> Unit,
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .background(color = colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = stringResource(R.string.welcome_to_captive_portal_analyzer),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        //App goal
        Text(
            text = stringResource(R.string.this_app_is_designed_to_assist_in_the_collection_of_data_related_to_captive_portals_with_the_aim_of_enhancing_user_security_and_ensuring_data_privacy),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        // App how to use description
        Text(
            text = stringResource(R.string.start_by_creating_a_session_and_login_to_your_network_with_a_captive_portal_we_ll_guide_you_step_by_step),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Setup PCAP button
        RoundCornerButton(
            onClick = navigateToPCAPSetupScreen,
            buttonText = stringResource(R.string.setup_packet_capture),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Start app button
        RoundCornerButton(
            onClick = navigateToNetworkList,
            buttonText = stringResource(R.string.start),
        )


    }

}

/**
 * Preview for the welcome content.
 * This preview shows the welcome content with a light background and a dark background.
 */
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun WelcomeContentPreview() {
    val context = LocalContext.current
    AppTheme {
        WelcomeContent(
            paddingValues = PaddingValues(0.dp),
            navigateToNetworkList = {},
            navigateToPCAPSetupScreen = {},
        )
    }
}