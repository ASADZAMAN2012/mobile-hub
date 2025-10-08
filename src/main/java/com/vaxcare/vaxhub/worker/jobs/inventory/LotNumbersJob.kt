/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.inventory

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.LotNumbersRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - for resyncing LotNumbers from backend. 24 hour cadence
 */
@Singleton
class LotNumbersJob @Inject constructor(
    private val lotNumbersRepository: LotNumbersRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        lotNumbersRepository.syncLotNumbers(isCalledByJob = true)
    }
}
