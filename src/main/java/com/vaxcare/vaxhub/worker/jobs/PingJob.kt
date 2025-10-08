/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.SessionUserRepository
import javax.inject.Inject

class PingJob @Inject constructor(
    private val sessionUserRepository: SessionUserRepository,
    @MHAnalyticReport report: AnalyticReport
) : BaseVaxJob(report) {
    companion object {
        const val PING_JOB_NAME = "pingjob"
    }

    override suspend fun doWork(parameter: Any?) {
        sessionUserRepository.pingVaxCareServer()
    }
}
