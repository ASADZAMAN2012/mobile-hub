/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

data class OutOfDateScreenOptionSelectedMetric(
    val severity: UpdateSeverity,
    val currentVersionCode: Int,
    val buttonClicked: String
) : BaseMetric() {
    override var eventName: String = "OutOfDateScreenOptionSelected"

    override val destination: Int =
        MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().toMutableMap().apply {
            put("severity", severity.name)
            put("currentVersionCode", currentVersionCode.toString())
            put("buttonClicked", buttonClicked)
        }
    }
}
