/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.inventory

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.ProductRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called for syncing products and LotInventory from backend. 24 hour cadence
 */
@Singleton
class ProductJob @Inject constructor(
    private val productRepository: ProductRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        productRepository.syncAllProducts(isCalledByJob = true)
    }
}
