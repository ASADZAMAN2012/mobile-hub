/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.FirewallCheckRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FireWallCheckJob
    @Inject
    constructor(
        private val firewallCheckRepository: FirewallCheckRepository,
        @MHAnalyticReport report: AnalyticReport,
    ) : BaseVaxJob(report) {
        override suspend fun doWork(parameter: Any?) {
            firewallCheckRepository.pingServices()
        }
    }
