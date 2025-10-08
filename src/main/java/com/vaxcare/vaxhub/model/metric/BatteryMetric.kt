/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

data class BatteryMetric(
    val percent: Int,
    val isConnected: Boolean,
    val powerSavingMode: Boolean? = null
) : BaseMetric() {
    override val destination: Int =
        MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    private val powerSavingModeString = when (powerSavingMode) {
        true -> "On"
        false -> "Off"
        else -> "Unknown"
    }

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("percentage", "$percent%")
            put("pluggedIn", "$isConnected")
            put("powerSavingMode", powerSavingModeString)
        }
    }
}
