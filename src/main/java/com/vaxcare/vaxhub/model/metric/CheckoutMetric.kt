/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

abstract class CheckoutMetric(
    open var patientVisitId: Int?,
    checkoutEventName: String
) : BaseMetric() {
    override var eventName: String = "Checkout.$checkoutEventName"

    override val destination: Int =
        MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().toMutableMap().apply {
            put("patientVisitId", patientVisitId.toString())
        }
    }
}
