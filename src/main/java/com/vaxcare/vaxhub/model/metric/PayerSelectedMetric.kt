/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

data class PayerSelectedMetric(
    val selectedPayer: String,
    val selectedPayerId: String?,
    val selectedPlanId: String?
) : BaseMetric() {
    companion object {
        private const val SELECTED_PAYER = "selectedPayer"
        private const val SELECTED_PAYER_ID = "selectedPayerId"
        private const val SELECTED_PLAN_ID = "selectedInsurancePlanId"
    }

    override var eventName: String = "PayerSelected"
    override val destination: Int =
        MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put(SELECTED_PAYER, selectedPayer)
            put(SELECTED_PAYER_ID, selectedPayerId ?: "N/A")
            put(SELECTED_PLAN_ID, selectedPlanId ?: "N/A")
        }
    }
}
