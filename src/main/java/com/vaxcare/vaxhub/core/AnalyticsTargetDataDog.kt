/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core

import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.RumActionType
import com.vaxcare.core.report.analytics.AnalyticsTarget
import com.vaxcare.core.report.model.BaseMetric
import javax.inject.Inject

class AnalyticsTargetDataDog @Inject constructor() : AnalyticsTarget {
    override suspend fun saveMetric(metrics: List<BaseMetric>) {
        metrics.forEach { metric ->
            GlobalRumMonitor.get().addAction(
                type = RumActionType.CUSTOM,
                name = metric.eventName,
                attributes = metric.toMap()
            )
        }
    }

    override fun configure(identification: String) = Unit

    override suspend fun updatePartnerData(partnerId: Long, clinicId: Long) = Unit

    override suspend fun updateUserData(
        userId: Int,
        userName: String,
        fullName: String
    ) = Unit

    override suspend fun flush() = Unit
}
