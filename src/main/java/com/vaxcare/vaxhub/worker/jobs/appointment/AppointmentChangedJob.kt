/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker.jobs.appointment

import android.content.Context
import android.content.Intent
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.worker.vaxjob.BaseVaxJob
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.args.AppointmentChangedJobArgs
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.model.metric.AceReceivedMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active job - Called via FCM when event type is
 * "VaxCare.Scheduler.Partner.Clinic.AppointmentChangedEvent"
 */
@Singleton
class AppointmentChangedJob @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    @MHAnalyticReport val analyticReport: AnalyticReport
) : BaseVaxJob(analyticReport) {
    override suspend fun doWork(parameter: Any?) {
        Timber.d("AppointmentChangedEventWorker started: args: ${parameter as? AppointmentChangedJobArgs}")

        (parameter as? AppointmentChangedJobArgs)?.let { args ->
            when (args.changeType) {
                AppointmentChangeType.Created -> Unit
                AppointmentChangeType.Updated -> updateAppointmentWithChangeReason(args)
                AppointmentChangeType.Deleted -> deleteAppointment(args)
                else -> Timber.d(
                    "Unknown AppointmentChangedEvent received for appointmentId %s",
                    args.appointmentId
                )
            }
        }

        Timber.d("AppointmentChangedEventWorker completed")
    }

    private suspend fun deleteAppointment(args: AppointmentChangedJobArgs) {
        args.appointmentId?.let {
            appointmentRepository.deleteAppointmentByIds(listOf(it))
            args.context?.sendAppointmentChangedEventBroadcast(
                appointmentId = it,
                changeReason = args.changeReason,
                changeType = args.changeType
            )
        }
    }

    private suspend fun updateAppointmentWithChangeReason(args: AppointmentChangedJobArgs) {
        val appointmentId = args.appointmentId ?: -1

        when (args.changeReason) {
            AppointmentChangeReason.CheckoutCompleted,
            AppointmentChangeReason.RiskUpdated -> try {
                appointmentRepository.updateAppointmentDetailsById(appointmentId, true)
                args.context?.sendAppointmentChangedEventBroadcast(
                    appointmentId = appointmentId,
                    changeReason = args.changeReason,
                    changeType = args.changeType
                )
            } catch (e: Exception) {
                Timber.e(e)
                throw e
            }

            AppointmentChangeReason.MedDCompleted,
            AppointmentChangeReason.MedDError -> args.context?.sendAppointmentChangedEventBroadcast(
                appointmentId = appointmentId,
                changeReason = args.changeReason,
                changeType = args.changeType
            ).also {
                analyticReport.saveMetric(AceReceivedMetric(args.changeReason, args.changeType, appointmentId))
            }

            else -> Timber.d(
                "Ignored AppointmentChangeReason ${args.changeReason} received for appointmentId $appointmentId"
            )
        }
    }

    private fun Context.sendAppointmentChangedEventBroadcast(
        appointmentId: Int,
        changeReason: AppointmentChangeReason?,
        changeType: AppointmentChangeType?
    ) {
        Timber.d("Sending Broadcast for id $appointmentId | $changeReason")
        sendBroadcast(
            Intent(Receivers.ACE_ACTION).apply {
                val (changeReasonOrdinal, changeTypeOrdinal) =
                    (changeReason?.ordinal ?: AppointmentChangeReason.Unknown.ordinal) to
                        (changeType?.ordinal ?: AppointmentChangeType.Unknown.ordinal)

                putExtra(Receivers.ACE_APPOINTMENT_ID, appointmentId)
                putExtra(Receivers.ACE_CHANGE_REASON, changeReasonOrdinal)
                putExtra(Receivers.ACE_CHANGE_TYPE, changeTypeOrdinal)
            }
        )
    }
}
