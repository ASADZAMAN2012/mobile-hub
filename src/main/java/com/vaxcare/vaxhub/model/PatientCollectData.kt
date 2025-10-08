/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Parcelize
data class PatientCollectData(
    var flow: NoInsuranceCardFlow = NoInsuranceCardFlow.CREATE_PATIENT,
    val appointmentId: Int? = null,
    val patientId: Int = -1,
    var currentPhone: String? = null,
    var updatePatientData: UpdatePatientData? = null,
    var manualDob: LocalDate? = null,
    // PhoneNumberWorkflowPresented metric data
    var phoneNumberPrefilled: Boolean = false,
    var phoneNumberUpdated: Boolean = false,
    var phoneContactAgreement: Boolean = false,
    // InsuranceCard flow
    var frontInsurancePath: String? = null,
    var backInsurancePath: String? = null,
    var driverLicenseFrontPath: String? = null,
    // MedD Signature Submission
    var signatureSubmitted: Boolean = false,
    // Enhanced Login
    var isSessionLocked: Boolean = false
) : Parcelable {
    fun mergeUpdatePatientData(): UpdatePatientData =
        updatePatientData?.copy(
            appointmentId = appointmentId,
            updatedPhone = currentPhone,
            frontInsurancePath = frontInsurancePath,
            backInsurancePath = backInsurancePath,
            phoneNumberWorkflowShown = true,
            phoneNumberPrefilled = phoneNumberPrefilled,
            phoneNumberUpdated = phoneNumberUpdated,
            phoneContactAgreement = phoneContactAgreement,
            phoneEntered = currentPhone
        ) ?: UpdatePatientData(
            appointmentId = appointmentId,
            updatedPhone = currentPhone,
            frontInsurancePath = frontInsurancePath,
            backInsurancePath = backInsurancePath,
            phoneNumberWorkflowShown = true,
            phoneNumberPrefilled = phoneNumberPrefilled,
            phoneNumberUpdated = phoneNumberUpdated,
            phoneContactAgreement = phoneContactAgreement,
            phoneEntered = currentPhone
        )

    fun hasUploadedCard() =
        (frontInsurancePath != null && backInsurancePath != null) || (driverLicenseFrontPath != null)
}
