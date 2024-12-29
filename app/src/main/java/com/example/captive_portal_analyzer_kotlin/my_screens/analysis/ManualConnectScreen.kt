import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.HintText
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.ManualConnectViewModel

@Composable
fun HomeScreen(
    navigateToAnalysis: () -> Unit,
    navigateToLanding: () -> Unit,
) {

    val viewModel: ManualConnectViewModel = viewModel()
    val areAllRequirementsFulfilled by viewModel.areAllRequirementsFulfilled.collectAsState()



    Scaffold(
        topBar = {
            ToolbarWithMenu(
                    title = stringResource(id = R.string.app_name),
            )
        },
    ) { paddingValues ->


                //device has wifi connection ask user to connect to captive portal
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding( start = paddingValues.calculateStartPadding(LayoutDirection.Ltr) * 2,
                            top = paddingValues.calculateTopPadding() * 2,
                            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr) * 2,
                            bottom = paddingValues.calculateBottomPadding() * 2),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        (stringResource(R.string.please_connect_to_a_wifi_captive_disable_mobile_data_then_press_continue)),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,  // Make the text bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val isWifiOn = viewModel.isWifiOn.collectAsState().value
                    StatusTextWithIcon(
                        text = stringResource(R.string.wifi_is_on),
                        isSuccess = isWifiOn
                    )
                    val isCellularOff = !viewModel.isCellularOn.collectAsState().value
                    StatusTextWithIcon(
                        text = stringResource(R.string.cellular_is_off),
                        isSuccess = isCellularOff
                    )
                    val isConnectedToWifiNetwork =
                        viewModel.isConnectedToWifiNetwork.collectAsState().value
                    StatusTextWithIcon(
                        text = stringResource(R.string.wifi_network_is_connected),
                        isSuccess = isConnectedToWifiNetwork
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HintText(stringResource(R.string.hint1))
                    Spacer(modifier = Modifier.height(16.dp))


                    Button(
                        onClick = { navigateToAnalysis() },
                        enabled = areAllRequirementsFulfilled
                    ) {
                        Text(stringResource(R.string.continuee))
                    }
                }
            }



}


@Composable
fun StatusTextWithIcon(text: String, isSuccess: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = TextStyle(fontSize = 16.sp))
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