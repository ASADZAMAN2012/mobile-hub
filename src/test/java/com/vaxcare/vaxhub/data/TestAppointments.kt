/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.RelationshipToInsured
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ShotStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TestAppointments {
    val medDAppt = Appointment(
        id = 72600942,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.now(),
        checkedOutTime = LocalDateTime.now(),
        patient = Patient(
            id = 43871724,
            originatorPatientId = null,
            firstName = "MEDD",
            lastName = "ELIGIBLE",
            dob = "1940-03-07T00:00:00",
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
            phoneNumber = "330-088-8785",
            email = null,
            paymentInformation = PaymentInformation(
                id = 500044299,
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
        ),
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.InsurancePay,
        visitType = "Well",
        checkedOut = true,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = 6169,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PostShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    123,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = null,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "10019",
                    serviceType = AppointmentServiceType.VACCINE
                ),
                EncounterMessageEntity(
                    123,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = null,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.MedDCanRun,
                    topRejectCode = "10019",
                    serviceType = AppointmentServiceType.MEDD
                )
            )
        },
        administeredVaccines = emptyList(),
        isProcessing = false,
        orders = emptyList()
    )

    val pediatricAppt = Appointment(
        id = 72600943,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.parse("2022-10-19T14:00"),
        checkedOutTime = LocalDateTime.now(),
        patient = Patient(
            id = 43871724,
            originatorPatientId = null,
            firstName = "Little",
            lastName = "Buddy",
            dob = LocalDateTime.now().minusMonths(12)
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
            phoneNumber = "330-088-8785",
            email = null,
            paymentInformation = PaymentInformation(
                id = 500044299,
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
        ),
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.InsurancePay,
        visitType = "Well",
        checkedOut = true,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = 6169,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PostShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    123,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = null,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "10019",
                    serviceType = AppointmentServiceType.VACCINE
                )
            )
        },
        administeredVaccines = emptyList(),
        isProcessing = false,
        orders = emptyList()
    )
}
