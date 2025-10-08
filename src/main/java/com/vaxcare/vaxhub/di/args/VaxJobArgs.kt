/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.di.args

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.model.partd.PartDCopayEvent
import com.vaxcare.vaxhub.worker.jobs.hub.VaxCareConfigResult
import java.time.LocalDate

/**
 * Arguments for AppointmentChangedJob
 * @see com.vaxcare.vaxhub.worker.jobs.appointment.AppointmentChangedJob
 * @see com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
 * @see com.vaxcare.vaxhub.model.enums.AppointmentChangeType
 *
 * @property appointmentId appointment Id for the changed appointment
 * @property changeType change type to convert to ordinal
 * @property changeReason change reason to convert to ordinal
 * @property context context for sending broadcast
 */
data class AppointmentChangedJobArgs(
    val appointmentId: Int? = null,
    val changeType: AppointmentChangeType? = null,
    val changeReason: AppointmentChangeReason? = null,
    val context: Context? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "ChangeReason" to (changeReason?.ordinal ?: -1),
            "ChangeType" to (changeType?.ordinal ?: -1),
            "AppointmentId" to (appointmentId ?: -1)
        )
    }
}

/**
 * Arguments for InsertLotNumbersJob
 * @see com.vaxcare.vaxhub.worker.jobs.inventory.InsertLotNumbersJob
 * @see com.vaxcare.vaxhub.model.enums.LotNumberSources
 *
 * @property lotNumberName name of new lot
 * @property epProductId associated product Id of new lot
 * @property expiration expiration date of new lot
 * @property source source of the lot insertion
 */
data class InsertLotNumbersJobArgs(
    val lotNumberName: String? = null,
    val epProductId: Int? = null,
    val expiration: LocalDate? = null,
    val source: Int? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "LotNumberName" to (lotNumberName ?: ""),
            "productId" to (epProductId?.toString() ?: ""),
            "expiration" to (expiration?.toString() ?: ""),
            "source" to (source?.toString() ?: "")
        )
    }

    override fun toString(): String = "$lotNumberName | $epProductId | $expiration"
}

/**
 * Arguments for PartDJob
 * @see com.vaxcare.vaxhub.worker.jobs.appointment.PartDJob
 * @see com.vaxcare.vaxhub.model.event.PartDEvent
 * @see com.vaxcare.vaxhub.model.event.PartDCopayEvent
 *
 * @property patientVisitId appointment Id for the appointment
 * @property copays list of resulting copays
 */
data class PartDJobArgs(
    val patientVisitId: Int? = null,
    val copays: List<PartDCopayEvent> = emptyList(),
    val context: Context? = null,
    private val moshi: Moshi? = null
) {
    fun toMap(): Map<String, Any> {
        val typeList = Types.newParameterizedType(MutableList::class.java, PartDCopayEvent::class.java)
        val resolvedCopays = moshi?.adapter<List<PartDCopayEvent>>(typeList)?.toJson(copays) ?: ""
        return mapOf(
            "Copays" to resolvedCopays,
            "PatientVisitId" to (patientVisitId ?: -1)
        )
    }
}

/**
 * Arguments for VaxCareConfigJob
 *
 * @property completeCallback callback taking VaxCareConfigResult wrapper object
 */
fun interface VaxCareConfigJobArgs {
    fun completeCallback(result: VaxCareConfigResult)
}
