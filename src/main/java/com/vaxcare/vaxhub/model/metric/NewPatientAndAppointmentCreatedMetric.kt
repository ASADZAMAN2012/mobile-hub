/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.report.model.constant.MetricDestination

class NewPatientAndAppointmentCreatedMetric(
    val patientVisitId: Int,
    val wasForcedRiskFree: Boolean
) : BaseMetric() {
    override var eventName: String = "NewPatientAndAppointmentCreated"

    override val destination: Int = MetricDestination.APPLICATION_INSIGHTS or MetricDestination.MIX_PANEL

    override fun toMap(): Map<String, String> {
        return super.toMap().toMutableMap().apply {
            put("patientVisitId", patientVisitId.toString())
            put("wasForcedRiskFree", wasForcedRiskFree.toString())
        }
    }
}
