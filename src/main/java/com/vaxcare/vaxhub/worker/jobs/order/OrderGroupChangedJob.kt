/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.order

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.OrdersRepository
import com.vaxcare.vaxhub.repository.OrdersRepository.Companion.SyncContextFrom.FCM
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - Called via FCM when eventType is "VaxCare.Scheduler.Partner.Clinic.OrderGroupChangedEvent"
 * for syncing orders when an order has been changed.
 */
@Singleton
class OrderGroupChangedJob @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val locationRepository: LocationRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        if (locationRepository.getFeatureFlagByConstant(FeatureFlagConstant.RightPatientRightDose) != null) {
            Timber.d("Starting OrderGroupChanged job")
            val orderGroupNumber = parameter as? String
            orderGroupNumber?.let {
                ordersRepository.syncOrdersByGroup(
                    it,
                    isCalledByJob = true,
                    syncContextFrom = FCM
                )
            }
        }
        Timber.d("OrderGroupChanged job finished")
    }
}
