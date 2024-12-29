package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.CustomSnackBar
import com.example.captive_portal_analyzer_kotlin.components.MenuItem
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkItem
@Composable
fun LandingScreen(
    navigateToAnalysis: () -> Unit,
    navigateToManualConnect: () -> Unit,
    navigateToAbout: () -> Unit,
) {
    val viewModel: LandingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val wifiNetworks by viewModel.openWifiNetworks.collectAsState()
    val context = LocalContext.current

    var showPermissionDialog by remember { mutableStateOf(false) }
    var deniedPermissions by remember { mutableStateOf(emptyList<String>()) }

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.CHANGE_NETWORK_STATE
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allPermissionsGranted = result.all { it.value }
        if (!allPermissionsGranted) {
            deniedPermissions = result.filter { !it.value }.keys.toList()
            showPermissionDialog = true
        } else {
            viewModel.updateShowWelcomeScreen(false)
            viewModel.scanOpenWifiNetworks()
        }
    }

    if (showPermissionDialog) {
        PermissionDeniedDialog(
            missingPermissions = deniedPermissions,
            onRetry = {
                showPermissionDialog = false
                permissionLauncher.launch(permissions)
            },
            onDismiss = {
                Toast.makeText(
                    context,
                    context.getString(R.string.location_and_other_permissions_are_required_to_scan_wi_fi_networks),
                    Toast.LENGTH_SHORT
                ).show()
            },
            context = context
        )
    }

    Scaffold(
        topBar = {
            ToolbarWithMenu(
                title = stringResource(id = R.string.landing_screen_title),
            menuItems = listOf(
                MenuItem(
                    iconPath = R.drawable.about,
                    itemName = stringResource(id = R.string.about),
                    onClick = {
                        navigateToAbout()
                    }
                ),

                )
            )
        },
    ) { paddingValues ->
            when (uiState) {
                is LandingUiState.Loading -> {
                    CustomProgressIndicator()
                }
                is LandingUiState.LoadNetworkSuccess -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        items(wifiNetworks) { networkInformation ->
                            WifiItem(networkInformation, onClick = {
                                viewModel.connectToNetwork(networkInformation.scanResult.SSID)
                            })
                        }
                    }
                }
                is LandingUiState.Error -> {
                    CustomSnackBar(
                        message = stringResource((uiState as LandingUiState.Error).messageStringResource)
                    ) {}
                }
                is LandingUiState.ConnectionSuccess -> {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text(stringResource(id = R.string.connected_to_wifi)) },
                        text = { Text(stringResource(id = R.string.please_proceed_to_next_step)) },
                        confirmButton = {
                            TextButton(onClick = navigateToAnalysis) {
                                Text(stringResource(id = R.string.next))
                            }
                        },
                    )
                }
                LandingUiState.NoOpenNetworks -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_open_networks_found),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navigateToManualConnect() }) {
                            Text(text = stringResource(id = R.string.connect_manually))
                        }
                    }
                }

                LandingUiState.AskPermissions ->             GrantPermissionsScreen(paddingValues, permissionLauncher, permissions)

            }
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
        Text(
            text = stringResource(id = R.string.welcome_to_app),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.location_and_other_permissions_are_required_for_analyzing_captive_networks),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { permissionLauncher.launch(permissions) },
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(text = stringResource(id = R.string.grant_permissions))
        }
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
                    Text("• ${getPermissionDescription(context,permission)}")
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
        Manifest.permission.ACCESS_FINE_LOCATION -> context.getString(R.string.access_fine_location_required_to_detect_wi_fi_networks_accurately)
        Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.access_coarse_location_required_to_detect_nearby_wi_fi_networks)
        Manifest.permission.ACCESS_NETWORK_STATE -> context.getString(R.string.access_network_state_required_to_check_the_network_s_status)
        Manifest.permission.ACCESS_WIFI_STATE -> context.getString(R.string.access_wi_fi_state_required_to_monitor_the_wi_fi_network_s_status)
        Manifest.permission.CHANGE_WIFI_STATE -> context.getString(R.string.change_wi_fi_state_required_to_manage_wi_fi_connections)
        Manifest.permission.INTERNET -> context.getString(R.string.internet_access_required_to_connect_to_the_internet)
        Manifest.permission.CHANGE_NETWORK_STATE -> context.getString(R.string.change_network_state_required_to_manage_network_configurations)
        else -> permission // Fallback for unknown permissions
    }
}


@Composable
fun WifiItem(
    networkItem: NetworkItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = networkItem.scanResult.SSID,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = networkItem.securityIcon),
            contentDescription = if (!networkItem.isSecured) "Open Network" else "Secured Network",
            modifier = Modifier.size(24.dp),
            tint = if (!networkItem.isSecured) Color.Green else Color.Black
        )
    }
}

