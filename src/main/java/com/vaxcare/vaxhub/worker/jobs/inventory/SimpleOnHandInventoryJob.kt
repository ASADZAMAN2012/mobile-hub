/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.inventory

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.core.extension.isFeaturePublicStockPilotEnabled
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepository
import com.vaxcare.vaxhub.worker.jobs.VaxJobCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleOnHandInventoryJob @Inject constructor(
    private val simpleOnHandInventoryRepository: SimpleOnHandInventoryRepository,
    private val locationRepository: LocationRepository,
    private val callback: VaxJobCallback,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        val callMade = locationRepository.getLocationAsync()?.let { location ->
            val shouldRun = location.inventorySources.size > 1 &&
                location.activeFeatureFlags.isFeaturePublicStockPilotEnabled()
            if (shouldRun) {
                simpleOnHandInventoryRepository.fetchAndSaveSimpleOnHandInventory(isCalledByJob = true)
            }

            shouldRun
        } ?: false

        callback.onJobFinished(jobName = this.javaClass.simpleName, success = callMade)
    }
}
