package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup

import MitmAddon
import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.security.KeyChain
import androidx.activity.result.ActivityResult
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate

class PCAPSetupViewModel(application: Application) : AndroidViewModel(application), MitmListener {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _addonState = MutableStateFlow<AddonState>(AddonState.Installed)
    val addonState: StateFlow<AddonState> get() = _addonState.asStateFlow()

    private val _certificateState = MutableStateFlow<CertificateUiState>(CertificateUiState.Loading)
    val certificateState: StateFlow<CertificateUiState> get() = _certificateState.asStateFlow()

    private val _needsStoragePermission = MutableStateFlow(true)
    val needsStoragePermission: StateFlow<Boolean> = _needsStoragePermission.asStateFlow()

    private val _needsNotificationPermission = MutableStateFlow(true)
    val needsNotificationPermission: StateFlow<Boolean> = _needsNotificationPermission.asStateFlow()

    private val _needsVPNPermission = MutableStateFlow(true)
    val needsVPNPermission: StateFlow<Boolean> = _needsVPNPermission.asStateFlow()

    private val _completedStepsCount = MutableStateFlow(0)
    val completedStepsCount: StateFlow<Int> = _completedStepsCount.asStateFlow()


    private var mitmAddon: MitmAddon? = null
    private var caPem: String? = null
    private var caCert: X509Certificate? = null
    private var fallbackExport = false

    init {
        refreshAddonState()
        connectToAddon()
        checkRequiredPermissions()
        updateCompletedStepsCount()
    }


    /** ------------- Mitm Addon related functions ------------- */

    private fun getInstalledVersionName(): String {
        return MitmAddon.getInstalledVersionName(context)
    }

    private fun isSemanticVersionCompatible(installedVer: String, newVer: String): Boolean {
        return PCAPUtils.isSemanticVersionCompatible(installedVer, newVer)
    }

    private fun getInstalledVersion(): Long {
        return MitmAddon.getInstalledVersion(context)
    }

    fun getGithubReleaseUrl(version: String): String {
        return MitmAddon.getGithubReleaseUrl(version)
    }

    // Returns a non-empty string if a newer, compatible addon version is available
    // Use ignoreNewVersion to silence this
    private fun getNewVersionAvailable(ctx: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

        if (Prefs.isIgnoredMitmVersion(
                prefs,
                MitmAddon.PACKAGE_VERSION_NAME
            )
        )  // update was ignored by the user
            return ""

        // NOTE: currently for the update check we only rely on the addon version hard-coded
        // in the source
        try {
            val pInfo: PackageInfo =
                PCAPUtils.getPackageInfo(ctx.packageManager, MitmAPI.PACKAGE_NAME, 0)

            if (PackageInfoCompat.getLongVersionCode(pInfo) >= MitmAddon.PACKAGE_VERSION_CODE)  // same version or better installed
                return ""

            if (PCAPUtils.isSemanticVersionCompatible(
                    MitmAddon.PACKAGE_VERSION_NAME,
                    pInfo.versionName
                )
            ) return MitmAddon.PACKAGE_VERSION_NAME
        } catch (ignored: PackageManager.NameNotFoundException) {
        }

        return ""
    }

    // returns true only if a compatible addon version is installed
    private fun isAddonInstalled(ctx: Context): Boolean {
        return MitmAddon.isInstalled(ctx)
    }

    fun getAddonState(context: Context) {
        val isInstalled = isAddonInstalled(context)
        val newVersion = getNewVersionAvailable(context)
        val installedVersion = getInstalledVersionName()

        _addonState.update {
            when {
                isInstalled -> AddonState.Installed
                installedVersion.isEmpty() -> AddonState.NotInstalled
                isSemanticVersionCompatible(installedVersion, newVersion) -> AddonState.UpdateAvailable
                getInstalledVersion() < MitmAddon.PACKAGE_VERSION_CODE -> AddonState.NewVersionRequired
                else -> AddonState.IncompatibleVersion
            }
        }
        updateCompletedStepsCount()
    }

    fun getTargetVersion(context: Context): String {
        val newVersion = getNewVersionAvailable(context)
        return if (newVersion.isEmpty()) MitmAddon.PACKAGE_VERSION_NAME else newVersion
    }


    fun refreshAddonState() {
        getAddonState(context)
    }

    /** ------------- Certificate related functions ------------- */
    private fun connectToAddon() {
        if (mitmAddon == null) {
            mitmAddon = MitmAddon(context, this)
        }

        if (!mitmAddon!!.isConnected) {
            if (!mitmAddon!!.connect(0)) {
                _certificateState.value = CertificateUiState.ConnectionError
            }
        }
    }

    fun checkCertificateStatus() {
        val currentCert = caCert

        if (currentCert != null && PCAPUtils.isCAInstalled(currentCert)) {
            MitmAddon.setCAInstallationSkipped(context, false)
            _certificateState.value = CertificateUiState.Installed
        } else {
            requestCertificate()
        }
    }

    private fun requestCertificate() {
        if (mitmAddon?.isConnected == true) {
            if (!mitmAddon!!.requestCaCertificate()) {
                _certificateState.value = CertificateUiState.Error(
                    "requestCaCertificate failed"
                )
            }
        } else {
            connectToAddon()
        }
    }

