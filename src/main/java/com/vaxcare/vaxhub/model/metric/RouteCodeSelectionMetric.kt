/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class RouteCodeSelectionMetric(
    appointmentId: Int,
    private val lotNumber: String,
    private val routeSelectionName: String
) : CheckoutMetric(
        patientVisitId = appointmentId,
        checkoutEventName = "RouteCodeSelectionPresented"
    ) {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("lotNumber", lotNumber)
            put("routeSelection", routeSelectionName)
        }
    }
}
