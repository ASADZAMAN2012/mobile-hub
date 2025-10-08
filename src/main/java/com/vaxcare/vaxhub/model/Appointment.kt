/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.squareup.moshi.Json
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.core.report.model.checkout.RelativeDoS
import com.vaxcare.vaxhub.core.annotation.LocalTime
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateJson
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ShotStatus
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.model.patient.PayerField
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * AppointmentDto serves as the return type for syncAppointments() and getClinicAppointmentsByDate().
 * Use the toAppointment() extension to transform it into an Appointment before storing it
 * in the database via the AppointmentDao
 *
 * @property id
 * @property clinicId
 * @property appointmentTime
 * @property patient
 * @property paymentType
 * @property paymentMode
 * @property stock
 * @property visitType
 * @property checkedOut
 * @property checkedOutTime
 * @property provider
 * @property administeredBy
 * @property encounterState
 * @property administeredVaccines
 */
data class AppointmentDto(
    val id: Int,
    val clinicId: Int,
    val appointmentTime: LocalDateTime,
    val patient: Patient,
    val paymentType: String?,
    @Json(name = "paymentMode") val paymentMethod: PaymentMethod,
    val stock: String,
    val visitType: String,
    val checkedOut: Boolean,
    @LocalTime val checkedOutTime: LocalDateTime?,
    val provider: Provider,
    val administeredBy: Int?,
    val isEditable: Boolean?,
    @Ignore val encounterState: EncounterStateJson?,
    val administeredVaccines: List<AdministeredVaccine>?
) {
    /**
     * Used by the AppointmentDao in order to properly
     * store data for an Appointment
     */
    fun toAppointmentData() =
        AppointmentData(
            id = id,
            clinicId = clinicId,
            vaccineSupply = stock,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            isProcessing = false
        )

    /**
     * Used to transform an object into a fully qualified Appointment
     */
    fun toAppointment() =
        Appointment(
            id = id,
            clinicId = clinicId,
            vaccineSupply = stock,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            encounterState = EncounterStateEntity.fromEncounterState(encounterState),
            administeredVaccines = administeredVaccines ?: listOf(),
            isProcessing = false,
            orders = listOf()
        )
}

/**
 * AppointmentDetailDto serves as the return type for getAppointmentById(). Use the
 * toAppointment() extension to transform it into an Appointment before storing it
 * in the database via the AppointmentDao
 *
 * @property id
 * @property clinicId
 * @property appointmentTime
 * @property patient
 * @property paymentType
 * @property paymentMethod
 * @property stock
 * @property visitType
 * @property checkedOut
 * @property checkedOutTime
 * @property provider
 * @property administeredBy
 * @property encounterState
 * @property administeredVaccines
 */
data class AppointmentDetailDto(
    val id: Int,
    val clinicId: Int,
    val appointmentTime: LocalDateTime,
    val patient: Patient,
    val paymentType: String?,
    @Json(name = "paymentMode") val paymentMethod: PaymentMethod,
    val stock: String,
    val visitType: String,
    val checkedOut: Boolean,
    @LocalTime val checkedOutTime: LocalDateTime?,
    val provider: Provider,
    val administeredBy: Int?,
    val isEditable: Boolean?,
    @Ignore val encounterState: EncounterStateJson,
    val administeredVaccines: List<AdministeredVaccine>?
) {
    fun toAppointmentDto(): AppointmentDto {
        return AppointmentDto(
            id = id,
            clinicId = clinicId,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            stock = stock,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            encounterState = null,
            administeredVaccines = administeredVaccines
        )
    }

    /**
     * Used by the AppointmentDao in order to properly
     * store data for an Appointment
     */
    fun toAppointmentData() =
        AppointmentData(
            id = id,
            clinicId = clinicId,
            vaccineSupply = stock,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            isProcessing = false
        )

    /**
     * Used to transform an object into a fully qualified Appointment
     */
    fun toAppointment() =
        Appointment(
            id = id,
            clinicId = clinicId,
            vaccineSupply = stock,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            encounterState = EncounterStateEntity.fromEncounterState(encounterState),
            administeredVaccines = administeredVaccines ?: listOf(),
            isProcessing = false,
            orders = listOf()
        )
}

/**
 * AppointmentData serves as the database representation of an Appointment and should only
 * be used by the AppointmentDao's protected insertAppointments and deleteAppointments function
 *
 * @property id
 * @property clinicId
 * @property vaccineSupply
 * @property appointmentTime
 * @property patient
 * @property paymentType
 * @property paymentMethod
 * @property visitType
 * @property checkedOut
 * @property checkedOutTime
 * @property provider
 * @property administeredBy
 */
@Entity
data class AppointmentData(
    @PrimaryKey val id: Int,
    val clinicId: Int,
    val vaccineSupply: String,
    val appointmentTime: LocalDateTime,
    @Embedded(prefix = "patient_") val patient: Patient,
    val paymentType: String?,
    val paymentMethod: PaymentMethod,
    val visitType: String,
    val checkedOut: Boolean,
    @LocalTime val checkedOutTime: LocalDateTime?,
    @Embedded(prefix = "provider_") val provider: Provider,
    val administeredBy: Int?,
    val isEditable: Boolean?,
    val isProcessing: Boolean = false
)

