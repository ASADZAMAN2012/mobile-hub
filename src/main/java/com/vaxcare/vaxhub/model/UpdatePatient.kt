/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.data.typeconverter.SerializeNulls
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.Race
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.model.patient.PayerField
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Parcelize
@JsonClass(generateAdapter = true)
class UpdatePatient(
    val id: Int,
    var mediaProvided: List<Int>? = null,
    val firstName: String,
    val lastName: String,
    var dob: LocalDate,
    val gender: Int,
    var phoneNumber: String,
    @SerializeNulls val address1: String?,
    @SerializeNulls val address2: String?,
    @SerializeNulls val city: String?,
    @SerializeNulls val state: String?,
    @SerializeNulls val zip: String?,
    var paymentInformation: PaymentInformation? = null,
    @SerializeNulls val race: Race? = null,
    @SerializeNulls val ethnicity: Ethnicity? = null,
    @SerializeNulls val originatorPatientId: String?,
    @SerializeNulls val middleInitial: String?,
    @SerializeNulls val ssn: String?,
    @SerializeNulls val email: String?,
) : Parcelable {
    @Parcelize
    @JsonClass(generateAdapter = true)
    class PaymentInformation(
        @SerializeNulls val id: Int? = null,
        @SerializeNulls val patientId: Int? = null,
        @SerializeNulls val uninsured: Boolean? = null,
        @SerializeNulls val insuranceName: String? = null,
        @SerializeNulls var primaryInsuranceId: Int? = null,
        @SerializeNulls var primaryInsurancePlanId: Int? = null,
        @SerializeNulls val portalInsuranceMappingId: Int? = null,
        @SerializeNulls var primaryMemberId: String? = null,
        @SerializeNulls var primaryGroupId: String? = null,
        @SerializeNulls val mbi: String? = null,
        @SerializeNulls val insuredFirstName: String? = null,
        @SerializeNulls val insuredLastName: String? = null,
        val insuredDob: LocalDate? = null,
        @SerializeNulls val insuredGender: Int? = null,
        @SerializeNulls val appointmentId: Int? = null,
        @SerializeNulls val relationshipToInsured: RelationshipToInsured? = null,
        @SerializeNulls val paymentMode: PaymentMethod? = null,
        @SerializeNulls val vfcFinancialClass: String? = null
    ) : Parcelable {
        constructor(patient: Patient, payer: Payer?) : this(
            id = patient.paymentInformation?.id,
            patientId = patient.id,
            uninsured = payer?.isUninsuredPayer() ?: patient.paymentInformation?.uninsured,
            insuranceName = payer?.insuranceName ?: patient.paymentInformation?.insuranceName,
            primaryInsuranceId = payer?.insuranceId
                ?: patient.paymentInformation?.primaryInsuranceId,
            primaryInsurancePlanId = payer?.insurancePlanId
                ?: patient.paymentInformation?.primaryInsurancePlanId,
            portalInsuranceMappingId = payer?.portalInsuranceMappingId
                ?: patient.paymentInformation?.portalInsuranceMappingId,
            primaryMemberId = patient.paymentInformation?.primaryMemberId,
            primaryGroupId = patient.paymentInformation?.primaryGroupId,
            mbi = patient.paymentInformation?.mbi,
            insuredFirstName = patient.paymentInformation?.insuredFirstName,
            insuredLastName = patient.paymentInformation?.insuredLastName,
            insuredDob = patient.paymentInformation?.getDob(),
            insuredGender = if (patient.paymentInformation?.insuredGender == "Male") 0 else 1,
            appointmentId = patient.paymentInformation?.appointmentId,
            relationshipToInsured = patient.paymentInformation?.relationshipToInsured,
            paymentMode = patient.paymentInformation?.paymentMode,
            vfcFinancialClass = patient.paymentInformation?.vfcFinancialClass
        )

        fun toPatientPayerInfo() =
            PaymentInformation(
                id = id ?: -1,
                insuranceName = insuranceName,
                primaryInsuranceId = primaryInsuranceId,
                primaryInsurancePlanId = primaryInsurancePlanId,
                primaryMemberId = primaryMemberId,
                primaryGroupId = primaryGroupId,
                uninsured = uninsured ?: true,
                paymentMode = paymentMode ?: PaymentMethod.NoPay,
                vfcFinancialClass = vfcFinancialClass,
                insuredFirstName = insuredFirstName,
                insuredLastName = insuredLastName,
                insuredDob = insuredDob?.atStartOfDay()
                    ?.toLocalDateString("yyyy-MM-dd'T'hh:mm:ss"),
                insuredGender = insuredGender?.let { gen ->
                    Patient.PatientGender.fromInt(gen).toString()
                },
                appointmentId = appointmentId,
                relationshipToInsured = relationshipToInsured,
                portalInsuranceMappingId = portalInsuranceMappingId,
                mbi = mbi
            )
    }

    constructor(patient: Patient, paymentInfo: PaymentInformation? = null) : this(
        id = patient.id,
        address1 = patient.address1,
        address2 = patient.address2,
        city = patient.city,
        state = patient.state,
        zip = patient.zip,
        originatorPatientId = patient.originatorPatientId,
        firstName = patient.firstName,
        middleInitial = patient.middleInitial,
        lastName = patient.lastName,
        ethnicity = patient.ethnicity?.let { Ethnicity.fromString(it) },
        race = patient.race?.let { Race.fromString(it) },
        dob = patient.getDobString()
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("M/dd/yyyy")) }
            ?: LocalDate.now(),
        gender = patient.gender?.let { Patient.PatientGender.fromString(it).ordinal }
            ?: 0,
        ssn = patient.ssn,
        phoneNumber = patient.phoneNumber?.replace("-", "") ?: "",
        email = patient.email,
        paymentInformation = paymentInfo
    )

    fun toPatient(patientId: Int) =
        Patient(
            id = patientId,
            originatorPatientId = originatorPatientId,
            firstName = firstName,
            lastName = lastName,
            dob = dob.atStartOfDay()?.toLocalDateString("yyyy-MM-dd'T'hh:mm:ss"),
            middleInitial = middleInitial,
            race = race?.toString(),
            ethnicity = ethnicity?.toString(),
            gender = Patient.PatientGender.fromInt(gender).value,
            ssn = ssn,
            address1 = address1,
            address2 = address2,
            city = city,
            state = state,
            zip = zip,
            phoneNumber = phoneNumber,
            email = email,
            paymentInformation = paymentInformation?.toPatientPayerInfo()
        )

    /**
     * Returns a list of fields from a given appointment's patient demographics and paymentinformantion
     *
     * @param appointment The appointment to use
     */
    fun getDeltaFields(appointment: Appointment): List<InfoField> {
        val patient = appointment.patient
        val payerDeltas = getPaymentDeltas(patient.paymentInformation)
        return payerDeltas + listOfNotNull(
            if (patient.firstName != firstName) {
                DemographicField.FirstName(firstName)
            } else {
                null
            },
            if (patient.lastName != lastName) {
                DemographicField.LastName(lastName)
            } else {
                null
            },
            if (patient.getDobString() != dob.toLocalDateString()) {
                DemographicField.DateOfBirth(dob.toLocalDateString())
            } else {
                null
            },
            if (patient.gender?.let { Patient.PatientGender.fromString(it) } != Patient.PatientGender.fromInt(
                    gender
                )
            ) {
                DemographicField.Gender(Patient.PatientGender.fromInt(gender).value)
            } else {
                null
            },
            if (patient.phoneNumber != phoneNumber) {
                DemographicField.Phone(phoneNumber)
            } else {
                null
            },
            if (patient.address1 != address1) {
                DemographicField.AddressOne(address1)
            } else {
                null
            },
            if (patient.address2 != address2) {
                DemographicField.AddressTwo(address2)
            } else {
                null
            },
            if (patient.city != city) {
                DemographicField.City(city)
            } else {
                null
            },
            if (patient.state != state) {
                DemographicField.State(state)
            } else {
                null
            },
            if (patient.zip != zip) {
                DemographicField.Zip(zip)
            } else {
                null
            }
        )
    }

    /**
     * Returns a list of fields from a given paymentinformantion
     */
    fun getPaymentDeltas(paymentInfo: com.vaxcare.vaxhub.model.PaymentInformation?): List<InfoField> =
        paymentInformation?.toPatientPayerInfo()?.let { newInfo ->
            if (paymentInfo == newInfo) {
                emptyList()
            } else {
                listOfNotNull(
                    if (paymentInfo?.insuranceName != newInfo.insuranceName) {
                        PayerField.PayerName(newInfo.insuranceName, newInfo.primaryInsuranceId ?: 0)
                    } else {
                        null
                    },
                    if (paymentInfo?.primaryGroupId != newInfo.primaryGroupId) {
                        PayerField.GroupId(newInfo.primaryGroupId)
                    } else {
                        null
                    },
                    if (paymentInfo?.primaryMemberId != newInfo.primaryMemberId) {
                        PayerField.MemberId(newInfo.primaryMemberId)
                    } else {
                        null
                    },
                    if (paymentInfo?.primaryInsurancePlanId != newInfo.primaryInsurancePlanId) {
                        PayerField.PlanId(newInfo.primaryInsurancePlanId?.toString())
                    } else {
                        null
                    },
                    if (paymentInfo?.portalInsuranceMappingId != newInfo.portalInsuranceMappingId) {
                        PayerField.PortalMappingId(newInfo.portalInsuranceMappingId?.toString())
                    } else {
                        null
                    }
                )
            }
        } ?: emptyList()
}
