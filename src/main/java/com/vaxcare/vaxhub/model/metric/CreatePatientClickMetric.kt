/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

class CreatePatientClickMetric(
    val isMBIAvailable: Boolean,
    val isSSNAvailable: Boolean,
) : BaseMetric() {
    override var eventName: String = "CreatePatient.Click"

    override val destination: Int = MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("isMBIAvailable", isMBIAvailable.toString())
            put("isSSNAvailable", isSSNAvailable.toString())
        }
    }
}
