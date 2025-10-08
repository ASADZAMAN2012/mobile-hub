/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.metric.RouteCodeSelectionMetric
import com.vaxcare.vaxhub.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RouteRequiredViewModel @Inject constructor(
    @MHAnalyticReport private val analytics: AnalyticReport
) : BaseViewModel() {
    fun saveRouteCodeSelectionMetric(
        appointmentId: Int,
        lotNumber: String,
        routeSelectionName: String
    ) {
        analytics.saveMetric(
            RouteCodeSelectionMetric(
                appointmentId = appointmentId,
                lotNumber = lotNumber,
                routeSelectionName = routeSelectionName
            )
        )
    }
}
