/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.asFlow
import com.vaxcare.core.api.di.OkHttpClientFastTimeout
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.core.extension.combineWith
import com.vaxcare.vaxhub.core.extension.getMainThread
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.SignalStrengthLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Application,
    @OkHttpClientFastTimeout private val httpClient: OkHttpClient
) {
    companion object {
        /**
         * The interval that we will ping vaxcare.com with
         */
        private const val INTERVAL = 1000L * 60L * 60L

        /**
         * How long we should wait between 400-599 http status codes on http requests
         */
        private const val HOLDTIME = 1000 * 5

        private const val VAXCARE_URL = "${BuildConfig.VAX_VHAPI_URL}index.txt"
        private const val GOOGLE_URL = "https://www.google.com"

        // Check the signal strength every 5 seconds
        private const val SIGNAL_STRENGTH_INTERVAL = 1000L * 5L
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var handler: Handler = Handler(Looper.getMainLooper())

    private var isNetworkUp = false
    private var currentStatus = NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE

    var disconnectedTime: LocalDateTime = LocalDateTime.now()

    private var lastNetworkConnectionTime: Long = -1L

    // A single integer from 0 to 4 representing the general signal quality.
    // -1 no cellular network
    // 0 represents very poor signal strength
    // while 4 represents a very strong signal strength.
    val cellularLevel = MutableLiveData(-1)

    private var enterToFrontTimeMillis: Long = 0

    private var networkDisconnectedOnCurrentSession = true

    private var signalStrengthTask: Runnable? = null

    private val signalStrengthHelper: SignalStrengthHelper by lazy {
        SignalStrengthHelper()
    }

    private val _lastCheckNetworkConnection = MutableLiveData(LocalDateTime.now())
    val lastCheckNetworkConnection: LiveData<LocalDateTime> = _lastCheckNetworkConnection

    private val _networkStatus: MutableLiveData<NetworkStatus> =
        MutableLiveData(NetworkStatus.DISCONNECTED)
    val networkStatus: LiveData<NetworkStatus> = _networkStatus

    private val _networkProperties: MutableLiveData<NetworkProperties> =
        MutableLiveData(NetworkProperties())
    val networkProperties: LiveData<NetworkProperties> = _networkProperties

    private val isWiFi = MutableLiveData(true)

    private val _isCellular = MutableLiveData(false)
    val isCellular: LiveData<Boolean> = _isCellular

    private val networkStatusShownUpdate = MutableLiveData<NetworkStatusShownUpdate>()

    class NetworkStatusShownUpdate(
        val ssid: String?,
        val networkStatus: NetworkStatus,
        val signalStrengthLevel: SignalStrengthLevel
    )

    /**
     * Combine NetworkStatus and isWifi boolean for ControlPanelView and return Pair
     */
    val networkInfo: LiveData<Pair<NetworkStatus?, Boolean?>> =
        networkStatus.combineWith(isWiFi) { status, wifi -> Pair(status, wifi) }

    /**
     * Combine NetworkStatus and NetworkStatusShownUpdate for Main and return Pair
     */
    val networkStatusForUI: Flow<Pair<NetworkStatus?, NetworkStatusShownUpdate?>> =
        networkStatus.combineWith(networkStatusShownUpdate) { status, update ->
            Pair(
                status,
                update
            )
        }.asFlow()

    private fun updateCellularNetworkStatus() {
        context.getMainThread {
            val telephonyManager: TelephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val level = if (telephonyManager.simState == TelephonyManager.SIM_STATE_READY) {
                telephonyManager.signalStrength?.level ?: -1
            } else {
                -1
            }

            if (cellularLevel.value != level) {
                cellularLevel.value = level
            }
        }
    }

    init {
        context.getMainThread {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Timber.d("Network available $network")
                    val capabilities = cm.getNetworkCapabilities(network)
                    if (capabilities != null) {
                        isNetworkUp = true
                        context.getMainThread {
                            isWiFi.postValue(
                                when {
                                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> false
                                    else -> capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                }
                            )

                            _isCellular.postValue(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                        }
                    }
                    updateCellularNetworkStatus()
                    startCheckConnectionTask()
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    Timber.d("Losing network.")
                    updateNetworkStatus(NetworkStatus.DISCONNECTED)
                    updateCellularNetworkStatus()
                    isNetworkUp = false
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Timber.d("Network Lost.")
                    updateNetworkStatus(NetworkStatus.DISCONNECTED)
                    updateCellularNetworkStatus()
                    isNetworkUp = false
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Timber.d("Network Unavailable.")
                    updateNetworkStatus(NetworkStatus.DISCONNECTED)
                    updateCellularNetworkStatus()
                    isNetworkUp = false
                }
            })
            updateCellularNetworkStatus()

            registerScreenStateReceiver()
        }
    }

    fun isCurrentlyOnline(): Boolean = (networkStatus.value ?: NetworkStatus.DISCONNECTED) == NetworkStatus.CONNECTED

    fun setupSignalStrengthListeners(lifecycle: Lifecycle) {
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                startSignalStrengthTask()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                stopSignalStrengthTask()
            }
        })
    }

    private fun registerScreenStateReceiver() {
        context.getMainThread {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            context.registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            Intent.ACTION_SCREEN_ON ->
                                enterToFrontTimeMillis =
                                    System.currentTimeMillis()

                            Intent.ACTION_SCREEN_OFF -> networkDisconnectedOnCurrentSession = false
                        }
                    }
                },
                filter
            )
        }
    }

    private fun startSignalStrengthTask() {
        Timber.d("Start get signal strength")
        if (signalStrengthTask == null) {
            signalStrengthTask = object : Runnable {
                override fun run() {
                    val telephonyManager: TelephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    if (telephonyManager.simState == TelephonyManager.SIM_STATE_READY) {
                        signalStrengthHelper.getSignalStrength(context)?.let {
                            cellularLevel.value = it.level
                            Timber.d("signal strength: ${it.level}")
                        }
                    } else {
                        cellularLevel.value = -1
                    }
                    handler.postDelayed(this, SIGNAL_STRENGTH_INTERVAL)
                }
            }
        }
        signalStrengthTask?.let {
            handler.removeCallbacks(it)
            it.run()
        }
    }

    private fun stopSignalStrengthTask() {
        Timber.d("Stop get signal strength")
        signalStrengthTask?.let {
            handler.removeCallbacks(it)
        }
        signalStrengthTask = null
    }

    fun setResponseCode(code: Int) {
        if (code.isReachableResponseCode() && currentStatus != NetworkStatus.CONNECTED) {
            updateNetworkStatus(NetworkStatus.CONNECTED)
        } else if (code != 401 && code != 403 && code != 423 && code in 400..599 && isNetworkUp) {
            updateNetworkStatus(NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE)

            val timeElapsedBetweenLastTimeConnectionCheckedAndNow =
                ChronoUnit.SECONDS.between(LocalDateTime.now(), lastCheckNetworkConnection.value)

            if (timeElapsedBetweenLastTimeConnectionCheckedAndNow >= HOLDTIME) {
                startCheckConnectionTask()
            }
        }
    }

    private fun Int.isReachableResponseCode() = (this in 200..299 || this == 401 || this == 403 || this == 423)

    private fun updateNetworkStatus(newStatus: NetworkStatus) {
        currentStatus = newStatus
        val oldStatus = networkStatus.value
        val networkProperties = updateProperties()

        context.getMainThread {
            _networkProperties.value = networkProperties

            if (oldStatus != newStatus) {
                Timber.d("Network status changed from $oldStatus to $newStatus")
                _networkStatus.value = newStatus

                if (newStatus != NetworkStatus.CONNECTED) {
                    networkDisconnectedOnCurrentSession = true
                    disconnectedTime = LocalDateTime.now()
                }
            }

            networkProperties.dBmSignal?.let { signal ->
                if (networkStatusShownUpdate.value?.signalStrengthLevel != SignalStrengthLevel.fromInt(
                        signal
                    )
                ) {
                    val signalStrengthLevel =
                        if (newStatus == NetworkStatus.CONNECTED_NO_INTERNET) {
                            SignalStrengthLevel.NO_INTERNET
                        } else {
                            SignalStrengthLevel.fromInt(signal)
                        }
                    networkStatusShownUpdate.value = NetworkStatusShownUpdate(
                        ssid = networkProperties.ssid,
                        networkStatus = newStatus,
                        signalStrengthLevel = signalStrengthLevel
                    )
                }
            }
        }

        // Restart the connection task after re-connects
        if (newStatus == NetworkStatus.CONNECTED && oldStatus != NetworkStatus.CONNECTED) {
            lastNetworkConnectionTime = System.currentTimeMillis()
            startCheckConnectionTask()
        }
    }

    private fun updateProperties(): NetworkProperties {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifi.connectionInfo.ssid
        val frequency = wifi.connectionInfo.frequency
        val security = decodeSecurityType(wifi.connectionInfo)
        val dBmSignal = wifi.connectionInfo.rssi

        var upSpeed: Int? = null
        var downSpeed: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            upSpeed = wifi.connectionInfo.txLinkSpeedMbps
            downSpeed = wifi.connectionInfo.rxLinkSpeedMbps
        }

        return NetworkProperties(ssid, security, frequency, dBmSignal, upSpeed, downSpeed)
    }

    private var connectionCheckRunnable: Runnable = object : Runnable {
        override fun run() {
            pingServersAndUpdateNetWorkStatus()
            handler.postDelayed(this, INTERVAL)
        }
    }

    /**
     * Start the connection check pre 1 hour
     */
    fun startCheckConnectionTask() {
        Timber.d("Start check network connection")
        handler.removeCallbacks(connectionCheckRunnable)
        connectionCheckRunnable.run()
    }

    /**
     * Ping to check connection status.
     * Ping Google URL, then VaxCare URL
     */
    fun pingServersAndUpdateNetWorkStatus(callback: (networkStatus: NetworkStatus) -> Unit = {}) {
        // if the OS has no network, then checking for urls will set the wrong message
        if (!isNetworkUp) {
            callback(NetworkStatus.DISCONNECTED)
            return
        }

        scope.safeLaunch {
            if (pingURL(VAXCARE_URL) == -1) {
                if (pingURL(GOOGLE_URL) == -1) {
                    Timber.d("Ping Google failed")
                    updateNetworkStatus(NetworkStatus.CONNECTED_NO_INTERNET)
                    callback(NetworkStatus.CONNECTED_NO_INTERNET)
                } else {
                    Timber.d("Ping VaxCare failed")
                    updateNetworkStatus(NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE)
                    callback(NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE)
                }
            } else {
                Timber.d("Ping VaxCare success")
                updateNetworkStatus(NetworkStatus.CONNECTED)
                callback(NetworkStatus.CONNECTED)
            }
        }
    }

    private fun pingURL(url: String): Int {
        if (url.contains("vaxcare.com")) {
            context.getMainThread {
                _lastCheckNetworkConnection.value = LocalDateTime.now()
            }
        }
        return try {
            val response = httpClient.newCall(
                Request.Builder().url(url).build()
            ).execute()
            response.code
        } catch (e: Exception) {
            Timber.e(e, "Ping to $url failed")
            -1
        }
    }

    enum class ConnectionType(val displayName: String) {
        CELLULAR("SIM"),
        WIFI("Wifi")
    }

    private fun decodeSecurityType(wifiInfo: WifiInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (wifiInfo.currentSecurityType) {
                WifiInfo.SECURITY_TYPE_DPP -> "DPP"
                WifiInfo.SECURITY_TYPE_EAP -> "EAP"
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> "WPA3_ENTERPRISE"
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> "WPA3_ENTERPRISE_192_BIT"
                WifiInfo.SECURITY_TYPE_OPEN -> "OPEN"
                WifiInfo.SECURITY_TYPE_OSEN -> "OSEN"
                WifiInfo.SECURITY_TYPE_OWE -> "OWE"
                WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2 -> "PASSPOINT_R1_R2"
                WifiInfo.SECURITY_TYPE_PASSPOINT_R3 -> "PASSPOINT_R3"
                WifiInfo.SECURITY_TYPE_PSK -> "PSK"
                WifiInfo.SECURITY_TYPE_SAE -> "SAE"
                WifiInfo.SECURITY_TYPE_UNKNOWN -> "UNKNOWN"
                WifiInfo.SECURITY_TYPE_WAPI_CERT -> "WAPI_CERT"
                WifiInfo.SECURITY_TYPE_WAPI_PSK -> "WAPI_PSK"
                WifiInfo.SECURITY_TYPE_WEP -> "WEP"
                else -> wifiInfo.currentSecurityType.toString()
            }
        } else {
            "UNABLE_TO_DETERMINE"
        }
    }
}
