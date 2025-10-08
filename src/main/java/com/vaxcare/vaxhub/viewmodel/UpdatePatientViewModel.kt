/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.domain.UploadAppointmentMediaUseCase
import com.vaxcare.vaxhub.model.AppointmentMediaType
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.model.UpdatePatientData
import com.vaxcare.vaxhub.model.metric.InsuranceUpdateWorkflowPresentedMetric
import com.vaxcare.vaxhub.model.patient.AppointmentMediaField
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatePatientViewModel @Inject constructor(
    val appointmentRepository: AppointmentRepository,
    val locationRepository: LocationRepository,
    @MHAnalyticReport val analytics: AnalyticReport,
    private val uploadAppointmentMediaUseCase: UploadAppointmentMediaUseCase
) : BaseViewModel() {
    sealed class UpdatePatientState : State {
        object UpdateSuccessful : UpdatePatientState()

        object UpdateFailed : UpdatePatientState()
    }

    // TODO: Refactor this, is breaking multiple SOLID principles
    fun updatePatient(appointmentId: Int?, data: UpdatePatientData) =
        viewModelScope.launch(Dispatchers.IO) {
            setState(LoadingState)
            val result =
                appointmentRepository.getAppointmentByIdAsync(appointmentId ?: -1)?.let { appointment ->
                    try {
                        val patient = appointment.patient
                        val patientId = patient.id
                        val request = data.updatePatient ?: run {
                            UpdatePatient(
                                patient,
                                UpdatePatient.PaymentInformation(patient, data.payer)
                            )
                        }
                        val selectedPayer = data.payer
                        val retriedPhoto = data.retriedPhoto
                        val payerChanged =
                            selectedPayer?.insuranceId != appointment.patient.paymentInformation?.primaryInsuranceId
                        val cardRequested = selectedPayer?.isNormalPayer() ?: false
                        val noCardSelected = selectedPayer?.isNormalPayer() == true &&
                            (data.frontInsurancePath.isNullOrEmpty() || data.backInsurancePath.isNullOrEmpty())

                        saveInsuranceUpdateMetric(
                            data.appointmentId,
                            payerChanged,
                            cardRequested,
                            noCardSelected,
                            retriedPhoto
                        )

                        if (selectedPayer == null) {
                            if (data.frontInsurancePath != null && data.backInsurancePath != null) {
                                request.mediaProvided = listOf(
                                    AppointmentMediaType.INSURANCE_CARD_FRONT.tag,
                                    AppointmentMediaType.INSURANCE_CARD_BACK.tag
                                )
                            }

                            val mediaFields = extractMediaFields(data)

                            appointmentRepository.patchPatient(
                                patientId = patientId,
                                fields = mediaFields + request.getDeltaFields(appointment),
                                appointmentId = null
                            )
                        } else {
                            if (selectedPayer.isNormalPayer()) {
                                if (data.frontInsurancePath != null && data.backInsurancePath != null) {
                                    request.mediaProvided = listOf(
                                        AppointmentMediaType.INSURANCE_CARD_FRONT.tag,
                                        AppointmentMediaType.INSURANCE_CARD_BACK.tag
                                    )
                                }
                                request.paymentInformation?.primaryInsuranceId =
                                    selectedPayer.insuranceId
                                request.paymentInformation?.primaryInsurancePlanId =
                                    selectedPayer.insurancePlanId
                                request.paymentInformation?.primaryMemberId = null
                                request.paymentInformation?.primaryGroupId = null

                                val mediaFields = extractMediaFields(data)
                                appointmentRepository.patchPatient(
                                    patientId = patientId,
                                    fields = mediaFields + request.getDeltaFields(appointment),
                                    appointmentId = null
                                )
                            }
                        }

                        Result.success(Unit)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Result.failure(e)
                    }
                } ?: Result.failure(Exception("Appointment not found"))

            val state = if (result.isSuccess) {
                appointmentId?.let {
                    uploadPatientCards(
                        appointmentId = appointmentId,
                        driverLicenseFrontPath = null,
                        insuranceCardFrontPath = data.frontInsurancePath,
                        insuranceCardBackPath = data.backInsurancePath
                    )
                    UpdatePatientState.UpdateSuccessful
                } ?: UpdatePatientState.UpdateFailed
            } else {
                UpdatePatientState.UpdateFailed
            }

            setState(state)
        }

    /**
     * Returns a list of InfoFields based on the frontInsurance and or backInsurance path
     */
    private fun extractMediaFields(data: UpdatePatientData): List<InfoField> =
        listOfNotNull(
            if (data.frontInsurancePath != null) {
                AppointmentMediaField.InsuranceCardFront()
            } else {
                null
            },
            if (data.backInsurancePath != null) {
                AppointmentMediaField.InsuranceCardBack()
            } else {
                null
            }
        )

    private fun saveInsuranceUpdateMetric(
        visitId: Int?,
        payerChanged: Boolean,
        insuranceCardRequested: Boolean,
        noInsuranceCardSelected: Boolean,
        cleanImageCaptureValidationPresented: Boolean
    ) {
        analytics.saveMetric(
            InsuranceUpdateWorkflowPresentedMetric(
                visitId,
                payerChanged,
                insuranceCardRequested,
                noInsuranceCardSelected,
                cleanImageCaptureValidationPresented
            )
        )
    }

    private suspend fun uploadPatientCards(
        appointmentId: Int,
        driverLicenseFrontPath: String?,
        insuranceCardFrontPath: String?,
        insuranceCardBackPath: String?
    ) {
        // Driver License Front
        driverLicenseFrontPath?.let { mediaPath ->
            uploadAppointmentMediaUseCase.uploadDriverLicenseFront(mediaPath, appointmentId)
        }

        // Insurance Card Front
        insuranceCardFrontPath?.let { mediaPath ->
            uploadAppointmentMediaUseCase.uploadInsuranceCardFront(mediaPath, appointmentId)
        }

        // Insurance Card Back
        insuranceCardBackPath?.let { mediaPath ->
            uploadAppointmentMediaUseCase.uploadInsuranceCardBack(mediaPath, appointmentId)
        }
    }
}
