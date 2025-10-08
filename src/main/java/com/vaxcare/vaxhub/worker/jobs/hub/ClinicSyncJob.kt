/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.ClinicRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called for syncing all clinics. 24 hour cadence
 */
@Singleton
class ClinicSyncJob @Inject constructor(
    private val clinicRepository: ClinicRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("Syncing clinics...")
        clinicRepository.syncClinics(isCalledByJob = true)
    }
}
