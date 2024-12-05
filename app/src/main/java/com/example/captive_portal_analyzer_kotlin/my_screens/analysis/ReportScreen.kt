package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.CustomSnackBar
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.dataclasses.BypassVulnerability
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.dataclasses.DefaultCredentialResult
import com.example.captive_portal_analyzer_kotlin.dataclasses.TLSAnalysis
import com.example.captive_portal_analyzer_kotlin.mock_data.generateMockReport
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@Composable
fun ReportScreen(
    dataRepository: IDataRepository,
    navigateBack: () -> Unit,
) {
    val viewModel: ReportViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()


    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    //todo replace mock with real data
    //val report = dataRepository.portalReport!!
    val report = generateMockReport()


    Scaffold(
        topBar = {
            ToolbarWithMenu(
                openMenu = {
                    scope.launch {
                        drawerState.open() // Open the drawer when menu icon is clicked
                    }
                }, title = stringResource(id = R.string.report_screen_title)
            )
        },
    ) { paddingValues ->

        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            // Display each field dynamically
            item { FieldDisplay("Portal URL", report.portalUrl) }
            item {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)) {
                KeyValueDisplay("SSID", report.networkInformation?.ssid)
                KeyValueDisplay("BSSID", report.networkInformation?.bssid)
                KeyValueDisplay("IP Address", report.networkInformation?.ipAddress)
                KeyValueDisplay("Gateway Address", report.networkInformation?.gatewayAddress)
                KeyValueDisplay("Link Speed", "${report.networkInformation?.linkSpeed} Mbps")
                KeyValueDisplay("RSSI", "${report.networkInformation?.rssi} dBm")
                KeyValueDisplay("MAC Address", report.networkInformation?.macAddress)
            }
            }

            item {
                Text(
                    text = "Bypass Vulnerabilities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                BypassVulnerabilitiesDisplay(report.bypassVulnerabilities)
            }

            item {
                Text(
                    text = "Default Credentials",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DefaultCredentialsDisplay(report.defaultCredentials)
            }

            item { FieldDisplay("Uses HTTPS", report.usesHttps.toString()) }

            item { TLSAnalysisDisplay(report.tlsAnalysis!!) }
            item { PageAnalysesDisplay(report.pageAnalyses) }


        }
    }







    when (uiState) {
        is ReportUiState.Initial -> {
            // Show initial state (empty form)
        }

        is ReportUiState.Loading -> {
            // Show loading indicator
            CustomProgressIndicator()
        }

        is ReportUiState.Error -> {
            // Show error message
            CustomSnackBar(message = stringResource((uiState as LandingUiState.Error).messageStringResource)) {
            }
        }


    }

    }

@Composable
fun FieldDisplay(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(value ?: "No Data", style = MaterialTheme.typography.bodySmall)
    }
}


@Composable
fun KeyValueDisplay(key: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$key: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.3f) // Adjust weight to control alignment
        )
        Text(
            text = value ?: "N/A",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.7f)
        )
    }
}


/*@Composable
fun BypassVulnerabilitiesDisplay(vulnerabilities: List<BypassVulnerability>?) {
    if (vulnerabilities.isNullOrEmpty()) {
            Text(
        text = stringResource(R.string.no_bypass_vulnerabilities_found),
        style = MaterialTheme.typography.bodySmall
    )
        return
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        vulnerabilities.forEach { vulnerability ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    KeyValueDisplay("Type", vulnerability.type)
                    KeyValueDisplay("Description", vulnerability.description)
                    KeyValueDisplay("Severity", vulnerability.severity)
                    KeyValueDisplay("Mitigation", vulnerability.mitigation)
                    KeyValueDisplay("Exploitable", if (vulnerability.exploitable) "Yes" else "No")
                }
            }
        }
    }
}*/


