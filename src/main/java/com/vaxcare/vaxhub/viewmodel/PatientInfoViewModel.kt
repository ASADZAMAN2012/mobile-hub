/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.UpdatePatient
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PayerRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PatientInfoViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val payerRepository: PayerRepository,
    locationRepository: LocationRepository,
    productRepository: ProductRepository,
    wrongProductRepository: WrongProductRepository
) : BaseScannerViewModel(locationRepository, productRepository, wrongProductRepository) {
    sealed class PatientInfoState : State {
        data class AppointmentLoaded(val appointment: Appointment?) : PatientInfoState()

        data class AppointmentLocallyUpdated(val appointment: Appointment?) : PatientInfoState()

        data class PayerInfoLoaded(val payer: Payer?, val appointment: Appointment) :
            PatientInfoState()

        object Failed : PatientInfoState()
    }

    fun fetchUpdatedAppointment(appointmentId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val state = try {
                appointmentRepository.getAndInsertUpdatedAppointment(appointmentId)
                PatientInfoState.AppointmentLoaded(
                    appointmentRepository.getAppointmentByIdAsync(
                        appointmentId
                    )
                )
            } catch (e: Exception) {
                Timber.e(e)
                PatientInfoState.Failed
            }

            setState(state)
        }

    fun updateInfoLocally(
        patientId: Int,
        updatePatient: UpdatePatient,
        appointmentId: Int
    ) = viewModelScope.safeLaunch(Dispatchers.IO) {
        val updateAppt =
            appointmentRepository.updatePatientLocally(updatePatient, patientId, appointmentId)
        setState(PatientInfoState.AppointmentLocallyUpdated(updateAppt))
    }

    fun getPayerInformation(appointmentId: Int?) =
        viewModelScope.launch(Dispatchers.IO) {
            val state = try {
                val appointment =
                    appointmentId?.let { appointmentRepository.getAppointmentByIdAsync(it) }
                        ?: run {
                            throw Exception("GetPayerInformation: appointmentId $appointmentId returned null")
                        }

                appointment.patient.paymentInformation?.let {
                    val payer = payerRepository.getPayerByInsuranceId(it.primaryInsuranceId ?: -1)
                    PatientInfoState.PayerInfoLoaded(payer, appointment)
                } ?: PatientInfoState.PayerInfoLoaded(null, appointment)
            } catch (e: Exception) {
                Timber.e(e)
                PatientInfoState.Failed
            }

            setState(state)
        }
}
