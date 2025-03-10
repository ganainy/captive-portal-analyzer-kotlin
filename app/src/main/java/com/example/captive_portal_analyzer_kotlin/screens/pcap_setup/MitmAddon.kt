
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.RemoteException
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.MitmAPI
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.MitmListener
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.PCAPUtils
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.Prefs
import java.io.IOException
import java.lang.ref.WeakReference

class MitmAddon(ctx: Context, private val mReceiver: MitmListener) {
    private val mContext: Context = ctx.applicationContext
    private val mMessenger: Messenger = Messenger(ReplyHandler(ctx.mainLooper, mReceiver))
    private var mService: Messenger? = null
    private var mStopRequested = false

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Service connected")
            mService = Messenger(service)

            if (mStopRequested) stopProxy()
            else mReceiver.onMitmServiceConnect()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "Service disconnected")
            disconnect() // call unbind to prevent new connections
            mReceiver.onMitmServiceDisconnect()
        }

        override fun onBindingDied(name: ComponentName) {
            Log.w(TAG, "onBindingDied")
            disconnect()
            mReceiver.onMitmServiceDisconnect()
        }

        override fun onNullBinding(name: ComponentName) {
            Log.w(TAG, "onNullBinding")
            disconnect()
            mReceiver.onMitmServiceDisconnect()
        }
    }

    fun connect(extra_flags: Int): Boolean {
        val intent = Intent().apply {
            component = ComponentName(MitmAPI.PACKAGE_NAME, MitmAPI.MITM_SERVICE)
        }

        if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS or extra_flags)) {
            try {
                mContext.unbindService(mConnection)
            } catch (ignored: IllegalArgumentException) {
                Log.w(TAG, "unbindService failed")
            }
            mService = null
            return false
        }
        return true
    }

    fun disconnect() {
        if (mService != null) {
            Log.i(TAG, "Unbinding service...")
            try {
                mContext.unbindService(mConnection)
            } catch (ignored: IllegalArgumentException) {
                Log.w(TAG, "unbindService failed")
            }
            mService = null
        }
    }

    val isConnected: Boolean
        get() = mService != null

    fun requestCaCertificate(): Boolean {
        if (mService == null) {
            Log.e(TAG, "Not connected")
            return false
        }

        val msg = Message.obtain(null, MitmAPI.MSG_GET_CA_CERTIFICATE).apply {
            replyTo = mMessenger
        }
        return try {
            mService!!.send(msg)
            true
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }

    fun startProxy(conf: MitmAPI.MitmConfig?): ParcelFileDescriptor? {
        if (mService == null) {
            Log.e(TAG, "Not connected")
            return null
        }

        val pair: Array<ParcelFileDescriptor> = try {
            ParcelFileDescriptor.createReliableSocketPair()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        val msg = Message.obtain(null, MitmAPI.MSG_START_MITM, 0, 0, pair[0]).apply {
            data = Bundle().apply {
                putSerializable(MitmAPI.MITM_CONFIG, conf)
            }
        }

        return try {
            mService!!.send(msg)
            PCAPUtils.safeClose(pair[0])
            pair[1]
        } catch (e: RemoteException) {
            e.printStackTrace()
            PCAPUtils.safeClose(pair[0])
            PCAPUtils.safeClose(pair[1])
            null
        } catch (e: NullPointerException) {
            e.printStackTrace()
            PCAPUtils.safeClose(pair[0])
            PCAPUtils.safeClose(pair[1])
            null
        }
    }

    fun stopProxy(): Boolean {
        if (mService == null) {
            Log.i(TAG, "Not connected, postponing stop message")
            mStopRequested = true
            return true
        }

        Log.i(TAG, "Send stop message")
        val msg = Message.obtain(null, MitmAPI.MSG_STOP_MITM)
        return try {
            mService!!.send(msg)
            mStopRequested = false
            true
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }

    fun disableDoze(): Boolean {
        if (mService == null) return false

        Log.i(TAG, "Send disable doze")
        val msg = Message.obtain(null, MitmAPI.MSG_DISABLE_DOZE)
        return try {
            mService!!.send(msg)
            true
        } catch (e: RemoteException) {
            e.printStackTrace()
            false
        }
    }

    private class ReplyHandler(looper: Looper, receiver: MitmListener) : Handler(looper) {
        private val mReceiver: WeakReference<MitmListener> = WeakReference(receiver)

        override fun handleMessage(msg: Message) {
            Log.d(TAG, "Message: ${msg.what}")

            val receiver = mReceiver.get() ?: return

            if (msg.what == MitmAPI.MSG_GET_CA_CERTIFICATE) {
                val ca_pem = msg.data?.getString(MitmAPI.CERTIFICATE_RESULT)
                receiver.onMitmGetCaCertificateResult(ca_pem)
            }
        }
    }

    companion object {
        const val PACKAGE_VERSION_CODE: Long = 21
        const val PACKAGE_VERSION_NAME: String = "1.4"
        const val REPOSITORY: String = "https://github.com/emanuele-f/PCAPdroid-mitm"
        private const val TAG = "MitmAddon"

        fun getInstalledVersion(ctx: Context): Long {
            return try {
                val pInfo: PackageInfo = PCAPUtils.getPackageInfo(ctx.packageManager, MitmAPI.PACKAGE_NAME, 0)
                PackageInfoCompat.getLongVersionCode(pInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                -1
            }
        }

        fun getInstalledVersionName(ctx: Context): String {
            return try {
                val pInfo: PackageInfo = PCAPUtils.getPackageInfo(ctx.packageManager, MitmAPI.PACKAGE_NAME, 0)
                pInfo.versionName ?: ""
            } catch (e: PackageManager.NameNotFoundException) {
                ""
            }
        }

        fun getUid(ctx: Context): Int {
            return try {
                PCAPUtils.getPackageUid(ctx.packageManager, MitmAPI.PACKAGE_NAME, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                -1
            }
        }

        fun getNewVersionAvailable(ctx: Context): String {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

            if (Prefs.isIgnoredMitmVersion(prefs, PACKAGE_VERSION_NAME)) return ""

            return try {
                val pInfo: PackageInfo = PCAPUtils.getPackageInfo(ctx.packageManager, MitmAPI.PACKAGE_NAME, 0)

                if (PackageInfoCompat.getLongVersionCode(pInfo) >= PACKAGE_VERSION_CODE) return ""

                if (PCAPUtils.isSemanticVersionCompatible(PACKAGE_VERSION_NAME, pInfo.versionName)) PACKAGE_VERSION_NAME else ""
            } catch (ignored: PackageManager.NameNotFoundException) {
                ""
            }
        }

        fun ignoreNewVersion(ctx: Context) {
           /* val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            prefs.edit().putString(Prefs.PREF_IGNORED_MITM_VERSION, PACKAGE_VERSION_NAME).apply()*/
        }

        fun isInstalled(ctx: Context): Boolean {
            return try {
                val pInfo: PackageInfo = PCAPUtils.getPackageInfo(ctx.packageManager, MitmAPI.PACKAGE_NAME, 0)
                PCAPUtils.isSemanticVersionCompatible(PACKAGE_VERSION_NAME, pInfo.versionName)
            } catch (ignored: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun getGithubReleaseUrl(version: String): String {
            return "$REPOSITORY/releases/download/v$version/PCAPdroid-mitm_v${version}_${Build.SUPPORTED_ABIS[0]}.apk"
        }

        fun setCAInstallationSkipped(ctx: Context, skipped: Boolean) {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            prefs.edit().putBoolean(Prefs.PREF_CA_INSTALLATION_SKIPPED, skipped).apply()
        }

        fun isCAInstallationSkipped(ctx: Context): Boolean {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            return prefs.getBoolean(Prefs.PREF_CA_INSTALLATION_SKIPPED, false)
        }

        fun setDecryptionSetupDone(ctx: Context, done: Boolean) {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            prefs.edit().putBoolean(Prefs.PREF_TLS_DECRYPTION_SETUP_DONE, done).apply()
        }

        fun needsSetup(ctx: Context): Boolean {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

            if (!Prefs.isTLSDecryptionSetupDone(prefs)) return true

            if (!isInstalled(ctx)) {
                setDecryptionSetupDone(ctx, false)
                return true
            }

            return false
        }

        fun isDozeEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return !manager.isIgnoringBatteryOptimizations(MitmAPI.PACKAGE_NAME)
        }
    }
}