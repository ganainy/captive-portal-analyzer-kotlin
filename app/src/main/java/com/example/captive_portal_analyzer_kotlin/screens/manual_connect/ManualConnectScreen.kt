package com.example.captive_portal_analyzer_kotlin.screens.manual_connect

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.BuildConfig
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.StatusTextWithIcon
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme

/**
 * The composable function for the manual connect screen.
 *
 * The page is responsible for requesting permission needed for app functionality and navigating
 * to analysis screen when permissions are granted and all requirements are fulfilled
 * (e.g. device data is turned off, wifi is on and connected to a network).
 *
 * @param navigateToAnalysis A function that navigates to the analysis screen.
 */
@Composable
fun ManualConnectScreen(
    navigateToAnalysis: () -> Unit,
) {

    LaunchedEffect(Unit) {
        //ignore this screen in debug mode
        if (BuildConfig.IS_APP_IN_DEBUG_MODE){
            navigateToAnalysis()
        }
    }

    // Get the context for launching the permission request
    val context = LocalContext.current

    // Get the view model for this screen
    val manualConnectViewModel: ManualConnectViewModel = viewModel()

    // Get the lifecycle owner for observing the lifecycle events
    val lifecycleOwner = LocalLifecycleOwner.current

    // Get the UI state and permission state from the view model
    val uiState by manualConnectViewModel.uiState.collectAsState()
    val permissionState by manualConnectViewModel.permissionState.collectAsState()

    // Request multiple permissions using the ActivityResultContracts.RequestMultiplePermissions
    // and store the result in the permissionLauncher. This is used to request permissions
    // when the user clicks on the "Grant permissions" button.
    //
    // The permissions that are requested are defined in the ManualConnectViewModel and
    // are the permissions that are required for the app to function.
    //
    // When the user grants or denies the permissions, the result is passed to the
    // onPermissionResult function in the view model, which updates the uiState and
    // permissionState accordingly.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        manualConnectViewModel.onPermissionResult(permissions)
    }

    Scaffold(
        topBar = {

        },
    ) { paddingValues ->

        // Check permissions when the composition is created and when the app is resumed (for example after user granted permissions from settings)
        // This is done by observing the lifecycle events of the activity and calling checkPermissions() when the app is resumed.
        // The lifecycle event observer is added when the composition is created and removed when the composition is disposed.
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
        // Depending on the uiState, show a different screen to the user
        // uiState can be:
        //  - Loading: show a loading indicator
        //  - Error: show an error message with a retry button
        //  - AskPermissions: show a screen asking the user to grant permissions
        //  - PermissionsGranted: show a screen that checks if all the requirements are fulfilled
        //      and if they are, navigate to the analysis screen
        when (uiState) {
            // Show a loading indicator
            is ManualConnectUiState.Loading -> {
                LoadingIndicator()
            }

            // Show an error message with a retry button
            is ManualConnectUiState.Error -> {
                ErrorComponent(
                    // Get the error message from the uiState
                    error = stringResource((uiState as ManualConnectUiState.Error).messageStringResource),
                    // When the user clicks on the retry button, check the permission state
                    // and either open the app settings if the permission was denied or
                    // check the permissions again if the user hasn't granted/denied them yet
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

            // Show a screen asking the user to grant permissions
            ManualConnectUiState.AskPermissions -> {
                when (permissionState) {
                    // If the permission needs to be requested, show a screen asking the user to grant permissions
                    PermissionState.NeedsRequest -> {
                        PermissionRequestScreen(
                            // When the user clicks on the button, request the permissions
                            onRequestPermission = {
                                permissionLauncher.launch(ManualConnectViewModel.REQUIRED_PERMISSIONS)
                            }
                        )
                    }
                    // If the permission should show a rationale, show a screen explaining why the permission is needed
                    PermissionState.ShouldShowRationale -> {
                        PermissionRationaleScreen(
                            // When the user clicks on the button, request the permissions
                            onRequestPermission = {
                                permissionLauncher.launch(ManualConnectViewModel.REQUIRED_PERMISSIONS)
                            }
                        )
                    }
                    // If the permission is already granted or denied, check the permissions again
                    else -> {
                        manualConnectViewModel.checkPermissions()
                    }
                }
            }

            // Show a screen that checks if all the requirements are fulfilled
            // and if they are, navigate to the analysis screen
            ManualConnectUiState.PermissionsGranted -> {
                // Get the state of wifi and cellular from the view model
                val isWifiOn by manualConnectViewModel.isWifiOn.collectAsState()
                val isCellularOn by  manualConnectViewModel.isCellularOn.collectAsState()
                val isConnectedToWifiNetwork by manualConnectViewModel.isConnectedToWifiNetwork.collectAsState()
                val areAllRequirementsFulfilled by manualConnectViewModel.areAllRequirementsFulfilled.collectAsState()

                // Show the content of the screen
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

/**
 * A composable that shows a screen that explains to user why the app needs internet,wifi and
 * location permissions and ask user to grant them again.
 *
 * The screen is displayed when the user clicks on the "Enable
 * Location" button in the [ManualConnectScreen].
 *
 * @param onRequestPermission A callback function that is called when the user
 * clicks on the "Grant Permissions" button. The function should request the
 * required permissions using the [Activity.requestPermissions] method.
 */
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
        //  Title: permission request
        Text(
            text = stringResource(R.string.request_permissions),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        //  Description: why app needs those permissions
        Text(
            text = stringResource(R.string.need_location_and_internet_permissions_to_continue_2),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))


        // grant permissions button
        RoundCornerButton(
            onClick = onRequestPermission,
            buttonText = stringResource(R.string.grant_permissions),
        )
    }
}

/**
 * A composable function that displays a screen requesting the user to grant necessary permissions.
 *
 * @param onRequestPermission A callback function to invoke when the user clicks the "Grant Permissions" button.
 */
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
        // Title: Request Permissions
        Text(
            text = stringResource(R.string.request_permissions),
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Description: Explanation for the need of permissions
        Text(
            text = stringResource(R.string.need_location_and_internet_permissions_to_continue),
            style = typography.bodyMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Button to trigger permission request
        RoundCornerButton(
            onClick = onRequestPermission,
            buttonText = stringResource(R.string.grant_permissions),
        )
    }
}



/**
 * A composable that shows a screen that explains to user how to connect to a network and
 * check if the device is connected to a network and if the cellular is off.
 *
 * @param paddingValues The padding values of the composable.
 * @param isWifiOn Whether the wifi is on.
 * @param isCellularOff Whether the cellular is off.
 * @param isConnectedToWifiNetwork Whether the device is connected to a network.
 * @param navigateToAnalysis A callback function to navigate to the analysis screen.
 * @param areAllRequirementsFulfilled Whether all the requirements are fulfilled.
 */
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
        HintTextWithIcon(stringResource(R.string.hint1),rowAllignment = Alignment.Center)
        Spacer(modifier = Modifier.height(16.dp))


        RoundCornerButton(
            onClick = { navigateToAnalysis() },
            enabled =  areAllRequirementsFulfilled,
            buttonText = stringResource(R.string.continuee)
        )
    }
}

/**
 * A preview function for the ManualConnectContent composable.
 * Demonstrates the UI when all network requirements are fulfilled.
 */
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
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

/**
 * Preview function for the [GrantPermissionsScreen] composable.
 * This preview shows the screen in both light and dark modes.
 */
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
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

/**
 * A composable function that displays a screen asking the user to grant the necessary permissions
 * in order to analyze captive networks.
 *
 * @param paddingValues the padding values to be used for the screen
 * @param permissionLauncher the managed activity result launcher for requesting multiple permissions
 * @param permissions the permissions to be requested
 */
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
        // Set the text color to white if the system is in dark theme, otherwise use the on surface color
        val textColor = if (isSystemInDarkTheme()) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        // Display the text indicating that location and other permissions are required
        Text(
            text = stringResource(id = R.string.location_and_other_permissions_are_required_for_analyzing_captive_networks),
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
        // Add a spacer to separate the text and the button
        Spacer(modifier = Modifier.height(24.dp))
        // Display a round corner button for granting the permissions
        RoundCornerButton(
            onClick = { permissionLauncher.launch(permissions) },
            buttonText = stringResource(id = R.string.grant_permissions),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


/**
 * A preview composable function for the [PermissionDeniedDialog].
 * This preview demonstrates the UI in both light and dark modes.
 */
@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
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
/**
 * A composable function that displays a dialog that shows a list of permissions that are missing.
 *
 * The dialog displays the title, the list of permissions, and two buttons: retry and dismiss.
 * The retry button calls the [onRetry] lambda when clicked, and the dismiss button calls the [onDismiss] lambda when clicked.
 *
 * @param missingPermissions a list of permissions that are missing
 * @param onRetry a lambda that is called when the retry button is clicked
 * @param onDismiss a lambda that is called when the dismiss button is clicked
 * @param context the context to use to get the string resources
 */
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

/**
 * Returns a string that describes the permission to explain to user why such permission is needed.
 *
 * This function takes a context and a permission string as parameters and returns a string that
 * describes the permission. The string is obtained from the string resources and is specific to
 * the permission.
 *
 * @param context the context to use to get the string resources
 * @param permission the permission string to get the description for
 * @return a string that describes the permission
 */
fun getPermissionDescription(context: Context, permission: String): String {
    return when (permission) {
        Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.access_coarse_location_required_to_detect_nearby_wi_fi_networks)
        Manifest.permission.ACCESS_NETWORK_STATE -> context.getString(R.string.access_network_state_required_to_check_the_network_s_status)
        Manifest.permission.ACCESS_WIFI_STATE -> context.getString(R.string.access_wi_fi_state_required_to_monitor_the_wi_fi_network_s_status)
        Manifest.permission.INTERNET -> context.getString(R.string.internet_access_required_to_connect_to_the_internet)
        else -> permission // Fallback for unknown permissions
    }
}