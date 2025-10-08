/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.ndc

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.WrongProductRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WrongProductNdcJob @Inject constructor(
    private val wrongProductRepository: WrongProductRepository,
    @MHAnalyticReport analyticReport: AnalyticReport,
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        wrongProductRepository.getAndUpsertWrongProductNdcs(isCalledByJob = true)
    }
}
