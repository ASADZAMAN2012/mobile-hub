/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.ShotStatus
import java.time.LocalDateTime

object MockAppointments {
    val medDAppointmentCheckPostRun = Appointment(
        id = 72600942,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.parse("2022-10-19T14:00"),
        checkedOutTime = null,
        patient = MockPatients.medDPatient65,
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.InsurancePay,
        visitType = "Well",
        checkedOut = false,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = null,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = AppointmentIcon.STAR,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "abc123",
                    serviceType = AppointmentServiceType.VACCINE
                ),
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = null,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.MedDCollectCreditCard,
                    topRejectCode = "10019",
                    serviceType = AppointmentServiceType.MEDD
                )
            )
        },
        administeredVaccines = emptyList(),
        isProcessing = false,
        orders = emptyList()
    )
    val medDAppointmentCheckNotRun = Appointment(
        id = 72600942,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.now(),
        checkedOutTime = null,
        patient = MockPatients.medDPatient65,
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.InsurancePay,
        visitType = "Well",
        checkedOut = false,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = null,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = AppointmentIcon.STAR,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "abc123",
                    serviceType = AppointmentServiceType.VACCINE
                ),
                EncounterMessageEntity(
                    222,
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

    val medDAppointmentCheckFinished = Appointment(
        id = 72600942,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.now(),
        checkedOutTime = null,
        patient = MockPatients.medDPatient65,
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.InsurancePay,
        visitType = "Well",
        checkedOut = false,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = null,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = AppointmentIcon.STAR,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "abc123",
                    serviceType = AppointmentServiceType.VACCINE
                ),
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = null,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.MedDCollectCreditCard,
                    topRejectCode = "10019",
                    serviceType = AppointmentServiceType.MEDD
                )
            )
        },
        administeredVaccines = emptyList(),
        isProcessing = false,
        orders = emptyList()
    )

    val medDAppointmentCheckNotRunAndMissingMedDDemo = Appointment(
        id = 72600942,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.parse("2022-10-19T14:00"),
        checkedOutTime = null,
        patient = MockPatients.medDPatient65MissingSSNAndMBI,
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.InsurancePay,
        visitType = "Well",
        checkedOut = false,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = null,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = AppointmentIcon.STAR,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "abc123",
                    serviceType = AppointmentServiceType.VACCINE
                ),
                EncounterMessageEntity(
                    222,
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

    val medDAppointmentCheckFinishedSelfPay = Appointment(
        id = 72600942,
        clinicId = 10808,
        vaccineSupply = "Private",
        appointmentTime = LocalDateTime.parse("2022-10-19T14:00"),
        checkedOutTime = null,
        patient = MockPatients.medDPatient65,
        paymentType = "Cigna",
        paymentMethod = PaymentMethod.SelfPay,
        visitType = "Well",
        checkedOut = false,
        provider = Provider(
            id = 100001877,
            firstName = "Dean",
            lastName = "Morris"
        ),
        administeredBy = null,
        isEditable = true,
        encounterState = EncounterStateEntity(
            id = 0,
            appointmentId = 0,
            shotStatus = ShotStatus.PreShot,
            isClosed = false,
            createdUtc = LocalDateTime.now()
        ).apply {
            messages = listOf(
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = AppointmentIcon.STAR,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.None,
                    topRejectCode = "abc123",
                    serviceType = AppointmentServiceType.VACCINE
                ),
                EncounterMessageEntity(
                    222,
                    0,
                    AppointmentStatus.RISK_FREE,
                    icon = null,
                    primaryMessage = "",
                    secondaryMessage = "",
                    callToAction = CallToAction.MedDCollectCreditCard,
                    topRejectCode = "10019",
                    serviceType = AppointmentServiceType.MEDD
                )
            )
        },
        administeredVaccines = emptyList(),
        isProcessing = false,
        orders = emptyList()
    )
}
