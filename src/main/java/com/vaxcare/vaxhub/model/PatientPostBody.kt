/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.content.Context
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalDateTime

@Parcelize
@JsonClass(generateAdapter = true)
class PatientPostBody(
    val patientId: Int? = null,
    val newPatient: NewPatient? = null,
    val clinicId: Long = 0L,
    var date: LocalDateTime? = null,
    var providerId: Int? = null,
    var initialPaymentMode: String? = null,
    var metaData: AppointmentMetaData? = null,
    val visitType: String
) : Parcelable {
    @Parcelize
    class NewPatient(
        val firstName: String,
        val lastName: String,
        val dob: LocalDate,
        val gender: Int,
        var phoneNumber: String,
        val address1: String?,
        val address2: String?,
        val city: String?,
        val state: String?,
        val zip: String?,
        var paymentInformation: PaymentInformation? = null,
        val race: Race? = null,
        val ethnicity: Ethnicity? = null,
        @Json(name = "SSN")
        val ssn: String? = null
    ) : Parcelable {
        fun getFormatAddress(context: Context): String {
            val builder = StringBuilder()
            if (!address1.isNullOrEmpty()) {
                builder.append("${address1}\n")
            }
            if (!address2.isNullOrEmpty()) {
                builder.append("${address2}\n")
            }
            if (!city.isNullOrEmpty()) {
                builder.append(city)
                if (state.isNullOrEmpty()) {
                    builder.append("\n")
                } else {
                    builder.append(", ${state}\n")
                }
            } else {
                if (!state.isNullOrEmpty()) {
                    builder.append("${state}\n")
                }
            }
            if (!zip.isNullOrEmpty()) {
                builder.append(zip)
            }

            var address = builder.toString().trim()
            if (address.isEmpty()) {
                address = context.getString(R.string.patient_confirm_no_address)
            }
            return address
        }

        fun toUpdatePatient(patient: Patient) =
            UpdatePatient(
                id = patient.id,
                address1 = this.address1,
                address2 = this.address2,
                city = this.city,
                state = this.state,
                zip = this.zip,
                originatorPatientId = patient.originatorPatientId,
                firstName = this.firstName,
                middleInitial = patient.middleInitial,
                lastName = this.lastName,
                ethnicity = this.ethnicity,
                race = this.race,
                dob = this.dob,
                gender = this.gender,
                ssn = patient.ssn,
                phoneNumber = this.phoneNumber,
                email = patient.email,
                paymentInformation = patient.paymentInformation?.let {
                    UpdatePatient.PaymentInformation(
                        id = it.id,
                        patientId = patient.id,
                        uninsured = it.uninsured,
                        insuranceName = it.insuranceName,
                        primaryInsuranceId = it.primaryInsuranceId,
                        primaryInsurancePlanId = it.primaryInsurancePlanId,
                        portalInsuranceMappingId = it.portalInsuranceMappingId,
                        primaryMemberId = it.primaryMemberId,
                        primaryGroupId = it.primaryGroupId,
                        mbi = it.mbi,
                        insuredFirstName = this.firstName,
                        insuredLastName = this.lastName,
                        insuredDob = this.dob,
                        insuredGender = this.gender,
                        appointmentId = it.appointmentId,
                        relationshipToInsured = it.relationshipToInsured,
                        paymentMode = it.paymentMode,
                        vfcFinancialClass = it.vfcFinancialClass
                    )
                }
            )
    }

    @Parcelize
    class PaymentInformation(
        var primaryInsuranceId: Int? = null,
        var primaryInsurancePlanId: Int? = null,
        var secondaryInsuranceId: Int? = null,
        var uninsured: Boolean? = null,
        var paymentMode: String? = null,
        var primaryMemberId: String? = null,
        var primaryGroupId: String? = null,
        var relationshipToInsured: RelationshipToInsured? = null,
        var insuranceName: String? = null,
        var insuredFirstName: String? = null,
        var insuredLastName: String? = null,
        var insuredDob: String? = null,
        var insuredGender: String? = null,
        var mbi: String? = null,
        var stock: String? = null,
    ) : Parcelable

    @Parcelize
    data class AppointmentMetaData(
        val id: Int = 0,
        val mediaProvided: List<Int> = emptyList(),
        val flags: List<Int> = emptyList()
    ) : Parcelable

    @JsonClass(generateAdapter = false)
    enum class AppointmentFlagType(val value: Int) {
        PatientContactPhoneOptIn(1)
    }

    /**
     * Populate a list of Media tags based on PatientCollectData parameter
     * @return List of MediaType tags - empty list if N/A
     */
    fun getMediaProvidedFromCollection(data: PatientCollectData) =
        when {
            data.frontInsurancePath != null && data.backInsurancePath != null -> listOf(
                AppointmentMediaType.INSURANCE_CARD_FRONT.tag,
                AppointmentMediaType.INSURANCE_CARD_BACK.tag
            )

            data.driverLicenseFrontPath != null -> listOf(AppointmentMediaType.DRIVERS_LICENSE_FRONT.tag)
            else -> emptyList()
        }
}
