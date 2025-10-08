/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.appointment

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.repository.AppointmentRepository
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called to sync appointments. 3 hour cadence
 */
@Singleton
class AppointmentJob @Inject constructor(
    private val localStorage: LocalStorage,
    private val appointmentRepository: AppointmentRepository,
    @MHAnalyticReport analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("Getting today's Appointments...")
        val today = LocalDate.now()
        val lastAppointmentSyncDateString = localStorage.lastAppointmentSyncDate
        val shouldSync = lastAppointmentSyncDateString == null || today.isAfter(
            (LocalDate.parse(lastAppointmentSyncDateString))
        )

        if (shouldSync) {
            appointmentRepository.syncAppointmentsAndSave(isCalledByJob = true)
        }
    }
}
