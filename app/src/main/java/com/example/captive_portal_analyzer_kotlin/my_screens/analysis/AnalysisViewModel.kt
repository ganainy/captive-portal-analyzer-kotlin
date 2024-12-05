package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter.formatIpAddress
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.dataclasses.BypassVulnerability
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.dataclasses.Credential
import com.example.captive_portal_analyzer_kotlin.dataclasses.DefaultCredentialResult
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkInformation
import com.example.captive_portal_analyzer_kotlin.dataclasses.SessionTokenAnalysis
import com.example.captive_portal_analyzer_kotlin.dataclasses.TLSAnalysis
import com.example.captive_portal_analyzer_kotlin.utils.CaptivePortalDetector.detectPortalURL
import com.example.captive_portal_analyzer_kotlin.utils.Utils
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.calculateChannel
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.getDnsServers
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.getSecurityType
import com.example.captive_portal_analyzer_kotlin.utils.Utils.Companion.isNetworkSecure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import java.util.Arrays
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.log2
import androidx.compose.runtime.State
import com.example.captive_portal_analyzer_kotlin.dataclasses.JavaScriptAnalysisReport
import com.example.captive_portal_analyzer_kotlin.utils.JavaScriptInterface
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.net.ssl.HostnameVerifier

sealed class AnalysisUiState {
    object Loading : AnalysisUiState()
    object Initial : AnalysisUiState()
    data class Analyzed(val captivePortalReport: CaptivePortalReport) : AnalysisUiState()
    data class Error(val messageStringResource: Int) : AnalysisUiState()
}