@Composable
fun DefaultCredentialsDisplay(credentials: List<DefaultCredentialResult>?) {
    if (credentials.isNullOrEmpty()) {
        Text(
            text = stringResource(R.string.no_default_credentials_found),
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 4.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            credentials.forEachIndexed { index, credential ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    KeyValueDisplayWithTruncation("Portal", credential.portal)
                    KeyValueDisplay("Username", credential.username)
                    KeyValueDisplay("Password Hash", credential.passwordHash)
                    KeyValueDisplay("Accessible", if (credential.accessible) "Yes" else "No")
                    KeyValueDisplay("Endpoint", credential.endpoint)

                    if (index < credentials.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp)) // Add a divider between items
                    }
                }
            }
        }
    }
}


@Composable
fun KeyValueDisplayWithTruncation(key: String, value: String?, maxLength: Int = 20) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$key: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.3f)
        )

        if (value != null && value.length > maxLength && !isExpanded) {
            Text(
                text = "${value.take(maxLength)}...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.5f)
            )
            TextButton(
                onClick = { isExpanded = true },
                modifier = Modifier.weight(0.2f)
            ) {
                Text(
                    text = stringResource(R.string.see_more),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                text = value ?: "N/A",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.7f)
            )
        }
    }
}


@Composable
fun TLSAnalysisDisplay(tlsAnalysis: TLSAnalysis) {
    var missingValuesCount = 0

    // Helper function to count missing values and return text if available
    fun displayField(label: String, value: String?): String {
        return if (value.isNullOrEmpty()) {
            missingValuesCount++
            "$label: N/A"
        } else {
            "$label: $value"
        }
    }

    // Helper function to display lists and handle missing values
    fun displayListField(label: String, list: List<String>?): String {
        return if (list.isNullOrEmpty()) {
            missingValuesCount++
            "$label: N/A"
        } else {
            "$label: ${list.joinToString(", ")}"
        }
    }

    // Layout to display the data
    Column(modifier = Modifier.padding(16.dp)) {
        // Displaying TLS version
        Text(text = displayField("TLS Version", tlsAnalysis.tlsVersion), fontWeight = FontWeight.Bold)

        // Displaying Supported Cipher Suites
        Text(text = displayListField("Supported Cipher Suites", tlsAnalysis.supportedCipherSuites))

        // Displaying Weak Ciphers
        Text(text = "Has Weak Ciphers: ${if (tlsAnalysis.hasWeakCiphers) "Yes" else "No"}")

        // Displaying Certificate Validity
        Text(text = "Certificate Valid: ${if (tlsAnalysis.certificateValid) "Yes" else "No"}")

        // Displaying Certificate Issuer
        Text(text = displayField("Certificate Issuer", tlsAnalysis.certificateIssuer))

        // Displaying Certificate Expiry Date
        Text(text = displayField("Certificate Expiry",
            tlsAnalysis.certificateExpiry?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(it) }))

        // Displaying Vulnerabilities
        Text(text = if (tlsAnalysis.vulnerabilities.isNullOrEmpty()) {
            missingValuesCount++
            "Vulnerabilities: N/A"
        } else {
            "Vulnerabilities: ${tlsAnalysis.vulnerabilities.joinToString(", ")}"
        })

        // Showing the count of missing values
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Missing Values: $missingValuesCount", fontWeight = FontWeight.Bold, color = Color.Red)
    }
}


