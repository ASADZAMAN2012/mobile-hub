/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.CallToAction

// TODO: map EncounterMessage with the parent EncounterState id rather than the AppointmentId. This
// is currently not implemented because the id is still null/0 before insertion and therefore not
// mapped. Solution was to just map to appointmentId

/**
 * Interface to dictate what properties should exist for an EncounterMessage object
 * We should receive at most one EncounterMessage per AppointmentServiceType (Vaccine, MedD, Larc, etc.)
 *
 * @property riskAssessmentId riskAssessment identifier
 * @property appointmentId id of the associated appointment
 * @property status AppointmentStatus value
 * @property icon AppointmentIcon value
 * @property primaryMessage Appointment primary message
 * @property secondaryMessage Appointment secondary message
 * @property callToAction Appointment CallToAction value
 * @property topRejectCode reject code value (for medD)
 * @property serviceType AppointmentServiceType value
 */
interface EncounterMessage {
    val riskAssessmentId: Int
    val appointmentId: Int
    val status: AppointmentStatus
    val icon: AppointmentIcon?
    val primaryMessage: String?
    val secondaryMessage: String?
    var callToAction: CallToAction?
    val topRejectCode: String?
    val serviceType: AppointmentServiceType
}

/**
 * Json representation of EntityMessage object for moshi interaction
 *
 * @property riskAssessmentId riskAssessment identifier
 * @property appointmentId id of the associated appointment
 * @property status AppointmentStatus value
 * @property icon AppointmentIcon value
 * @property primaryMessage Appointment primary message
 * @property secondaryMessage Appointment secondary message
 * @property callToAction Appointment CallToAction value
 * @property topRejectCode reject code value (for medD)
 * @property serviceType AppointmentServiceType value
 */
data class EncounterMessageJson(
    @Json(name = "riskAssessmentId") @PrimaryKey override val riskAssessmentId: Int,
    @Json(ignore = true) override var appointmentId: Int = 0,
    @Json(name = "status") override val status: AppointmentStatus,
    @Json(name = "icon") override val icon: AppointmentIcon?,
    @Json(name = "primaryMessage") override val primaryMessage: String?,
    @Json(name = "secondaryMessage") override val secondaryMessage: String?,
    @Json(name = "callToAction") override var callToAction: CallToAction?,
    @Json(name = "topRejectCode") override val topRejectCode: String?,
    @Json(name = "serviceType") override val serviceType: AppointmentServiceType
) : EncounterMessage

/**
 * Entity representation of EntityMessage object for room interaction
 *
 * @property riskAssessmentId riskAssessment identifier
 * @property appointmentId id of the associated appointment
 * @property status AppointmentStatus value
 * @property icon AppointmentIcon value
 * @property primaryMessage Appointment primary message
 * @property secondaryMessage Appointment secondary message
 * @property callToAction Appointment CallToAction value
 * @property topRejectCode reject code value (for medD)
 * @property serviceType AppointmentServiceType value
 */
@Entity(tableName = "EncounterMessage", primaryKeys = ["riskAssessmentId", "serviceType"])
data class EncounterMessageEntity(
    override val riskAssessmentId: Int,
    override var appointmentId: Int,
    override val status: AppointmentStatus,
    override val icon: AppointmentIcon?,
    override val primaryMessage: String?,
    override val secondaryMessage: String?,
    override var callToAction: CallToAction?,
    override val topRejectCode: String?,
    override val serviceType: AppointmentServiceType
) : EncounterMessage {
    fun getIconRes(): Int? =
        when (icon) {
            AppointmentIcon.STAR -> R.drawable.ic_vax3_eligibility_guaranteed_ic
            AppointmentIcon.DOLLAR -> R.drawable.ic_vax3_eligibility_self_pay
            AppointmentIcon.DOTTED_CIRCLE -> R.drawable.ic_vax3_eligibility_pending
            AppointmentIcon.FULL_CIRCLE -> R.drawable.ic_vax3_eligibility_partner_resp_ic
            AppointmentIcon.HALF_CIRCLE -> R.drawable.ic_vax3_eligibility_issue_ic
            else -> null
        }

    companion object {
        fun fromEncounterMessage(encounterMessages: List<EncounterMessage>) =
            encounterMessages.map {
                EncounterMessageEntity(
                    riskAssessmentId = it.riskAssessmentId,
                    appointmentId = it.appointmentId,
                    status = it.status,
                    icon = it.icon,
                    primaryMessage = it.primaryMessage,
                    secondaryMessage = it.secondaryMessage,
                    callToAction = it.callToAction,
                    topRejectCode = it.topRejectCode,
                    serviceType = it.serviceType
                )
            }
    }
}
