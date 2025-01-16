package com.example.captive_portal_analyzer_kotlin.screens.manual_connect

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.HintText
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme


@Composable
fun ManualConnectScreen(
    navigateToAnalysis: () -> Unit,
    showToast: (message: String, style: ToastStyle) -> Unit
) {

    val context = LocalContext.current
    val manualConnectViewModel: ManualConnectViewModel = viewModel()

    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by manualConnectViewModel.uiState.collectAsState()
    val permissionState by manualConnectViewModel.permissionState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        manualConnectViewModel.onPermissionResult(permissions)
    }

    Scaffold(
        topBar = {

        },
    ) { paddingValues ->

        // Check permissions on initial composition and when resuming (for example after user granted permissions from settings)
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    manualConnectViewModel.checkPermissions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        when (uiState) {
            is ManualConnectUiState.Loading -> {
                LoadingIndicator()
            }

            is ManualConnectUiState.Error -> {
                ErrorComponent(
                    error = stringResource((uiState as ManualConnectUiState.Error).messageStringResource),
                    onRetryClick  = {
                        when (permissionState) {
                            is PermissionState.Denied -> {
                                // Open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                            else -> manualConnectViewModel.checkPermissions()
                        }
                    }
                )
            }

            ManualConnectUiState.AskPermissions -> {
                when (permissionState) {
                    PermissionState.NeedsRequest -> {
                        PermissionRequestScreen(
                            onRequestPermission = {
                                permissionLauncher.launch(ManualConnectViewModel.REQUIRED_PERMISSIONS)
                            }
                        )
                    }
                    PermissionState.ShouldShowRationale -> {
                        PermissionRationaleScreen(
                            onRequestPermission = {
                                permissionLauncher.launch(ManualConnectViewModel.REQUIRED_PERMISSIONS)
                            }
                        )
                    }
                    else -> {
                        manualConnectViewModel.checkPermissions()
                    }
                }
            }

            ManualConnectUiState.PermissionsGranted -> {
                val isWifiOn by manualConnectViewModel.isWifiOn.collectAsState()
                val isCellularOn by  manualConnectViewModel.isCellularOn.collectAsState()
                val isConnectedToWifiNetwork by manualConnectViewModel.isConnectedToWifiNetwork.collectAsState()
                val areAllRequirementsFulfilled by manualConnectViewModel.areAllRequirementsFulfilled.collectAsState()

                ManualConnectContent(
                    paddingValues = paddingValues,
                    isWifiOn = isWifiOn,
                    isCellularOff = !isCellularOn,
                    isConnectedToWifiNetwork = isConnectedToWifiNetwork,
                    navigateToAnalysis = navigateToAnalysis,
                    areAllRequirementsFulfilled = areAllRequirementsFulfilled
                )
            }
        }
    }


    }


@Composable
fun PermissionRationaleScreen(onRequestPermission: () -> Unit) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //  Title
        Text(
            text = stringResource(R.string.request_permissions),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        //  Description
        Text(
            text = stringResource(R.string.need_location_and_internet_permissions_to_continue_2),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))


        // Start app button
        RoundCornerButton(
            onClick = onRequestPermission,
            buttonText = stringResource(R.string.grant_permissions),
        )
    }
}


@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {

    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //  Title
        Text(
            text = stringResource(R.string.request_permissions),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        //  Description
        Text(
            text = stringResource(R.string.need_location_and_internet_permissions_to_continue),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )



        Spacer(modifier = Modifier.height(24.dp))

        // Start app button
        RoundCornerButton(
            onClick = onRequestPermission,
            buttonText = stringResource(R.string.grant_permissions),
        )
    }

}




@Composable
private fun ManualConnectContent(
    paddingValues: PaddingValues,
    isWifiOn: Boolean,
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusTextWithIcon(text: String, isSuccess: Boolean) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
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


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSuccessDialog() {
    AppTheme {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(id = R.string.connected_to_wifi)) },
            text = { Text(stringResource(id = R.string.please_proceed_to_next_step)) },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(id = R.string.next))
                }
            },
        )
    }
}


@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun GrantPermissionsScreenPreview() {
    AppTheme {
        GrantPermissionsScreen(
            paddingValues = PaddingValues(0.dp),
            permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { },
            permissions = arrayOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION"
            )
        )
    }

}

@Composable
private fun GrantPermissionsScreen(
    paddingValues: PaddingValues,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    permissions: Array<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val textColor = if (isSystemInDarkTheme()) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Text(
            text = stringResource(id = R.string.location_and_other_permissions_are_required_for_analyzing_captive_networks),
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        RoundCornerButton(
            onClick = { permissionLauncher.launch(permissions) },
            buttonText = stringResource(id = R.string.grant_permissions),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun PermissionDeniedDialogPreview() {
    AppTheme {
        PermissionDeniedDialog(
            missingPermissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
            ),
            onRetry = {},
            onDismiss = {},
            context = LocalContext.current
        )
    }
}

@Composable
fun PermissionDeniedDialog(
    missingPermissions: List<String>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    context: Context,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.permissions_required)) },
        text = {
            Column {
                Text(stringResource(R.string.the_following_permissions_are_required_for_the_app_to_function_properly))
                Spacer(modifier = Modifier.height(8.dp))
                missingPermissions.forEach { permission ->
                    Text("• ${getPermissionDescription(context, permission)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.please_grant_these_permissions_to_continue))
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

fun getPermissionDescription(context: Context, permission: String): String {
    return when (permission) {
        Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.access_coarse_location_required_to_detect_nearby_wi_fi_networks)
        Manifest.permission.ACCESS_NETWORK_STATE -> context.getString(R.string.access_network_state_required_to_check_the_network_s_status)
        Manifest.permission.ACCESS_WIFI_STATE -> context.getString(R.string.access_wi_fi_state_required_to_monitor_the_wi_fi_network_s_status)
        Manifest.permission.INTERNET -> context.getString(R.string.internet_access_required_to_connect_to_the_internet)
        else -> permission // Fallback for unknown permissions
    }
}