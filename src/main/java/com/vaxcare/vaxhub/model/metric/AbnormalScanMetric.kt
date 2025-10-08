/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

class AbnormalScanMetric(
    val barcode: String,
    val abnormality: String
) : BaseMetric() {
    override var eventName: String = "AbnormalScanMetric"
    override val destination: Int = MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("barcode", barcode)
            put("abnormality", abnormality)
        }
    }
}