class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Initial)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    val htmlContent: MutableLiveData<String> = MutableLiveData()
    val javascriptContent: MutableLiveData<String> = MutableLiveData()

    private val redirectChain: MutableList<String> = ArrayList()
    private val headers: MutableMap<String, String> = HashMap()
    private val formFields: List<String> = ArrayList()
    private val javascriptAnalysis = StringBuilder()
    var portalReport: CaptivePortalReport = CaptivePortalReport()

    private val _portalUrl = mutableStateOf("") // Default URL
    val portalUrl: State<String> get() = _portalUrl


    private val connectivityManager: ConnectivityManager? = null

    init {
        getCaptivePortalAddress()
    }

    fun getCaptivePortalAddress(testUrl: String = "http://connectivitycheck.gstatic.com:80/generate_204") {
        viewModelScope.launch(Dispatchers.IO) { // IO dispatcher for background work
            try {
                val portalUrl = detectPortalURL(testUrl)
                portalReport.portalUrl = portalUrl
                portalReport = initializePortalAnalysis(portalUrl) // do initial analysis on homepage of captive portal
                updateUrl(portalUrl) //pass url to ui to load in the webview

                // Switch back to the Main thread to update the UI
                withContext(Dispatchers.Main) {

                }
            } catch (e: Exception) {
                e.printStackTrace() // Handle exceptions appropriately
            }
        }
    }

    //update the url which will be passed to ui to show in the webview
    fun updateUrl(newUrl: String) {
        _portalUrl.value = newUrl
    }

    fun onWebViewPageFinished(pageUrl: String, userAgentString: String) {
        viewModelScope.launch {
            try {

                    val pageAnalysis = analyzeWebpage(
                        agentString =userAgentString,
                        currentUrl = pageUrl
                    )
                    portalReport.addPageAnalysis(pageAnalysis)

            } catch (e: Exception) {
                // Handle errors
                println("Error during captive portal analysis: ${e.message}")
            }
        }
    }

    //todo
    fun analyzeFormFields(formDataJson: String) {
        // Process form fields as needed
        // val formData = parseFormDataFromJson(formDataJson)
        // Update state or perform analysis
    }

    fun onAnalysisComplete(captivePortalReport: CaptivePortalReport) {
        // Update the UI with the analyzed report
        _uiState.update { AnalysisUiState.Analyzed(captivePortalReport) }
    }


    suspend fun initializePortalAnalysis(portalUrl: String?): CaptivePortalReport {

        //make sure only initial page of the captive portal is analyzed
        if (portalUrl != portalReport.portalUrl) {
            return portalReport
        }

        return withContext(Dispatchers.IO) {
            val report = CaptivePortalReport()

            // One-time portal-level analysis
            report.apply {
                this.portalUrl = portalUrl

                // Network-level information (collected once)
                networkInformation = collectNetworkInfo(getApplication<Application>())

                // Security configuration analysis
                if (portalUrl != null) {
                    usesHttps = portalUrl.startsWith("https")
                    tlsAnalysis = analyzeTLSConfiguration(portalUrl)
                    defaultCredentials = checkDefaultCredentials(portalUrl)
                }

                // Portal-wide security checks
                bypassVulnerabilities = checkBypassVulnerabilities()
            }

            report
        }
    }

    suspend fun analyzeWebpage(agentString: String,currentUrl: String): CaptivePortalReport.PageAnalysis {
        return withContext(Dispatchers.IO) {
            val pageAnalysis = CaptivePortalReport.PageAnalysis().apply {
                pageUrl = currentUrl

                // Page-specific analysis
                headers = collectHeaders(
                    userAgentString = agentString,
                    url = currentUrl
                )
                //todo execute both functions without passing the webView directly
                //formFields = extractFormFields()
               // javascriptAnalysis = analyzeJavaScript().toString()
                cookiesFound = analyzeCookies(currentUrl)
                potentialVulnerabilities = analyzeVulnerabilities(currentUrl)
                sessionTokens = analyzeSessionTokens(currentUrl)
            }
            pageAnalysis
        }
    }


    /**
     * Collects and analyzes HTTP headers from the current web page.
     *
     * @param webView The WebView from which to extract headers
     * @return A map of collected HTTP headers with their values
     */
    fun collectHeaders(userAgentString: String, url: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        try {
            // Add basic headers
            headers["User-Agent"] = userAgentString
            // Add any additional static headers you might want to include
            headers["Host"] = Uri.parse(url).host ?: ""

            // If you want to add more headers, you can do so statically
            headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            headers["Accept-Language"] = "en-US,en;q=0.5"

        } catch (e: Exception) {
            // Log header collection errors
            Log.e("HeaderCollection", "Error collecting headers", e)
        }

        return headers
    }

    /**
     * Extracts form fields from the current web page using JavaScript injection.
     *
     * @param webView The WebView to analyze
     * @return A list of form field descriptions
     */
    fun extractFormFields(webView: WebView): List<String> {
        val formFields = mutableListOf<String>()

        webView.evaluateJavascript(
            """
        (function() {
            var forms = document.getElementsByTagName('form');
            var fieldData = [];
            
            for (var i = 0; i < forms.length; i++) {
                var form = forms[i];
                var inputs = form.getElementsByTagName('input');
                
                for (var j = 0; j < inputs.length; j++) {
                    var input = inputs[j];
                    fieldData.push(JSON.stringify({
                        type: input.type || 'unknown',
                        name: input.name || '',
                        id: input.id || '',
                        placeholder: input.placeholder || '',
                        required: input.required || false,
                        autocomplete: input.autocomplete || 'off'
                    }));
                }
            }
            
            return JSON.stringify(fieldData);
        })()
        """.trimIndent()
        ) { result ->
            try {
                // Parse the returned JSON result
                val parsedFields = JSONArray(result)
                for (k in 0 until parsedFields.length()) {
                    formFields.add(parsedFields.getString(k))
                }
            } catch (e: JSONException) {
                Log.e("FormFieldExtraction", "Error parsing form fields", e)
            }
        }

        return formFields
    }

    /**
     * Performs comprehensive JavaScript analysis on the current web page.
     *
     * @param webView The WebView to analyze
     * @return A detailed JavaScript analysis report
     */
    fun analyzeJavaScript(webView: WebView): JavaScriptAnalysisReport {
        val report = JavaScriptAnalysisReport()

        // Collect basic JavaScript information
        webView.evaluateJavascript(
            """
        (function() {
            var result = {
                scriptCount: document.scripts.length,
                inlineScripts: [],
                externalScripts: [],
                eventHandlers: [],
                windowProperties: []
            };
            
            // Collect inline scripts
            document.querySelectorAll('script:not([src])').forEach(function(script) {
                result.inlineScripts.push(script.textContent.substring(0, 100)); // Limit to first 100 chars
            });
            
            // Collect external scripts
            document.querySelectorAll('script[src]').forEach(function(script) {
                result.externalScripts.push(script.src);
            });
            
            // Detect event listeners
            ['click', 'submit', 'load', 'change'].forEach(function(event) {
                var elements = document.querySelectorAll('[on' + event + ']');
                elements.forEach(function(el) {
                    result.eventHandlers.push({
                        type: event,
                        element: el.tagName
                    });
                });
            });
            
            // Check for potentially suspicious global properties
            ['eval', 'Function', 'setTimeout', 'setInterval'].forEach(function(prop) {
                if (window[prop]) {
                    result.windowProperties.push(prop);
                }
            });
            
            return JSON.stringify(result);
        })()
        """.trimIndent()
        ) { result ->
            try {
                val analysisData = JSONObject(result)

                report.apply {
                    scriptCount = analysisData.getInt("scriptCount")
                    inlineScripts = analysisData.getJSONArray("inlineScripts")
                        .let { array -> (0 until array.length()).map { array.getString(it) } }
                    externalScripts = analysisData.getJSONArray("externalScripts")
                        .let { array -> (0 until array.length()).map { array.getString(it) } }

                    // Analyze potential security risks
                    securityRisks = mutableListOf<String>().apply {
                        if (inlineScripts.any { it.contains("eval(") })
                            add("Potential eval() injection risk")

                        if (externalScripts.any { it.contains("untrusted") })
                            add("Suspicious external script source")
                    }
                }
            } catch (e: JSONException) {
                Log.e("JavaScriptAnalysis", "Error parsing JavaScript analysis", e)
            }
        }

        return report
    }

    private fun analyzeCookies(portalUrl: String?): Map<String, String> {
        val cookies: MutableMap<String, String> = java.util.HashMap()
        val cookieString = CookieManager.getInstance().getCookie(portalUrl)
        if (cookieString != null) {
            val cookiePairs =
                cookieString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (pair in cookiePairs) {
                val keyValue = pair.split("=".toRegex(), limit = 2).toTypedArray()
                if (keyValue.size == 2) {
                    cookies[keyValue[0].trim { it <= ' ' }] = keyValue[1].trim { it <= ' ' }
                }
            }
        }
        return cookies
    }

    private fun analyzeVulnerabilities(portalUrl: String?): List<String> {
        val vulnerabilities: MutableList<String> = java.util.ArrayList()

        // Check for basic security issues
        if (portalUrl?.startsWith("https") == true) {
            vulnerabilities.add("Portal uses unencrypted HTTP")
        }

        // Check for insecure form submissions
        if (javascriptAnalysis.toString().contains("Insecure Forms")) {
            vulnerabilities.add("Forms submit data over unencrypted connection")
        }

        // Check for mixed content
        if (javascriptAnalysis.toString().contains("Mixed Content")) {
            vulnerabilities.add("Page contains mixed content (HTTP resources on HTTPS page)")
        }

        return vulnerabilities
    }

    fun collectNetworkInfo(context: Context): NetworkInformation? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo

        if (wifiInfo == null || wifiInfo.ssid == WifiManager.UNKNOWN_SSID) {
            return null // No active Wi-Fi connection
        }

        // Get DHCP Info
        val dhcpInfo: DhcpInfo? = wifiManager.dhcpInfo

        return NetworkInformation(
            ssid = wifiInfo.ssid.trim('"'), // Remove quotation marks from SSID
            bssid = wifiInfo.bssid,
            ipAddress = formatIpAddress(wifiInfo.ipAddress),
            gatewayAddress = dhcpInfo?.gateway?.let { formatIpAddress(it) },
            linkSpeed = wifiInfo.linkSpeed,
            rssi = wifiInfo.rssi,
            macAddress = wifiInfo.macAddress,
            frequency = wifiInfo.frequency,
            channel = calculateChannel(wifiInfo.frequency),
            securityType = getSecurityType(wifiManager, wifiInfo),
            dnsServers = getDnsServers(dhcpInfo),
            isSecure = isNetworkSecure(getSecurityType(wifiManager, wifiInfo))
        )
    }

    private fun checkBypassVulnerabilities(): List<BypassVulnerability> {
        val vulnerabilities: MutableList<BypassVulnerability> =
            java.util.ArrayList<BypassVulnerability>()

        // Check DNS tunnel susceptibility
        checkDNSTunnelBypass(vulnerabilities)

        // Check MAC spoofing vulnerability
        checkMACSpoofingBypass(vulnerabilities)

        // Check for SSL strip vulnerability
        checkSSLStripBypass(vulnerabilities)

        // Check for packet manipulation vulnerability
        checkPacketManipulationBypass(vulnerabilities)

        return vulnerabilities
    }

    private fun checkDNSTunnelBypass(vulnerabilities: MutableList<BypassVulnerability>) {
        try {
            // Simulate DNS tunnel by sending a DNS query to a public resolver (e.g., 8.8.8.8)
            val command = "nslookup example.com 8.8.8.8"
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val vulnerability = BypassVulnerability()
                vulnerability.type = "DNS Tunnel Susceptibility"
                vulnerability.description =
                    "Captive portal allows DNS tunneling, which could bypass restrictions."
                vulnerability.severity = "High"
                vulnerability.mitigation =
                    "Implement DNS filtering or restrict external DNS servers."
                vulnerability.exploitable = true
                vulnerabilities.add(vulnerability)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun checkMACSpoofingBypass(vulnerabilities: MutableList<BypassVulnerability>) {
        try {
            // Simulate MAC address spoofing by attempting to change MAC address (requires admin rights)
            val command = "ifconfig eth0 hw ether 00:11:22:33:44:55"
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val vulnerability = BypassVulnerability()
                vulnerability.type = "MAC Spoofing"
                vulnerability.description =
                    "Captive portal allows bypassing by spoofing MAC addresses."
                vulnerability.severity = "Medium"
                vulnerability.mitigation = "Implement additional client identification mechanisms."
                vulnerability.exploitable = true
                vulnerabilities.add(vulnerability)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun checkSSLStripBypass(vulnerabilities: MutableList<BypassVulnerability>) {
        try {
            // Simulate an SSL strip by performing an HTTP request over a non-SSL connection
            val url = URL("http://example.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val vulnerability = BypassVulnerability()
                vulnerability.type = "SSL Strip Vulnerability"
                vulnerability.description =
                    "Captive portal allows unencrypted HTTP requests, susceptible to SSL stripping."
                vulnerability.severity = "High"
                vulnerability.mitigation =
                    "Enforce HTTPS and redirect HTTP traffic to secure connections."
                vulnerability.exploitable = true
                vulnerabilities.add(vulnerability)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPacketManipulationBypass(vulnerabilities: MutableList<BypassVulnerability>) {
        try {
            // Send a malformed or fragmented packet to test for packet manipulation
            val command = "ping -c 1 -s 1500 example.com" // Adjust payload size for fragmentation
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val vulnerability = BypassVulnerability()
                vulnerability.type = "Packet Manipulation"
                vulnerability.description =
                    "Captive portal is vulnerable to packet manipulation or fragmentation attacks."
                vulnerability.severity = "Medium"
                vulnerability.mitigation = "Inspect and validate packet integrity."
                vulnerability.exploitable = true
                vulnerabilities.add(vulnerability)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun analyzeSessionTokens(webViewUrl: String?): SessionTokenAnalysis {
        val analysis = SessionTokenAnalysis().apply {
            vulnerabilities = mutableListOf()
            cookieFlags = mutableMapOf()
        }

        // Get cookies
        val cookies = CookieManager.getInstance().getCookie(webViewUrl)
        if (!cookies.isNullOrEmpty()) {
            // Analyze session cookies
            val sessionTokens = mutableListOf<String>()
            val cookiePairs = cookies.split(";").map { it.trim() }

            for (pair in cookiePairs) {
                val keyValue = pair.split("=", limit = 2).map { it.trim() }
                if (keyValue.size == 2) {
                    val (name, value) = keyValue

                    // Identify session-related cookies
                    if (name.contains("session", ignoreCase = true) ||
                        name.contains("token", ignoreCase = true) ||
                        name.contains("auth", ignoreCase = true)
                    ) {
                        sessionTokens.add(value)

                        // Check cookie flags
                        analysis.cookieFlags?.set(name, pair)

                        if (!pair.contains("secure", ignoreCase = true)) {
                            analysis.vulnerabilities?.add("Cookie '$name' missing Secure flag")
                        }
                        if (!pair.contains("httponly", ignoreCase = true)) {
                            analysis.vulnerabilities?.add("Cookie '$name' missing HttpOnly flag")
                        }
                        if (!pair.contains("samesite", ignoreCase = true)) {
                            analysis.vulnerabilities?.add("Cookie '$name' missing SameSite attribute")
                        }
                    }
                }
            }

            // Calculate entropy
            if (sessionTokens.isNotEmpty()) {
                val tokenEntropy = calculateEntropy(sessionTokens.first())
                analysis.entropy = tokenEntropy
                analysis.predictable = tokenEntropy < 4.0

                if (analysis.predictable) {
                    analysis.vulnerabilities?.add("Session tokens have low entropy: $tokenEntropy")
                }
            }
        }

        return analysis
    }

    // Calculate Shannon entropy of a string
    private fun calculateEntropy(input: String): Double {
        val charFrequencies = input.groupingBy { it }.eachCount()
        val length = input.length.toDouble()

        return charFrequencies.values.sumOf { freq ->
            val probability = freq / length
            -probability * log2(probability)
        }
    }

    private fun checkDefaultCredentials(portalUrl: String): List<DefaultCredentialResult> {
        val results: MutableList<DefaultCredentialResult> =
            java.util.ArrayList<DefaultCredentialResult>()

        // Common default credentials for various captive portal systems
        val commonCredentials: List<Credential> =
            Arrays.asList<Credential>(
                Credential("admin", "admin"),
                Credential("administrator", "password"),
                Credential("root", "toor"),
                Credential("guest", "guest")
            )

        for (cred in commonCredentials) {
            val result: DefaultCredentialResult = DefaultCredentialResult()
            result.portal = portalUrl
            result.username = cred.username
            result.passwordHash = Utils.hashPassword(cred.password)

            try {
                // Test authentication endpoints
                val endpoints = arrayOf("/admin", "/login", "/manage", "/configure")

                for (endpoint in endpoints) {
                    val testUrl = portalUrl + endpoint
                    result.endpoint = endpoint
                    result.accessible = testAuthentication(testUrl, cred)

                    if (result.accessible) {
                        results.add(result)
                        break
                    }
                }
            } catch (e: java.lang.Exception) {
                // Log error but continue testing
                Log.e("CredentialTest", "Error testing credential: " + cred.username)
            }
        }

        return results
    }

    private fun testAuthentication(
        testUrl: String,
        credential: Credential
    ): Boolean {
        try {
            // Create a URL object
            val url = URL(testUrl)
            val connection = url.openConnection() as HttpURLConnection

            // Set HTTP request method and headers
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            // Prepare data for POST request
            val postData =
                ("username=" + credential.username).toString() + "&password=" + credential.password

            connection.outputStream.use { os ->
                os.write(postData.toByteArray())
                os.flush()
            }
            // Get the response code
            val responseCode = connection.responseCode

            // Return true if authentication is successful (e.g., HTTP 200)
            return responseCode == 200
        } catch (e: java.lang.Exception) {
            // Log errors and return false for failed authentication
            Log.e("testAuthentication", "Error during authentication attempt: $testUrl", e)
            return false
        }
    }

    fun analyzeTLSConfiguration(url: String): TLSAnalysis {
        val tlsAnalysis = TLSAnalysis().apply {
            vulnerabilities = mutableListOf()
        }

        var connection: HttpsURLConnection? = null
        var socket: SSLSocket? = null

        try {
            // Validate and establish HTTPS connection
            val targetUrl = URL(url)
            connection = (targetUrl.openConnection() as HttpsURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                hostnameVerifier =
                    HostnameVerifier { hostname, session -> hostname == targetUrl.host }
                connect()
            }

            // Analyze SSL/TLS certificate
            val certificates = connection.serverCertificates
            if (certificates.isNotEmpty() && certificates[0] is X509Certificate) {
                val cert = certificates[0] as X509Certificate

                tlsAnalysis.apply {
                    certificateValid = true
                    certificateIssuer = cert.issuerX500Principal.name
                    certificateExpiry = cert.notAfter

                    // Check if certificate is about to expire
                    val daysToExpiry =
                        ((cert.notAfter.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))
                    if (daysToExpiry < 30) {
                        vulnerabilities.add("Certificate expires in $daysToExpiry days")
                    }
                }
            }

            // Analyze TLS version and cipher suites
            socket = (SSLSocketFactory.getDefault()
                .createSocket(targetUrl.host, 443) as SSLSocket).apply {
                startHandshake() // Ensure the handshake is performed
            }

            tlsAnalysis.apply {
                tlsVersion = socket.session.protocol
                supportedCipherSuites = socket.enabledCipherSuites.toList()

                // Check for weak ciphers
                val weakCiphers = listOf("SSL", "MD5", "RC4", "DES")
                hasWeakCiphers = supportedCipherSuites.any { cipher ->
                    weakCiphers.any { weak -> cipher.contains(weak, ignoreCase = true) }
                }

                if (hasWeakCiphers) {
                    vulnerabilities.add("Weak ciphers detected")
                }
            }
        } catch (e: Exception) {
            tlsAnalysis.vulnerabilities.add("Error analyzing TLS: ${e.message}")
        } finally {
            try {
                connection?.disconnect()
            } catch (ignored: Exception) {
            }

            try {
                socket?.close()
            } catch (ignored: Exception) {
            }
        }

        return tlsAnalysis
    }

    // Function to initiate content extraction from WebView
    fun extractHTMLandJavascript(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(JavaScriptInterface(), "AndroidInterface")

        // Set WebViewClient to load JS content
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                // Extract HTML
                webView.loadUrl("javascript:AndroidInterface.receiveHTML(document.documentElement.outerHTML);")

                // Extract JavaScript
                webView.loadUrl(
                    "javascript:(function() { " +
                            "var scripts = document.getElementsByTagName('script');" +
                            "var allJS = '';" +
                            "for(var i = 0; i < scripts.length; i++) {" +
                            "if(scripts[i].src) {" +
                            "allJS += '// External Script: ' + scripts[i].src + '\\n';" +
                            "} else {" +
                            "allJS += '// Inline Script\\n' + scripts[i].innerHTML + '\\n';" +
                            "}" +
                            "}" +
                            "AndroidInterface.receiveJavaScript(allJS);" +
                            "})();"
                )
            }
        }
    }

    // JavaScript Interface class to receive content
    private inner class JavaScriptInterface {

        @JavascriptInterface
        fun receiveHTML(html: String) {
            htmlContent.postValue(html) // Update LiveData with HTML content
        }

        @JavascriptInterface
        fun receiveJavaScript(javascript: String) {
            javascriptContent.postValue(javascript) // Update LiveData with JavaScript content
        }
    }

}


/*
class FeedViewModelFactory(private val repository: IDataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LandingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LandingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}*/
