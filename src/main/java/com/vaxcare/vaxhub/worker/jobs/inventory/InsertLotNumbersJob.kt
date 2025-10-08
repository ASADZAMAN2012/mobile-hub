/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.inventory

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.LotUploadFailed
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.args.InsertLotNumbersJobArgs
import com.vaxcare.vaxhub.repository.LotNumbersRepository
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - Called only when sending a new lot to the backend
 */
@Singleton
class InsertLotNumbersJob @Inject constructor(
    private val lotNumbersRepository: LotNumbersRepository,
    @MHAnalyticReport private val analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        (parameter as? InsertLotNumbersJobArgs)?.let { args ->
            if (isValidArg(args)) {
                try {
                    lotNumbersRepository.postNewLotNumber(
                        expirationDate = args.expiration!!,
                        lotNumberName = args.lotNumberName!!,
                        productId = args.epProductId!!,
                        source = args.source!!,
                        isCalledByJob = true
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                    val lotMetric = LotUploadFailed(
                        args.lotNumberName ?: "",
                        args.expiration ?: LocalDate.MIN,
                        args.epProductId ?: -1
                    )

                    analyticReport.saveMetric(lotMetric)
                }
            } else {
                Timber.e("$args InsertLotNumber invalid arguments")
            }
        }
    }

    private fun isValidArg(args: InsertLotNumbersJobArgs) =
        !args.lotNumberName.isNullOrBlank() &&
            args.epProductId != -1 &&
            (args.expiration ?: LocalDate.MIN) > LocalDate.MIN
}
