/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.reporting

import com.vaxcare.core.report.analytics.MetricDecorator
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.storage.preference.UserSessionManager
import com.vaxcare.vaxhub.service.NetworkMonitor
import javax.inject.Inject

class MetricDecoratorImpl @Inject constructor(
    private val localStorage: LocalStorage,
    private val userSessionManager: UserSessionManager,
    private val networkMonitor: NetworkMonitor
) : MetricDecorator {
    override fun decorateMetrics(metrics: List<BaseMetric>): List<BaseMetric> {
        val partnerId = localStorage.partnerId
        val clinicId = localStorage.clinicId
        val userId = localStorage.userId
        val serialNumber = localStorage.deviceSerialNumber
        val userSessionId = userSessionManager.getCurrentUserSessionId()
        val wifiSignal = networkMonitor.networkProperties.value?.dBmSignal
        val cellSignal = networkMonitor.cellularLevel.value
        val upSpeed = networkMonitor.networkProperties.value?.upSpeed
        val downSpeed = networkMonitor.networkProperties.value?.downSpeed

        return metrics.map { metric ->
            metric.apply {
                this.partnerId = partnerId
                this.clinicId = clinicId
                this.userId = userId
                this.serialNumber = serialNumber
                this.userSessionId = userSessionId
                this.wifiSignal = wifiSignal
                this.cellSignal = cellSignal
                this.upSpeed = upSpeed
                this.downSpeed = downSpeed
            }
        }
    }
}
