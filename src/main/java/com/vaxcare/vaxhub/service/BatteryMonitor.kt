/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BATTERY_CHANGED
import android.content.IntentFilter
import android.os.BatteryManager.EXTRA_LEVEL
import android.os.BatteryManager.EXTRA_PLUGGED
import android.os.BatteryManager.EXTRA_SCALE
import android.os.PowerManager
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : LiveData<BatteryStatus?>() {
    /**
     * The battery broadcast receiver.
     */
    private var batteryReceiver: BroadcastReceiver? = null

    private val powerManager: PowerManager?
        get() = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)

    companion object {
        const val UNKNOWN = -1
    }

    override fun onActive() {
        super.onActive()
        registerBatteryReceiver()
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiver == null) {
            batteryReceiver = BatteryReceiver()
        }
        val intentFilter = IntentFilter(ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, intentFilter)
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(batteryReceiver)
    }

    /**
     * The battery broadcast receiver.
     */
    private inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = setBatteryInfo(intent)
    }

    private fun setBatteryInfo(intent: Intent) {
        val plugged =
            intent.getIntExtra(EXTRA_PLUGGED, UNKNOWN)
        val level =
            intent.getIntExtra(EXTRA_LEVEL, UNKNOWN)
        val scale =
            intent.getIntExtra(EXTRA_SCALE, UNKNOWN)
        val isPowerSavingMode = powerManager?.isPowerSaveMode ?: false
        // Where an int value of 0 is on battery power
        value = BatteryStatus(
            percent = (level * 100 / scale.toFloat()).toInt(),
            Connected = plugged != 0,
            isOnPowerSavingMode = isPowerSavingMode
        )
    }
}

data class BatteryStatus(
    val percent: Int,
    val Connected: Boolean,
    val isOnPowerSavingMode: Boolean
)
