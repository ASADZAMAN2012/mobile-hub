/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.appointment

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.ShotAdministratorRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called for syncing Shot Administrators. 24 hour cadence
 */
@Singleton
class ShotAdministratorJob @Inject constructor(
    private val shotAdministratorRepository: ShotAdministratorRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        shotAdministratorRepository.refreshShotAdministrators(isCalledByJob = true)
    }
}
