package com.example.captive_portal_analyzer_kotlin.screens.manual_connect

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.HintText
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme


@Composable
fun ManualConnectScreen(
    navigateToAnalysis: () -> Unit,
    navigateToNetworkList: () -> Unit,
    navigateToAbout: () -> Unit,
) {

    val viewModel: ManualConnectViewModel = viewModel()
    val areAllRequirementsFulfilled by viewModel.areAllRequirementsFulfilled.collectAsState()



    Scaffold(
        topBar = {

        },
    ) { paddingValues ->
        val isWifiOn = viewModel.isWifiOn.collectAsState().value
        val isCellularOff = !viewModel.isCellularOn.collectAsState().value
        val isConnectedToWifiNetwork =
            viewModel.isConnectedToWifiNetwork.collectAsState().value

                //device has wifi connection ask user to connect to captive portal
        ManualConnectContent(paddingValues, isWifiOn, isCellularOff, isConnectedToWifiNetwork, navigateToAnalysis, areAllRequirementsFulfilled)
            }



}

@Composable
private fun ManualConnectContent(
    paddingValues: PaddingValues,
    isWifiOn:Boolean,
    isCellularOff: Boolean,
    isConnectedToWifiNetwork: Boolean,
    navigateToAnalysis: () -> Unit,
    areAllRequirementsFulfilled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr) * 2,
                top = paddingValues.calculateTopPadding() * 2,
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr) * 2,
                bottom = paddingValues.calculateBottomPadding() * 2
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            (stringResource(R.string.please_connect_to_a_wifi_captive_disable_mobile_data_then_press_continue)),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusTextWithIcon(
            text = stringResource(R.string.wifi_is_on),

            isSuccess = isWifiOn
        )

        StatusTextWithIcon(
            text = stringResource(R.string.wifi_network_is_connected),
            isSuccess = isConnectedToWifiNetwork
        )

        StatusTextWithIcon(
            text = stringResource(R.string.cellular_is_off),
            isSuccess = isCellularOff
        )


        Spacer(modifier = Modifier.height(16.dp))
        HintText(stringResource(R.string.hint1))
        Spacer(modifier = Modifier.height(16.dp))


        RoundCornerButton(
            onClick = { navigateToAnalysis() },
            enabled = areAllRequirementsFulfilled,
            buttonText = stringResource(R.string.continuee)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManualConnectContentPreview() {
    AppTheme {
        ManualConnectContent(
            paddingValues = PaddingValues(),
            isWifiOn = true,
            isCellularOff = true,
            isConnectedToWifiNetwork = true,
            navigateToAnalysis = {},
            areAllRequirementsFulfilled = true
        )
    }
}



@Composable
fun StatusTextWithIcon(text: String, isSuccess: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = if (isSuccess) {
                MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.LineThrough
                )
            } else {
                MaterialTheme.typography.bodyLarge
            }
        )
        Spacer(modifier = Modifier.width(8.dp))  // Space between text and icon
        if (isSuccess) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Success",
                tint = Color.Green
            )
        } else {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Failure", tint = Color.Red)
        }
    }
}