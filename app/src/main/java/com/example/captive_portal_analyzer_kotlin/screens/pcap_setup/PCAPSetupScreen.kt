package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup

import MitmAddon
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.StatusTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * Simple screen that shows a list of common questions and answers that can help users understand
 * how the app works.
 * */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PCAPSetupScreen(sharedViewModel: SharedViewModel) {
    val pcapSetupViewModel: PCAPSetupViewModel = viewModel()
    val addonState = pcapSetupViewModel.addonState.collectAsState().value
    val certificateState = pcapSetupViewModel.certificateState.collectAsState().value

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    // This effect runs when the composable enters the composition and
    // whenever the app comes back to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh data when app comes to foreground
                pcapSetupViewModel.refreshAddonState()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Lifecycle-aware check for certificate status
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                pcapSetupViewModel.checkCertificateStatus()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Lambda function to show a toast with a message and style
    val showToast = { message: String, style: ToastStyle ->
        sharedViewModel.showToast(
            message = message, style = style,
        )
    }

    val context = LocalContext.current
    // Create VPN intent
    val prepareVpnIntent = remember {
        VpnService.prepare(context)
    }

    val needsNotificationPermission by pcapSetupViewModel.needsNotificationPermission.collectAsState()
    val needsStoragePermission by pcapSetupViewModel.needsStoragePermission.collectAsState()
    val needsVPNPermission by pcapSetupViewModel.needsVPNPermission.collectAsState()
    val completedSteps by pcapSetupViewModel.completedStepsCount.collectAsState()

    PCAPSetupScreenContent(
        addonState = addonState,
        certificateState = certificateState,
        showToast = showToast,
        handleCertificateInstallIntent = pcapSetupViewModel::handleCertificateInstallIntent,
        handleCertificateExportResult = pcapSetupViewModel::handleCertificateExportResult,
        getTargetVersion = pcapSetupViewModel::getTargetVersion,
        getGithubReleaseUrl = pcapSetupViewModel::getGithubReleaseUrl,
        createInstallCertificateIntent = pcapSetupViewModel::createInstallCertificateIntent,
        fallbackToCertExport = pcapSetupViewModel::fallbackToCertExport,
        createExportCertificateIntent = pcapSetupViewModel::createExportCertificateIntent,
        onStoragePermissionResult = pcapSetupViewModel::onStoragePermissionResult,
        onNotificationPermissionResult = pcapSetupViewModel::onNotificationPermissionResult,
        needsNotificationPermission = needsNotificationPermission,
        needsStoragePermission = needsStoragePermission,
        onvpnPermissionResult =pcapSetupViewModel::onVpnPermissionResult,
        needsVPNPermission = needsVPNPermission,
        completedSteps = completedSteps,
        prepareVpnIntent = prepareVpnIntent
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun PCAPSetupScreenContent(
    addonState: AddonState,
    certificateState: CertificateUiState,
    showToast: (String, ToastStyle) -> Unit,
    handleCertificateInstallIntent: (ActivityResult) -> Unit,
    handleCertificateExportResult: (ActivityResult) -> Unit,
    getTargetVersion: (Context) -> String,
    getGithubReleaseUrl: (String) -> String,
    createInstallCertificateIntent: () -> Intent,
    fallbackToCertExport: () -> Unit,
    createExportCertificateIntent: () -> Intent,
    onStoragePermissionResult: (Boolean) -> Unit,
    onNotificationPermissionResult: (Boolean) -> Unit,
    needsNotificationPermission: Boolean,
    needsStoragePermission: Boolean,
    onvpnPermissionResult: (Boolean) -> Unit,
    needsVPNPermission: Boolean,
    completedSteps: Int,
    prepareVpnIntent: Intent?
) {
    val context = LocalContext.current

    val certInstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleCertificateInstallIntent(result)
    }

    // Register activity result launchers
    val certExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleCertificateExportResult(result)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(1f))

        Text(
            (stringResource(R.string.please_complete_the_following_steps)),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        StoragePermissionComponent(
            onStoragePermissionResult = onStoragePermissionResult,
            showToast = showToast,
            componentOrder = 1,
            needsStoragePermission = needsStoragePermission
        )

        Spacer(modifier = Modifier.height(16.dp))

        NotificationPermissionComponent(
            onNotificationPermissionResult = onNotificationPermissionResult,
            showToast = showToast,
            needsNotificationPermission = needsNotificationPermission,
            componentOrder = 2,
        )

        
        Spacer(modifier = Modifier.height(16.dp))

        VpnPermissionComponent(
            onVpnPermissionResult = onvpnPermissionResult,
            showToast = showToast,
            needsVpnPermission = needsVPNPermission,
            componentOrder = 3,
            prepareVpnIntent = prepareVpnIntent
        )

        Spacer(modifier = Modifier.height(16.dp))

        InstallMitmAddonComponent(
            openBrowser = { url -> startBrowserIntent(context, url) },
            getTargetVersion = getTargetVersion,
            getGithubReleaseUrl = getGithubReleaseUrl,
            addonState = addonState,
            componentOrder = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        CertificateStateContent(
            certificateState = certificateState,
            onInstallClick = {
                try {
                    certInstallLauncher.launch(createInstallCertificateIntent())
                } catch (e: ActivityNotFoundException) {
                    showToast(
                        context.getString(R.string.no_intent_handler_found),
                        ToastStyle.ERROR
                    )
                    fallbackToCertExport()
                }
            },
            onExportClick = {
                try {
                    certExportLauncher.launch(createExportCertificateIntent())
                } catch (e: Exception) {
                    showToast(
                        context.getString(R.string.failed_to_open_export_dialog),
                        ToastStyle.ERROR
                    )
                }
            },
            componentOrder = 5
            )

        Spacer(modifier = Modifier.weight(1f))

        val buttonText =
            if (completedSteps == 5) stringResource(R.string.continuee) else
            "${stringResource(R.string.completed)}: $completedSteps/5"
        RoundCornerButton(
            onClick = { /* mock implementation */ },
            buttonText = buttonText,
            enabled = completedSteps == 5
        )

    }
}

@Composable
fun VpnPermissionComponent(
    onVpnPermissionResult: (Boolean) -> Unit,
    showToast: (String, ToastStyle) -> Unit,
    needsVpnPermission: Boolean,
    componentOrder: Int,
    prepareVpnIntent: Intent?,
) {
    val context = LocalContext.current
    var showVpnConfirmation by remember { mutableStateOf(false) }

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isGranted = result.resultCode == Activity.RESULT_OK
        onVpnPermissionResult(isGranted)

        if (!isGranted) {
            showToast(
                context.getString(R.string.vpn_permission_required),
                ToastStyle.ERROR
            )
        }
    }

    StatusTextWithIcon(
        text = stringResource(R.string.enable_vpn_connection),
        isSuccess = !needsVpnPermission,
        number = componentOrder
    )

    if (needsVpnPermission) {
        RoundCornerButton(
            onClick = {
                // Check if VPN permission is needed
                if (prepareVpnIntent != null) {
                    try {
                        vpnPermissionLauncher.launch(prepareVpnIntent)
                    } catch (e: ActivityNotFoundException) {
                        showToast(
                            context.getString(R.string.no_intent_handler_found),
                            ToastStyle.ERROR
                        )
                    }
                } else {
                    // VPN permission already granted
                    onVpnPermissionResult(true)
                }
            },
            buttonText = stringResource(R.string.request_vpn_access),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun StoragePermissionComponent(
    onStoragePermissionResult: (Boolean) -> Unit,
    showToast: (String, ToastStyle) -> Unit,
    componentOrder: Int,
    needsStoragePermission: Boolean,
) {
    val storageRequestText = stringResource(R.string.storage_permission_required)

    // Permission launcher for storage
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onStoragePermissionResult(isGranted)
        if (!isGranted) {
            showToast(
                storageRequestText,
                ToastStyle.ERROR
            )
        }
    }

    StatusTextWithIcon(
        text = stringResource(R.string.give_storage_permission),
        isSuccess = !needsStoragePermission,
        number = componentOrder
    )

    if (needsStoragePermission) {
        RoundCornerButton(
            onClick = {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            },
            buttonText = stringResource(R.string.allow_permission),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationPermissionComponent(
    onNotificationPermissionResult: (Boolean) -> Unit,
    showToast: (String, ToastStyle) -> Unit,
    needsNotificationPermission: Boolean,
    componentOrder: Int,
) {
    val context = LocalContext.current
    val noIntentText = stringResource(R.string.no_intent_handler_found)

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onNotificationPermissionResult(isGranted)
    }

    // Show notification permission rationale dialog
    var showNotificationRationale by remember { mutableStateOf(false) }
    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { /* Dialog cannot be dismissed by clicking outside */ },
            title = { Text(stringResource(R.string.permissions_required)) },
            text = { Text(stringResource(R.string.notifications_notice)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationRationale = false
                        try {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (e: ActivityNotFoundException) {
                            showToast(
                                noIntentText,
                                ToastStyle.ERROR
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    StatusTextWithIcon(
        text = stringResource(R.string.enable_notification),
        isSuccess = !needsNotificationPermission,
        number = componentOrder
    )

    if (needsNotificationPermission) {
        RoundCornerButton(
            onClick = {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    showNotificationRationale = true
                } else {
                    try {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } catch (e: ActivityNotFoundException) {
                        showToast(
                            noIntentText,
                            ToastStyle.ERROR
                        )
                    }
                }
            },
            buttonText = stringResource(R.string.enable_action),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun CertificateStateContent(
    certificateState: CertificateUiState,
    onInstallClick: () -> Unit,
    onExportClick: () -> Unit,
    componentOrder: Int,
) {
    when (certificateState) {
        CertificateUiState.Loading -> {
            StatusTextWithIcon(
                text = stringResource(R.string.checking_the_certificate),
                isSuccess = true,
                isLoading = true, // will override the isSuccess flag
                number = componentOrder
            )
        }

        CertificateUiState.Installed -> {
            StatusTextWithIcon(
                text = stringResource(R.string.cert_installed_correctly),
                isSuccess = true,
                number = componentOrder
            )
        }

        CertificateUiState.ReadyToInstall -> {
            StatusTextWithIcon(
                text = stringResource(R.string.install_ca_certificate),
                isSuccess = false,
                number = componentOrder
            )

            RoundCornerButton(
                onClick = onInstallClick,
                buttonText = stringResource(R.string.install_action),
                modifier = Modifier.padding(top = 16.dp)
            )

        }

        CertificateUiState.ReadyToExport -> {
            StatusTextWithIcon(
                text = stringResource(R.string.export_ca_certificate),
                isSuccess = false,
                number = componentOrder
            )

            RoundCornerButton(
                onClick = onExportClick,
                buttonText = stringResource(R.string.export_action),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        CertificateUiState.Exported -> {
            StatusTextWithIcon(
                text = stringResource(R.string.cert_exported_now_installed),
                isSuccess = false,
                number = componentOrder
            )
        }

        CertificateUiState.ConnectionError -> {
            StatusTextWithIcon(
                text = stringResource(R.string.mitm_addon_autostart_workaround),
                isSuccess = false,
                number = componentOrder,
            )
        }

        is CertificateUiState.Error -> {
            StatusTextWithIcon(
                text = stringResource(R.string.ca_cert_export_failed),
                isSuccess = false,
                number = componentOrder,
            )
        }

    }
}


data class AddonUiState(
    val label: String,
    val buttonLabel: String,
    val buttonEnabled: Boolean,
    val showButton: Boolean
)

@Composable
fun InstallMitmAddonComponent(
    addonState: AddonState,
    openBrowser: (String) -> Unit,
    getTargetVersion: (Context) -> String,
    getGithubReleaseUrl: (String) -> String,
    componentOrder: Int
) {
    val context = LocalContext.current

    // UI text resources
    val installLabel = stringResource(R.string.install_the_mitm_addon)
    val installAction = stringResource(R.string.install_action)
    val updateAvailableLabel = stringResource(R.string.mitm_addon_update_available)
    val updateAction = stringResource(R.string.update_action)
    val newVersionLabel = stringResource(R.string.mitm_addon_new_version)
    val installedLabel = stringResource(R.string.mitm_addon_installed)
    val badVersionLabel = stringResource(
        R.string.mitm_addon_bad_version,
        MitmAddon.PACKAGE_VERSION_NAME
    )

    // Determine UI state based on addon state
    val uiState = when (addonState) {
        AddonState.Installed -> AddonUiState(
            label = installedLabel,
            buttonLabel = "",
            buttonEnabled = false,
            showButton = false
        )

        AddonState.NotInstalled -> AddonUiState(
            label = installLabel,
            buttonLabel = installAction,
            buttonEnabled = true,
            showButton = true
        )

        AddonState.UpdateAvailable -> AddonUiState(
            label = updateAvailableLabel,
            buttonLabel = updateAction,
            buttonEnabled = true,
            showButton = true
        )

        AddonState.NewVersionRequired -> AddonUiState(
            label = newVersionLabel,
            buttonLabel = updateAction,
            buttonEnabled = true,
            showButton = true
        )

        AddonState.IncompatibleVersion -> AddonUiState(
            label = badVersionLabel,
            buttonLabel = installAction,
            buttonEnabled = false,
            showButton = true
        )
    }


    StatusTextWithIcon(
        text = uiState.label,
        isSuccess = addonState is AddonState.Installed,
        number = componentOrder
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.showButton) {
        RoundCornerButton(
            onClick = {
                val targetVersion = getTargetVersion(context)
                val url = getGithubReleaseUrl(targetVersion)
                openBrowser(url)
            },
            enabled = uiState.buttonEnabled,
            buttonText = uiState.buttonLabel,
        )
    }
}


fun startBrowserIntent(context: Context, url: String) {
    val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(browserIntent)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun AboutScreenPreview() {
    AppTheme {
        PCAPSetupScreenContent(
            addonState = AddonState.NotInstalled,
            certificateState = CertificateUiState.ReadyToInstall,
            showToast = { message, style -> /* mock implementation */ },
            handleCertificateInstallIntent = { _ -> /* mock implementation */ },
            handleCertificateExportResult = { _ -> /* mock implementation */ },
            getTargetVersion = { _ -> "1.0.0" },
            getGithubReleaseUrl = { _ -> "https://github.com/example/repo/releases" },
            createInstallCertificateIntent = { Intent() },
            fallbackToCertExport = { /* mock implementation */ },
            createExportCertificateIntent = { Intent() },
            onStoragePermissionResult = { _ -> /* mock implementation */ },
            onNotificationPermissionResult = { _ -> /* mock implementation */ },
            needsNotificationPermission = false,
            needsStoragePermission = false,
            onvpnPermissionResult = { _ -> /* mock implementation */ },
            needsVPNPermission = false,
            completedSteps = 4,
            prepareVpnIntent = Intent()
        )
    }
}


