/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.admin

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.isTouchPointInView
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toFloorOf
import com.vaxcare.vaxhub.core.extension.toPercentValue
import com.vaxcare.vaxhub.databinding.ViewControlPanelBinding
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.service.BatteryMonitor
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.util.BrightnessUtils
import com.vaxcare.vaxhub.util.BrightnessUtils.GAMMA_SPACE_MAX
import com.vaxcare.vaxhub.util.BrightnessUtils.convertLinearToGamma
import timber.log.Timber
import java.time.format.DateTimeFormatter

@SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
class ControlPanelView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var listener: Listener? = null

    // Power
    private var powerPercent: Int = 100
    private var powerConnected: Boolean = false

    // Wifi & system connect
    private var networkStatus: Pair<NetworkStatus?, Boolean?>? = null
    private var networkMonitor: NetworkMonitor? = null

    private val binding: ViewControlPanelBinding =
        ViewControlPanelBinding.inflate(context.getLayoutInflater(), this, true)

    private val brightnessObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            binding.apply {
                val brightness = getCurrentBrightness()
                val brightnessL2G = convertLinearToGamma(brightness)
                brightnessLevel.text = brightnessL2G.toPercentValue(GAMMA_SPACE_MAX)
                brightnessSeekbar.setOnSeekBarChangeListener(null)
                brightnessSeekbar.progress = brightnessL2G
                brightnessSeekbar.setOnSeekBarChangeListener(object :
                    SimpleOnSeekBarChangeListener() {
                    override fun onProgressChanged(progress: Int) {
                        updateBrightness(progress)
                    }
                })
            }
        }
    }

    private val wifiConnectedWhite: Drawable by lazy {
        resources.getDrawable(
            R.drawable.ic_wifi_connected,
            null
        ).apply {
            setTint(Color.WHITE)
        }
    }

    private val systemConnectedWhite: Drawable by lazy {
        resources.getDrawable(
            R.drawable.ic_system_connected,
            null
        ).apply {
            setTint(Color.WHITE)
        }
    }

    private val wifiDisconnectedDrawable: Drawable by lazy {
        resources.getDrawable(
            R.drawable.ic_wifi_disconnected,
            null
        )
    }

    private val systemDisconnectedDrawable: Drawable by lazy {
        resources.getDrawable(
            R.drawable.ic_system_disconnected,
            null
        )
    }

    private val systemErrorDrawable: Drawable by lazy {
        resources.getDrawable(R.drawable.ic_system_error, null)
    }

    private val wifiErrorDrawable: Drawable by lazy {
        resources.getDrawable(
            R.drawable.ic_wifi_error,
            null
        )
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val streamType = AudioManager.STREAM_MUSIC
    private val volumeObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.takeIf {
                it.action == VOLUME_CHANGED_ACTION &&
                    it.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == streamType
            }?.let {
                binding.apply {
                    val currentVolume = audioManager.getStreamVolume(streamType)
                    val maxVolume = audioManager.getStreamMaxVolume(streamType)
                    volumeLevel.text = currentVolume.toPercentValue(maxVolume)
                    volumeSeekbar.setOnSeekBarChangeListener(null)
                    volumeSeekbar.progress = currentVolume
                    volumeSeekbar.setOnSeekBarChangeListener(object :
                        SimpleOnSeekBarChangeListener() {
                        override fun onProgressChanged(progress: Int) {
                            audioManager.setStreamVolume(streamType, progress, 0)
                        }
                    })
                }
            }
        }
    }

    fun setListeners(
        activity: AppCompatActivity,
        networkMonitor: NetworkMonitor,
        batteryMonitor: BatteryMonitor,
        listener: Listener
    ) {
        this.networkMonitor = networkMonitor
        this.listener = listener
        batteryMonitor.observe(activity) {
            if (it != null) {
                updateBatteryInfo(percent = it.percent, isCharging = it.Connected)
            }
        }

        networkMonitor.networkInfo.observe(activity) {
            if (it.first != null && it.second != null) {
                updateConnectionInfo(it)
            }
        }

        networkMonitor.lastCheckNetworkConnection.observe(activity) {
            binding.systemLastCheck.text =
                it.format(DateTimeFormatter.ofPattern("MM/dd/yy HH:mm a"))
        }

        networkMonitor.cellularLevel.observe(activity) {
            val telephonyManager: TelephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (telephonyManager.simState == TelephonyManager.SIM_STATE_READY) {
                binding.cellularIcon.show()
                binding.cellularStatus.text = telephonyManager.simOperatorName
            } else {
                binding.cellularIcon.hide()
                binding.cellularStatus.text =
                    resources.getString(R.string.system_settings_cellular_not_connected)
            }
        }
    }

    init {
        setOnTouchListener { _, _ -> true }
        setupSystemSettingsView()
    }

    override fun onDetachedFromWindow() {
        context.contentResolver.unregisterContentObserver(brightnessObserver)
        context.unregisterReceiver(volumeObserver)
        super.onDetachedFromWindow()
    }

    private fun setupSystemSettingsView() {
        binding.apply {
            // WIFI
            wifiAction.paint.flags = Paint.UNDERLINE_TEXT_FLAG
            wifiAction.paint.isAntiAlias = true
            wifiContainer.setOnClickListener {
                listener?.onCloseTrayLayout()
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }

            // Brightness
            brightnessSeekbar.max = GAMMA_SPACE_MAX
            val brightnessL2G = convertLinearToGamma(getCurrentBrightness())
            brightnessSeekbar.progress = brightnessL2G
            brightnessLevel.text = brightnessL2G.toPercentValue(GAMMA_SPACE_MAX)
            brightnessSeekbar.setOnSeekBarChangeListener(object : SimpleOnSeekBarChangeListener() {
                override fun onProgressChanged(progress: Int) {
                    updateBrightness(progress)
                }
            })

            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                brightnessObserver
            )

            // Volume
            val currentVolume = audioManager.getStreamVolume(streamType)
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            volumeSeekbar.max = maxVolume
            volumeSeekbar.progress = currentVolume
            volumeLevel.text = currentVolume.toPercentValue(maxVolume)
            volumeSeekbar.setOnSeekBarChangeListener(object : SimpleOnSeekBarChangeListener() {
                override fun onProgressChanged(progress: Int) {
                    audioManager.setStreamVolume(streamType, progress, 0)
                }
            })
            context.registerReceiver(volumeObserver, IntentFilter(VOLUME_CHANGED_ACTION))

            // Admin
            adminAccessAction.paint.flags = Paint.UNDERLINE_TEXT_FLAG
            adminAccessAction.paint.isAntiAlias = true
            adminAccessAction.setOnClickListener {
                listener?.onStartAdminFlow()
            }
        }
    }

    private fun getCurrentBrightness(): Int {
        val defVal = GAMMA_SPACE_MAX / 2
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            defVal
        )
    }

    private fun updateBrightness(brightness: Int) {
        val modeAutomatic = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        val brightnessMode =
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                0
            )
        if (brightnessMode == modeAutomatic) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        }

        val brightnessG2L = BrightnessUtils.convertGammaToLinear(brightness)
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightnessG2L
        )
    }

    /**
     * Update battery info
     */
    private fun updateBatteryInfo(percent: Int, isCharging: Boolean) {
        this.powerPercent = percent
        this.powerConnected = isCharging

        val powerLevel: Int = when {
            percent >= 100 -> 100
            percent <= 15 -> 10
            else -> percent.toFloorOf(10)
        }
        val identifier = resources.getIdentifier(
            if (isCharging) "ic_battery_charging" else "ic_battery_power_$powerLevel",
            "drawable",
            context.packageName
        )
        val iconWithWhiteTint: Drawable = resources.getDrawable(identifier, null).apply {
            setTint(Color.WHITE)
        }

        // battery info on status bar
        listener?.onUpdateStatusBarBatteryInfo(iconWithWhiteTint, percent.toPercentValue(100))

        // battery info on system settings
        binding.batteryIcon.setImageDrawable(iconWithWhiteTint)
        binding.batteryPercent.text = percent.toPercentValue(100)
        binding.batteryStatus.text =
            if (isCharging) resources.getString(R.string.system_settings_plugged_in) else ""
    }

    /**
     * Update wifi & system connection info
     */
    private fun updateConnectionInfo(networkStatus: Pair<NetworkStatus?, Boolean?>?) {
        this.networkStatus = networkStatus
        networkStatus?.let {
            var wirelessDrawable = wifiDisconnectedDrawable
            val connectivityDrawable: Drawable

            when (it.first) {
                NetworkStatus.CONNECTED -> {
                    connectivityDrawable = systemConnectedWhite
                    if (it.second == true) {
                        wirelessDrawable = wifiConnectedWhite
                    }
                }

                NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE -> {
                    connectivityDrawable = systemErrorDrawable
                    if (it.second == true) {
                        wirelessDrawable = wifiConnectedWhite
                    }
                }

                NetworkStatus.CONNECTED_NO_INTERNET -> {
                    connectivityDrawable = systemErrorDrawable
                    if (it.second == true) {
                        wirelessDrawable = wifiErrorDrawable
                    }
                }

                else -> {
                    wirelessDrawable = wifiDisconnectedDrawable
                    connectivityDrawable = systemDisconnectedDrawable
                }
            }

            // wifi & system connection info on status bar
            listener?.onUpdateStatusBarNetworkInfo(connectivityDrawable, wirelessDrawable)

            // system connection info on system settings
            val vaxConnected = it.first == NetworkStatus.CONNECTED
            binding.systemConnectivityStatus.setText(
                if (vaxConnected) R.string.system_settings_connected else R.string.system_settings_not_connected
            )
        }
    }

    fun updateNetworkStatus(networkStatusShownUpdate: NetworkMonitor.NetworkStatusShownUpdate) {
        if (networkStatusShownUpdate.ssid == null) {
            binding.wifiIcon.hide()
            binding.wifiStatus.setText(R.string.system_settings_wifi_not_connected)
        } else {
            binding.wifiIcon.setImageResource(networkStatusShownUpdate.signalStrengthLevel.icon)
            binding.wifiIcon.show()
            if (networkStatusShownUpdate.ssid == WIFI_UNKNOWN_SSID) {
                Timber.d("Unknown SSID")
                binding.wifiStatus.text = context.getString(R.string.system_settings_connected)
            } else {
                Timber.d("Wifi Connected")
                binding.wifiStatus.text = networkStatusShownUpdate.ssid.trim('"')
            }
        }
    }

    fun shouldInterceptTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        return with(binding) {
            !brightnessSeekbar.isTouchPointInView(x, y) &&
                !volumeSeekbar.isTouchPointInView(x, y) &&
                !adminAccessAction.isTouchPointInView(x, y) &&
                !wifiContainer.isTouchPointInView(x, y)
        }
    }

    fun forceUpdateConnectivityIconsTint() {
        updateBatteryInfo(powerPercent, powerConnected)
        updateConnectionInfo(networkStatus)
    }

    abstract class SimpleOnSeekBarChangeListener : SeekBar.OnSeekBarChangeListener {
        abstract fun onProgressChanged(progress: Int)

        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            onProgressChanged(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    interface Listener {
        fun onStartAdminFlow()

        fun onCloseTrayLayout()

        fun onUpdateStatusBarBatteryInfo(batteryDrawable: Drawable, percent: String)

        fun onUpdateStatusBarNetworkInfo(connectivityDrawable: Drawable, wirelessDrawable: Drawable)
    }

    companion object {
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val WIFI_UNKNOWN_SSID = "<unknown ssid>"
    }
}