    fun handleCertificateInstallIntent(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && PCAPUtils.isCAInstalled(caCert)) {
            _certificateState.value = CertificateUiState.Installed
            updateCompletedStepsCount()
        } else {
            fallbackToCertExport()
        }
    }

    fun handleCertificateExportResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val certUri = result.data!!.data
            var written = false

            certUri?.let {
                try {
                    context.contentResolver.openOutputStream(it, "rwt")?.use { outputStream ->
                        PrintWriter(outputStream).use { writer ->
                            writer.print(caPem)
                            written = true
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            if (written) {
                _certificateState.value = CertificateUiState.Exported
            }
        }
    }

    fun canInstallCertViaIntent(): Boolean {
        // On Android < 11, an intent can be used for cert installation
        // On Android 11+, users must manually install the certificate from the settings
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) && !fallbackExport
    }

    fun fallbackToCertExport() {
        // If there are problems with the cert installation via Intent, fallback to export+install
        fallbackExport = true
        onMitmGetCaCertificateResult(caPem)
    }

    fun createInstallCertificateIntent(): Intent {
        val intent = KeyChain.createInstallIntent()
        intent.putExtra(KeyChain.EXTRA_NAME, "PCAPdroid CA")
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, caPem?.toByteArray(StandardCharsets.UTF_8))
        return intent
    }

    fun createExportCertificateIntent(): Intent {
        val fname = "PCAPdroid_CA.crt"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/x-x509-ca-cert")
        intent.putExtra(Intent.EXTRA_TITLE, fname)
        return intent
    }

     override fun onMitmGetCaCertificateResult(caPem: String?) {
        this.caPem = caPem

        if (caPem != null) {
            caCert = PCAPUtils.x509FromPem(caPem)
            if (caCert != null) {
                if (PCAPUtils.isCAInstalled(caCert)) {
                    _certificateState.value = CertificateUiState.Installed
                } else {
                    MitmAddon.setDecryptionSetupDone(context, false)
                    _certificateState.value = if (canInstallCertViaIntent()) {
                        CertificateUiState.ReadyToInstall
                    } else {
                        CertificateUiState.ReadyToExport
                    }
                }
                updateCompletedStepsCount()
            } else {
                _certificateState.value = CertificateUiState.Error("Addon did not return a valid certificate")
            }
        } else {
            _certificateState.value = CertificateUiState.Error("Certificate retrieval failed")
        }
    }

     override fun onMitmServiceConnect() {
        if (!mitmAddon!!.requestCaCertificate()) {
            _certificateState.value = CertificateUiState.Error("requestCaCertificate failed")
        }
    }

    override fun onMitmServiceDisconnect() {
        if (caPem == null) {
            _certificateState.value = CertificateUiState.Error("Addon disconnected")
        }
    }

        override fun onCleared() {
        mitmAddon?.disconnect()
        super.onCleared()
    }

    /** -------------  Storage Permission related functions ------------- */


    private fun checkRequiredPermissions() {
        // Check for storage permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ) {
            val hasStoragePerm = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            _needsStoragePermission.value = !hasStoragePerm
        } else {
            _needsStoragePermission.value = false
        }

        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPerm = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            _needsNotificationPermission.value = !hasNotificationPerm
        } else {
            _needsNotificationPermission.value = false
        }
        // Check for VPN permission
        // VPN permission is determined by whether VpnService.prepare returns null
        val intent = VpnService.prepare(context)
        _needsVPNPermission.value = intent != null

        updateCompletedStepsCount()
    }


    fun onStoragePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _needsStoragePermission.value = false
            updateCompletedStepsCount()
        }
    }


    /** -------------  Notification Permission related functions ------------- */

    fun onNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _needsNotificationPermission.value = false
            updateCompletedStepsCount()
        }
    }

    /** -------------  VPN Permission related functions ------------- */
    fun  onVpnPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _needsVPNPermission.value = false
            updateCompletedStepsCount()
        }
    }

    /** -------------  general functions ------------- */


    private fun updateCompletedStepsCount() {
        var count = 0

        // Step 1: Storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !needsStoragePermission.value) {
            count++
        }

        // Step 2: Notification permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !needsNotificationPermission.value) {
            count++
        }

        // Step 3: VPN permission
        if (!needsVPNPermission.value) {
            count++
        }

        // Step 4: CA certificate installed
        if (_certificateState.value is CertificateUiState.Installed) {
            count++
        }

        // Step 5: MITM addon installed
        if (_addonState.value is AddonState.Installed) {
            count++
        }

        _completedStepsCount.value = count
    }

}

// UI Addon state model
sealed class AddonState {
    object Installed : AddonState()
    object NotInstalled : AddonState()
    object UpdateAvailable : AddonState()
    object NewVersionRequired : AddonState()
    object IncompatibleVersion : AddonState()
}

// UI Certificate state model
sealed class CertificateUiState {
    object Loading : CertificateUiState()
    object Installed : CertificateUiState()
    object ReadyToInstall : CertificateUiState()
    object ReadyToExport : CertificateUiState()
    object Exported : CertificateUiState()
    object ConnectionError : CertificateUiState()
    data class Error(val message: String) : CertificateUiState()
}
