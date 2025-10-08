/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class CheckoutDoseClickMetric(
    patientVisitId: Int,
    private val productName: String,
    private val lotNumber: String,
    private val click: Click
) : CheckoutMetric(patientVisitId, "Dose.Click") {
    enum class Click(val description: String) {
        SET_SITE("Set Site"),
        SET_ROUTE("Set Route")
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("productName", productName)
            put("lotNumber", lotNumber)
            put("click", click.description)
        }
    }
}
