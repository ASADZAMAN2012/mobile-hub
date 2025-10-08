/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.enums.RouteCode

class SetRouteDialogClick(
    patientVisitId: Int,
    private val productName: String,
    private val lotNumber: String,
    private val route: RouteCode,
) : CheckoutMetric(patientVisitId, "SetRouteDialog.Click") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("productName", productName)
            put("lotNumber", lotNumber)
            put("routeSelected", route.displayName)
        }
    }
}
