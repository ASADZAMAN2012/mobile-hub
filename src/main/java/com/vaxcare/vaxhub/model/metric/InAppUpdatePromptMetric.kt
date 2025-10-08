/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

class InAppUpdatePromptMetric(
    private val buttonTitlePressed: String,
    private val appVersionName: String
) : BaseMetric() {
    override var eventName: String = "InAppUpdate.Prompt.Click"

    override val destination: Int =
        MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("buttonClicked", buttonTitlePressed)
            put("versionName", appVersionName)
        }
    }
}
