/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.order

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.OrdersRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes all expired orders. 24 hour cadence
 */
@Singleton
class OrderCleanupJob @Inject constructor(
    private val orderRepository: OrdersRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("Starting Orders cleanup job...")
        orderRepository.removeExpiredOrders()
        Timber.d("Orders cleanup job finished")
    }
}
