package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkItem
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun LandingScreen(
    dataRepository: IDataRepository,
    navigateToAnalysis: () -> Unit,
    navigateToManualConnect: () -> Unit
) {
    val viewModel: LandingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val wifiNetworks by viewModel.openWifiNetworks.collectAsState()

    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanOpenWifiNetworks()
        } else {
            showPermissionDialog = true
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (showPermissionDialog) {
        PermissionDeniedDialog(
            onRetry = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = {
                Toast.makeText(
                    context,
                    "Location permission is required to scan Wi-Fi networks.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }



    Scaffold(
        topBar = {
            ToolbarWithMenu(
                openMenu = {
                    scope.launch {
                        drawerState.open() // Open the drawer when menu icon is clicked
                    }
                }, title = stringResource(id = R.string.landing_screen_title)
            )
        },
    ) { paddingValues ->

        when (uiState) {
            is LandingUiState.Loading -> {
                // Show loading indicator
                CustomProgressIndicator()
            }

            is LandingUiState.LoadNetworkSuccess -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Display the list of Wi-Fi networks
                    items(wifiNetworks) { networkInformation ->
                        WifiItem(networkInformation, onClick = {
                            viewModel.connectToNetwork(networkInformation.scanResult.SSID)
                        })
                    }
                }

            }

            is LandingUiState.Error -> {
                //  error while loading networks
                CustomSnackBar(message = stringResource((uiState as LandingUiState.Error).messageStringResource)) {
                }
            }
            is LandingUiState.ConnectionSuccess -> {
                // Successfully connected to the network, navigate to analysis screen
                navigateToAnalysis()
            }

            LandingUiState.LoadNetworkError -> {
                // app couldn't find any open networks with no password, navigate to manual connect
                //screen to prompt user to connect manually
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Text message to inform the user
                    Text(
                        text = buildString {
                            append(stringResource(R.string.we_couldn_t_find_any_networks_with_no_password_which_is_usually_the))
                            append(stringResource(R.string.case_with_captive_portals_please_connect_manually_to_network))
                        },
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Button to prompt manual connection
                    Button(onClick = { navigateToManualConnect() }) {
                        Text("Connect Manually")
                    }
                }
            }
        }

    }





    }

@Composable
fun PermissionDeniedDialog(onRetry: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Permission Required") },
        text = { Text("Location permission is required to scan for Wi-Fi networks. Please grant the permission.") },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
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


@Composable
fun RequestPermission(
    permission: String,
    onPermissionGranted: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        launcher.launch(permission)
    }
}