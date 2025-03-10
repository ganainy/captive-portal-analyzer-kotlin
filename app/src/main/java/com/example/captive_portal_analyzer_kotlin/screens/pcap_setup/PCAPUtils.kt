package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class PCAPUtils {

companion object {
    private val TAG: String = "PCAPUtils"

    fun safeClose(obj: Closeable?) {
        if (obj == null) return
        try {
            obj.close()
        } catch (e: IOException) {
            e.localizedMessage?.let { Log.w(TAG, it) }
        }
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(pm: PackageManager, package_name: String?, flags: Int): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getPackageInfo(
            package_name!!, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else pm.getPackageInfo(package_name!!, flags)
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageUid(pm: PackageManager, packageName: String?, flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getPackageUid(
            packageName!!, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else pm.getPackageUid(
            packageName!!, 0
        )
    }

    // true if the two provided versions are semantically compatible (i.e. same major)
    fun isSemanticVersionCompatible(a: String?, b: String?): Boolean {
        val va: Int = getMajorVersion(a)
        return (va >= 0) && (va == getMajorVersion(b))
    }

    fun getMajorVersion(ver: String?): Int {
        if (ver == null) return -1

        var start_idx = 0

        // optionally starts with "v"
        if (ver.startsWith("v")) start_idx = 1

        val end_idx = ver.indexOf('.')
        if (end_idx < 0) return -1

        return try {
            ver.substring(start_idx, end_idx).toInt()
        } catch (ignored: NumberFormatException) {
            -1
        }
    }

    fun isCAInstalled(ca_cert: X509Certificate?): Boolean {
        try {
            val ks = KeyStore.getInstance("AndroidCAStore")
            ks.load(null, null)
            return ks.getCertificateAlias(ca_cert) != null
        } catch (e: KeyStoreException) {
            e.printStackTrace()
            return false
        } catch (e: CertificateException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return false
        }
    }

    fun x509FromPem(pem: String): X509Certificate? {
        val begin = pem.indexOf('\n') + 1
        val end = pem.indexOf('-', begin)

        if ((begin > 0) && (end > begin)) {
            val cert64 = pem.substring(begin, end)
            //Log.d(TAG, "Cert: " + cert64);
            try {
                val cf = CertificateFactory.getInstance("X.509")
                val cert_data = Base64.decode(cert64, Base64.DEFAULT)
                return cf.generateCertificate(ByteArrayInputStream(cert_data)) as X509Certificate
            } catch (e: CertificateException) {
                e.printStackTrace()
            }
        }

        return null
    }


}



}