/**
 * Appointment serves as a fully qualified Appointment. It contains all the data found in
 * AppointmentData as well as all the EncounterState Messages and AdministeredVaccines associated with an
 * Appointment. This class should be used when working with Appointments throughout the application
 * and when interacting with the AppointmentDao to create, update, and delete Appointments.
 *
 * @property id
 * @property clinicId
 * @property vaccineSupply
 * @property appointmentTime
 * @property patient
 * @property paymentType
 * @property paymentMethod
 * @property visitType
 * @property checkedOut
 * @property checkedOutTime
 * @property provider
 * @property administeredBy
 * @property administeredVaccines
 */
data class Appointment(
    val id: Int,
    val clinicId: Int,
    val vaccineSupply: String,
    val appointmentTime: LocalDateTime,
    @Embedded(prefix = "patient_") val patient: Patient,
    val paymentType: String?,
    val paymentMethod: PaymentMethod,
    val visitType: String,
    val checkedOut: Boolean,
    @LocalTime val checkedOutTime: LocalDateTime?,
    @Embedded(prefix = "provider_") val provider: Provider,
    val administeredBy: Int?,
    val isEditable: Boolean?,
    @Relation(parentColumn = "id", entityColumn = "appointmentId")
    var encounterState: EncounterStateEntity?,
    @Relation(parentColumn = "id", entityColumn = "appointmentId")
    val administeredVaccines: List<AdministeredVaccine>,
    val isProcessing: Boolean?,
    @Relation(parentColumn = "patient_id", entityColumn = "patientId")
    var orders: List<OrderEntity>
) {
    @Ignore
    val nonExpiredOrdersCount: Int = with(LocalDateTime.now()) {

        if (checkedOut) {
            // count unlinked and linked orders
            orders.count {
                it.expirationDate.isAfter(this) && it.patientVisitId == null || it.patientVisitId == id
            }
        } else {
            // count only unlinked orders
            orders.count {
                it.expirationDate.isAfter(this) && it.patientVisitId == null
            }
        }
    }

    fun isPrivate(): Boolean {
        return vaccineSupply.isNotEmpty() &&
            vaccineSupply.lowercase() == "private"
    }

    fun isVFC(): Boolean {
        return vaccineSupply.isNotEmpty() &&
            vaccineSupply.lowercase() == "vfc"
    }

    fun isState(): Boolean {
        return vaccineSupply.isNotEmpty() &&
            vaccineSupply.lowercase() == "state"
    }

    fun isSection317(): Boolean {
        return vaccineSupply.isNotEmpty() &&
            vaccineSupply.lowercase() == "section317"
    }

    fun vaccineSupplyString(): String = vaccineSupply.lowercase().replace("section", "").uppercase()

    fun isMedDTagShown() = encounterState?.medDMessage?.status != null && isPrivate()

    fun isMedDAndDateOfService() =
        encounterState?.medDMessage?.status != null && isPrivate() &&
            appointmentTime.toLocalDate() == LocalDate.now()

    fun isMissingDataRisk() = encounterState?.vaccineMessage?.status == AppointmentStatus.AT_RISK_DATA_MISSING

    fun hasShotStatus(shotStatus: ShotStatus) = encounterState?.shotStatus == shotStatus

    fun getAllRequiredDemographicFields(): List<DemographicField> {
        return patient.let {
            mutableListOf<DemographicField>().apply {
                add(DemographicField.FirstName(it.firstName))
                add(DemographicField.LastName(it.lastName))
                add(DemographicField.Phone(it.phoneNumber))
                add(DemographicField.Gender(it.gender))
                add(DemographicField.DateOfBirth(it.dob))
            }
        }
    }

    fun getAllMissingDemographicFields(): List<DemographicField> =
        getAllRequiredDemographicFields().filter { it.currentValue.isNullOrEmpty() }

    fun getAllRequiredPayerFields(): List<PayerField> {
        return patient.paymentInformation?.let {
            mutableListOf<PayerField>().apply {
                add(PayerField.PayerName(it.insuranceName))
                add(PayerField.MemberId(it.primaryMemberId))
                add(PayerField.GroupId(it.primaryGroupId))
                // TODO: Refactor this, these .toString should be in the Repository layer
                add(PayerField.PlanId(it.primaryInsurancePlanId?.toString()))
                add(PayerField.PortalMappingId(it.portalInsuranceMappingId?.toString()))
            }
        } ?: PayerField.emptyPayerFields()
    }

    fun getRelativeDoS() = RelativeDoS.getRelativeDoS(appointmentTime, checkedOut)

    fun getMedDCta() = encounterState?.medDMessage?.callToAction

    /**
     * Used by the AppointmentDao in order to properly
     * store data for an Appointment
     */
    fun toAppointmentData() =
        AppointmentData(
            id = id,
            clinicId = clinicId,
            vaccineSupply = vaccineSupply,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            isProcessing = false
        )
}

fun Appointment.getInventorySource(): InventorySource =
    when {
        this.isPrivate() -> InventorySource.PRIVATE
        this.isSection317() -> InventorySource.THREE_SEVENTEEN
        this.isState() -> InventorySource.STATE
        this.isVFC() -> InventorySource.VFC
        else -> InventorySource.ANOTHER_LOCATION
    }

/**
 * Object holding meta data for Eligibility UI
 *
 * @property appointment - associated Appointment
 * @property overridePaymentMode - overridePaymentMode
 */
data class EligibilityUiOptions(
    val appointment: Appointment,
    val overridePaymentMode: PaymentMode? = null,
    val inReviewIconOverride: Boolean = false
)
