/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.hub

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called to sync all users. 24 hour cadence
 */
@Singleton
class UserJob @Inject constructor(
    private val userRepository: UserRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        userRepository.forceSyncUsers(isCalledByJob = true)
    }
}
