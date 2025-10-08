/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.constant.MetricDestination

data class AgeWarningPromptAnsweredMetric(
    val visitId: Int,
    val promptTitle: String,
    val promptMessage: String,
    val userSelection: String
) : CheckoutMetric(
        patientVisitId = visitId,
        checkoutEventName = "AgeWarningPromptAnswered"
    ) {
    override var eventName: String = "AgeWarningPromptAnsweredMetric"
    override val destination: Int = MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().toMutableMap().apply {
            put("promptTitle", promptTitle)
            put("promptMessage", promptMessage)
            put("userSelection", userSelection)
        }
    }
}
