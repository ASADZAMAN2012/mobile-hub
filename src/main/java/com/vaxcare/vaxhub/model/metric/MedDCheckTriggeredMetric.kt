/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.core.model.enums.MedDIDType

class MedDCheckTriggeredMetric(
    patientVisitId: Int,
    private val medDIDType: MedDIDType?,
    private val triggeredMode: TriggeredMode
) : CheckoutMetric(patientVisitId = patientVisitId, checkoutEventName = METRIC_NAME) {
    companion object {
        private const val METRIC_NAME = "MedD.CheckTriggered"
        private const val PATIENT_ID_TYPE_USED = "patientIdType"
        private const val TRIGGERED_MODE = "triggeredMode"
    }

    enum class TriggeredMode {
        MANUALLY,
        AUTO_RUN,
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put(TRIGGERED_MODE, triggeredMode.toString())
            put(PATIENT_ID_TYPE_USED, medDIDType.toString())
        }
    }
}
