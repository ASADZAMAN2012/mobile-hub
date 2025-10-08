/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.worker

import android.content.Context
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes.COPAY_RESULTS
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes.SIMPLE_ON_HAND
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes.SYNC_APPOINTMENT
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes.SYNC_DIAGNOSTIC
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes.SYNC_GROUP_ORM
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes.SYNC_LOCATION
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.di.args.AppointmentChangedJobArgs
import com.vaxcare.vaxhub.di.args.PartDJobArgs
import com.vaxcare.vaxhub.model.AppointmentChangedEvent
import com.vaxcare.vaxhub.model.FirebaseClinicEvent
import com.vaxcare.vaxhub.model.firebase.OrderGroupChangedEvent
import com.vaxcare.vaxhub.model.partd.PartDEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class used to create a worker and queue it up based on the event type
 */
@Singleton
class JobSelector @Inject constructor(
    @ApplicationContext context: Context,
    private val localStorage: LocalStorage,
    @MobileMoshi private val moshi: Moshi,
    private val listener: HiltWorkManagerListener
) {
    private val wm: WorkManager by lazy { WorkManager.getInstance(context.applicationContext) }

    /**
     * Queues up a worker based on the [eventType]
     *
     * @param eventType The type of event
     * @param payload Optional payload, currently used for Appointment Change Events
     */
    fun queueJob(eventType: String, payload: String? = null) {
        val sameClinic = try {
            moshi
                .adapter(FirebaseClinicEvent::class.java)
                .fromJson(payload ?: "").isSameClinic()
        } catch (e: Exception) {
            Timber.e(e, "There was a problem deserializing payload to clinic event")
            true
        }

        if (sameClinic) {
            when (eventType) {
                SYNC_LOCATION -> executeLocationJob()
                SYNC_GROUP_ORM -> executeOrderGroupChangedJob(payload)
                SYNC_APPOINTMENT -> executeAppointmentChangedJob(payload)
                SIMPLE_ON_HAND -> executeSimpleOnHandInventoryJob()
                SYNC_DIAGNOSTIC -> executeDiagnosticJob()
                COPAY_RESULTS -> executePartDJob(payload)
            }
        }
    }

    private fun executeDiagnosticJob() {
        OneTimeWorker.buildOneTimeUniqueWorker(
            wm = wm,
            parameters = OneTimeParams.DiagnosticJob,
            listener = listener
        )
    }

    private fun executeLocationJob() {
        OneTimeWorker.buildOneTimeUniqueWorker(
            wm = wm,
            parameters = OneTimeParams.Location,
            listener = listener
        )
    }

    private fun executeAppointmentChangedJob(payload: String?) {
        if (!payload.isNullOrEmpty()) {
            val adapter = moshi.adapter(AppointmentChangedEvent::class.java)
            val event = adapter.fromJson(payload)
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = wm,
                parameters = OneTimeParams.AppointmentChanged(
                    params = AppointmentChangedJobArgs(
                        changeReason = event?.changeReason,
                        changeType = event?.changeType,
                        appointmentId = event?.appointmentId
                    )
                ),
                listener = listener
            )
        }
    }

    private fun executeOrderGroupChangedJob(payload: String?) {
        if (!payload.isNullOrEmpty()) {
            val adapter = moshi.adapter(OrderGroupChangedEvent::class.java)
            val event = adapter.fromJson(payload)

            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = wm,
                parameters = OneTimeParams.OrderGroupChanged(
                    orderGroupNumber = event?.orderGroupNumber ?: ""
                ),
                listener = listener
            )
        }
    }

    private fun executeSimpleOnHandInventoryJob() {
        OneTimeWorker.buildOneTimeUniqueWorker(
            wm = wm,
            parameters = OneTimeParams.SimpleOnHand,
            listener = listener
        )
    }

    private fun executePartDJob(payload: String?) {
        if (!payload.isNullOrEmpty()) {
            val adapter = moshi.adapter(PartDEvent::class.java)
            val event = adapter.fromJson(payload)
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = wm,
                parameters = OneTimeParams.PartD(
                    params = PartDJobArgs(
                        patientVisitId = event?.patientVisitId,
                        copays = event?.copays ?: emptyList(),
                        moshi = moshi
                    )
                ),
                listener = listener
            )
        }
    }

    private fun FirebaseClinicEvent?.isSameClinic(): Boolean =
        this?.let { event ->
            event.parentClinicId?.toLong() == localStorage.clinicId &&
                event.clinicId?.toLong() == localStorage.currentClinicId
        } ?: true
}
