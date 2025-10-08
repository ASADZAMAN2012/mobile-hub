/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.ProviderRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called for syncing Providers. 24 hour cadence
 */
@Singleton
class ProviderJob @Inject constructor(
    private val providerRepository: ProviderRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("Starting Provider work...")
        providerRepository.syncProviders(isCalledByJob = true)
        Timber.d("Finished Provider work...")
    }
}
