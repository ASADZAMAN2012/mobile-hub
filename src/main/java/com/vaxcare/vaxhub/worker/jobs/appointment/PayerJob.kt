/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.appointment

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PayerRepository
import com.vaxcare.vaxhub.web.PatientsApi
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called for syncing all contracted payers based on state. 24 hour cadence
 */
@Singleton
class PayerJob @Inject constructor(
    private val patientsApi: PatientsApi,
    private val payerRepository: PayerRepository,
    private val locationRepository: LocationRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        val payers = patientsApi.getPayers(
            isCalledByJob = true,
            state = locationRepository.getClinicState(),
            contractedOnly = false
        )

        // The used records we use are already stored in our database
        val recentThreePayers: List<Payer> =
            payerRepository.getThreeMostRecentPayersAsync() ?: emptyList()
        if (recentThreePayers.isNotEmpty()) {
            payers.forEach { payer ->
                recentThreePayers.forEach { recentPayer ->
                    if (payer.insuranceId == recentPayer.insuranceId &&
                        payer.insuranceName == recentPayer.insuranceName
                    ) {
                        payer.updatedTime = recentPayer.updatedTime
                    }
                }
            }
        }

        payerRepository.deleteAll()
        Timber.d("Insert payer size: ${payers.size}")
        payerRepository.insertPayers(payers)
    }
}
