/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination
import com.vaxcare.vaxhub.ui.checkout.dialog.PatientInfoConfirmationDialog

class PatientInfoConfirmationClickMetric(private val optionSelected: PatientInfoConfirmationDialog.Option) :
    BaseMetric() {
    override var eventName: String = "PatientInfoConfirmationDialog.Click"

    override val destination: Int = MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("selection", optionSelected.getMetricSelectionValue())
        }
    }

    private fun PatientInfoConfirmationDialog.Option.getMetricSelectionValue(): String =
        when (this) {
            PatientInfoConfirmationDialog.Option.YES -> "Yes"
            PatientInfoConfirmationDialog.Option.NO -> "No"
        }
}
