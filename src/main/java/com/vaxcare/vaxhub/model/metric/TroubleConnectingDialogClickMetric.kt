/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination
import com.vaxcare.vaxhub.ui.dialog.TroubleConnectingDialog

class TroubleConnectingDialogClickMetric(val optionSelected: TroubleConnectingDialog.Option) :
    BaseMetric() {
    override var eventName: String = "TroubleConnectingDialog.Click"

    override val destination: Int = MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("selection", optionSelected.getMetricSelectionValue())
        }
    }

    private fun TroubleConnectingDialog.Option.getMetricSelectionValue(): String =
        when (this) {
            TroubleConnectingDialog.Option.TRY_AGAIN -> "Try again"
            TroubleConnectingDialog.Option.OK -> "Ok"
        }
}
