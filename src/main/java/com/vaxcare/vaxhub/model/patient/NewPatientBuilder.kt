/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import java.time.LocalDate

class NewPatientBuilder {
    // NewPatient Demographics
    private var firstName: String? = null
    private var lastName: String? = null
    private var dob: LocalDate? = null
    private var gender: Int? = null
    private var phoneNumber: String? = null
    private var address1: String? = null
    private var address2: String? = null
    private var city: String? = null
    private var state: String? = null
    private var zip: String? = null
    private var paymentInformation: PatientPostBody.PaymentInformation? = null
    private var race: Race? = null
    private var ethnicity: Ethnicity? = null
    private var ssn: String? = null

    // PaymentInformation
    private var primaryInsuranceId: Int? = null
    private var primaryInsurancePlanId: Int? = null
    private var secondaryInsuranceId: Int? = null
    private var uninsured: Boolean? = null
    private var paymentMode: String? = null
    private var primaryMemberId: String? = null
    private var primaryGroupId: String? = null
    private var relationshipToInsured: RelationshipToInsured? = null
    private var insuranceName: String? = null
    private var insuredFirstName: String? = null
    private var insuredLastName: String? = null
    private var insuredDob: String? = null
    private var insuredGender: String? = null
    private var mbi: String? = null
    private var stock: String = "Private"

    fun firstName(firstName: String?) = apply { this.firstName = firstName }

    fun lastName(lastName: String?) = apply { this.lastName = lastName }

    fun dob(dob: LocalDate?) = apply { this.dob = dob }

    fun gender(gender: Int?) = apply { this.gender = gender }

    fun phoneNumber(phoneNumber: String?) = apply { this.phoneNumber = phoneNumber }

    fun address1(address1: String?) = apply { this.address1 = address1 }

    fun address2(address2: String?) = apply { this.address2 = address2 }

    fun city(city: String?) = apply { this.city = city }

    fun state(state: String?) = apply { this.state = state }

    fun zip(zip: String?) = apply { this.zip = zip }

    //    fun paymentInformation(paymentInformation: String?) = apply { this.paymentInformation = paymentInformation }
    fun race(race: Race?) = apply { this.race = race }

    fun ethnicity(ethnicity: Ethnicity?) = apply { this.ethnicity = ethnicity }

    fun ssn(ssn: String?) = apply { this.ssn = ssn }

    // payment information
    fun primaryInsuranceId(primaryInsuranceId: Int?) =
        apply { this.primaryInsuranceId = if (primaryInsuranceId == 0) null else primaryInsuranceId }

    fun primaryInsurancePlanId(primaryInsurancePlanId: Int?) =
        apply { this.primaryInsurancePlanId = if (primaryInsurancePlanId == 0) null else primaryInsurancePlanId }

    fun secondaryInsuranceId(secondaryInsuranceId: Int?) = apply { this.secondaryInsuranceId = secondaryInsuranceId }

    fun uninsured(uninsured: Boolean?) = apply { this.uninsured = uninsured }

    fun paymentMode(paymentMode: String?) = apply { this.paymentMode = paymentMode }

    fun primaryMemberId(primaryMemberId: String?) = apply { this.primaryMemberId = primaryMemberId }

    fun primaryGroupId(primaryGroupId: String?) = apply { this.primaryGroupId = primaryGroupId }

    fun relationshipToInsured(relationshipToInsured: RelationshipToInsured?) =
        apply { this.relationshipToInsured = relationshipToInsured }

    fun insuranceName(insuranceName: String?) = apply { this.insuranceName = insuranceName }

    fun insuredFirstName(insuredFirstName: String?) = apply { this.insuredFirstName = insuredFirstName }

    fun insuredLastName(insuredLastName: String?) = apply { this.insuredLastName = insuredLastName }

    fun insuredDob(insuredDob: String?) = apply { this.insuredDob = insuredDob }

    fun insuredGender(insuredGender: String?) = apply { this.insuredGender = insuredGender }

    fun mbi(mbi: String?) = apply { this.mbi = mbi }

    fun build() =
        if (areRequiredFieldsPresent()) {
            PatientPostBody.NewPatient(
                firstName = firstName!!,
                lastName = lastName!!,
                dob = dob!!,
                gender = gender!!,
                phoneNumber = phoneNumber!!,
                address1 = address1,
                address2 = address2,
                city = city,
                state = state,
                zip = zip,
                paymentInformation = buildPaymentInformation(),
                race = race,
                ethnicity = ethnicity,
                ssn = ssn
            )
        } else {
            null
        }

    private fun areRequiredFieldsPresent() =
        firstName != null && lastName != null && dob != null && gender != null && phoneNumber != null

    fun buildPaymentInformation() =
        PatientPostBody.PaymentInformation(
            primaryInsuranceId = primaryInsuranceId,
            primaryInsurancePlanId = primaryInsurancePlanId,
            secondaryInsuranceId = secondaryInsuranceId,
            uninsured = uninsured,
            paymentMode = paymentMode,
            primaryMemberId = primaryMemberId,
            primaryGroupId = primaryGroupId,
            relationshipToInsured = relationshipToInsured,
            insuranceName = insuranceName,
            insuredFirstName = insuredFirstName,
            insuredLastName = insuredLastName,
            insuredDob = insuredDob,
            insuredGender = insuredGender,
            mbi = mbi,
            stock = stock
        )
}
