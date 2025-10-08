/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.LocationRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - Called daily or via FCM when eventType is
 * "VaxHub.FirebaseEvents.SyncLocation" for syncing locationData from backend. 24 hour cadence
 */
@Singleton
class LocationJob @Inject constructor(
    private val locationRepository: LocationRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("LocationRefresh started")
        locationRepository.refreshLocation(isCalledByJob = true)
        Timber.d("LocationRefresh completed")
    }
}