@Composable
fun PageAnalysesDisplay(pageAnalyses: List<CaptivePortalReport.PageAnalysis>?) {
    var missingValuesCount = 0
    var emptyAnalysesCount = 0

    // Helper function to count missing values and return text if available
    fun displayField(label: String, value: String?): String {
        return if (value.isNullOrEmpty()) {
            missingValuesCount++
            "$label: N/A"
        } else {
            "$label: $value"
        }
    }

    // Helper function to display maps (e.g., headers, cookies)
    fun displayMapField(label: String, map: Map<String, String>?): String {
        return if (map.isNullOrEmpty()) {
            missingValuesCount++
            "$label: N/A"
        } else {
            "$label: ${map.entries.joinToString(", ") { "${it.key}: ${it.value}" }}"
        }
    }

    // Helper function to display lists (e.g., vulnerabilities)
    fun displayListField(label: String, list: List<String>?): String {
        return if (list.isNullOrEmpty()) {
            missingValuesCount++
            "$label: N/A"
        } else {
            "$label: ${list.joinToString(", ")}"
        }
    }

    // Displaying a message if pageAnalyses is empty or null
    if (pageAnalyses.isNullOrEmpty()) {
        Text("No Page Analysis data available", modifier = Modifier.padding(16.dp), color = Color.Gray)
        return
    }

    // Loop through each PageAnalysis and display
    Column(modifier = Modifier.padding(16.dp)) {
        pageAnalyses.forEach { pageAnalysis ->
            // Reset missing values count for each PageAnalysis
            missingValuesCount = 0

            Text(text = "Page Analysis", fontWeight = FontWeight.Bold)

            // Displaying Page URL
            Text(text = displayField("Page URL", pageAnalysis.pageUrl), fontWeight = FontWeight.Bold)

            // Displaying Headers
            Text(text = displayMapField("Headers", pageAnalysis.headers))

            // Displaying Cookies Found
            Text(text = displayMapField("Cookies Found", pageAnalysis.cookiesFound))

            // Displaying Potential Vulnerabilities
            Text(text = displayListField("Potential Vulnerabilities", pageAnalysis.potentialVulnerabilities))

            // Displaying Session Token Analysis
            pageAnalysis.sessionTokens?.let {
                Text(text = "Session Token Analysis:")
                Text(text = "Entropy: ${it.entropy}")
                Text(text = "Predictable: ${if (it.predictable) "Yes" else "No"}")
                Text(text = "Secure Transmission: ${if (it.secureTransmission) "Yes" else "No"}")
                Text(text = "Cookie Flags: ${it.cookieFlags?.entries?.joinToString(", ") { "${it.key}: ${it.value}" } ?: "N/A"}")
                Text(text = "Vulnerabilities: ${it.vulnerabilities?.joinToString(", ") ?: "N/A"}")
            }

            // Displaying the count of missing values for this PageAnalysis
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Missing Values: $missingValuesCount", fontWeight = FontWeight.Bold, color = Color.Red)

            // Add a divider between PageAnalyses
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Displaying the total missing PageAnalysis count if necessary
        Text(text = "Total Empty Page Analyses: $emptyAnalysesCount", fontWeight = FontWeight.Bold, color = Color.Red)
    }
}


@Composable
fun BypassVulnerabilitiesDisplay(bypassVulnerabilities: List<BypassVulnerability>?) {
    var missingValuesCount = 0

    // Helper function to display field and track missing values
    fun displayField(label: String, value: String?): String {
        return if (value.isNullOrEmpty()) {
            missingValuesCount++
            "$label: N/A"
        } else {
            "$label: $value"
        }
    }

    // Displaying a message if bypassVulnerabilities is empty or null
    if (bypassVulnerabilities.isNullOrEmpty()) {
        Text("No Bypass Vulnerabilities data available", modifier = Modifier.padding(16.dp), color = Color.Gray)
        return
    }

    // Loop through each BypassVulnerability and display
    Column(modifier = Modifier.padding(16.dp)) {
        bypassVulnerabilities.forEach { vulnerability ->
            // Reset missing values count for each vulnerability
            missingValuesCount = 0

            Text(text = "Bypass Vulnerability", fontWeight = FontWeight.Bold)

            // Displaying Type
            Text(text = displayField("Type", vulnerability.type))

            // Displaying Description
            Text(text = displayField("Description", vulnerability.description))

            // Displaying Severity
            Text(text = displayField("Severity", vulnerability.severity))

            // Displaying Mitigation
            Text(text = displayField("Mitigation", vulnerability.mitigation))

            // Displaying Exploitable Status
            Text(text = "Exploitable: ${if (vulnerability.exploitable) "Yes" else "No"}")

            // Displaying the count of missing values for this vulnerability
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Missing Values: $missingValuesCount", fontWeight = FontWeight.Bold, color = Color.Red)

            // Add a divider between vulnerabilities
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Displaying the total missing Bypass Vulnerabilities count if necessary
        Text(text = "Total Empty Bypass Vulnerabilities: $missingValuesCount", fontWeight = FontWeight.Bold, color = Color.Red)
    }
}

