/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MockPatients {
    val medDPatient65 = Patient(
        id = 1,
        originatorPatientId = null,
        firstName = "MEDD",
        lastName = "ELIGIBLE",
        dob = LocalDateTime.now().minusYears(65)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss")),
        middleInitial = null,
        race = "Unspecified",
        ethnicity = "Unspecified",
        gender = "Male",
        ssn = "XXX-XX-1111",
        address1 = null,
        address2 = null,
        city = null,
        state = "FL",
        zip = null,
        phoneNumber = "407-555-0123",
        email = null,
        paymentInformation = PaymentInformation(
            id = 1,
            insuranceName = "Cigna",
            primaryInsuranceId = 12,
            primaryInsurancePlanId = null,
            primaryMemberId = "SDF",
            primaryGroupId = null,
            uninsured = false,
            paymentMode = PaymentMethod.InsurancePay,
            vfcFinancialClass = null,
            insuredFirstName = "MEDD",
            insuredLastName = "ELIGIBLE",
            insuredDob = "1940-03-07T00:00:00",
            insuredGender = "Male",
            appointmentId = 72600942,
            relationshipToInsured = RelationshipToInsured.Self,
            portalInsuranceMappingId = null,
            mbi = null
        )
    )
    val medDPatient65MissingSSNAndMBI = Patient(
        id = 1,
        originatorPatientId = null,
        firstName = "MEDD",
        lastName = "ELIGIBLE",
        dob = LocalDateTime.now().minusYears(65)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss")),
        middleInitial = null,
        race = "Unspecified",
        ethnicity = "Unspecified",
        gender = "Male",
        ssn = null,
        address1 = null,
        address2 = null,
        city = null,
        state = "FL",
        zip = null,
        phoneNumber = "407-555-0123",
        email = null,
        paymentInformation = PaymentInformation(
            id = 1,
            insuranceName = "Cigna",
            primaryInsuranceId = 12,
            primaryInsurancePlanId = null,
            primaryMemberId = "SDF",
            primaryGroupId = null,
            uninsured = false,
            paymentMode = PaymentMethod.InsurancePay,
            vfcFinancialClass = null,
            insuredFirstName = "MEDD",
            insuredLastName = "ELIGIBLE",
            insuredDob = "1940-03-07T00:00:00",
            insuredGender = "Male",
            appointmentId = 72600942,
            relationshipToInsured = RelationshipToInsured.Self,
            portalInsuranceMappingId = null,
            mbi = null
        )
    )
}
