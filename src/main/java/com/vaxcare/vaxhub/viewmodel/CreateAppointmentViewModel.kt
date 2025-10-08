/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.combineWith
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentServiceType
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.enums.ShotStatus
import com.vaxcare.vaxhub.model.metric.TroubleConnectingDialogClickMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.ui.dialog.TroubleConnectingDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CreateAppointmentViewModel @Inject constructor(
    private val localStorage: LocalStorage,
    private val appointmentRepository: AppointmentRepository,
    private val analytics: AnalyticReport,
    private val dispatcherProvider: DispatcherProvider
) : BaseViewModel() {
    private val timeoutUI = 25000L // 25 seconds
    private val timeoutAPI = 20000L // 20 seconds
    private val appointmentIdLiveData = MutableLiveData<Int?>(null)
    private var creationJob: Job? = null

    val appointmentLiveData = appointmentIdLiveData.switchMap {
        it?.let {
            appointmentRepository.getAppointmentLiveDataById(it)
                .combineWith(
                    appointmentRepository.getEncounterMessagesByAppointmentId(it)
                ) { appointment, messages ->
                    appointment?.apply {
                        encounterState?.messages = messages ?: emptyList()
                    }
                }
        }
    }

    sealed class CreateAppointmentState : State {
        object CreationFailed : CreateAppointmentState()

        object UpdateUI : CreateAppointmentState()

        data class CreationSuccess(val appointmentId: Int) : CreateAppointmentState()

        object TimeoutReached : CreateAppointmentState()

        data class TimeoutAppointment(
            val appointment: Appointment?,
            val setRiskFree: Boolean
        ) : CreateAppointmentState()
    }

    init {
        loadingStates.add(CreateAppointmentState.UpdateUI)
    }

    fun waitLongerForAppointment(appointmentId: Int) {
        creationJob?.cancel()
        creationJob = viewModelScope.launch(dispatcherProvider.io) {
            setState(CreateAppointmentState.CreationSuccess(appointmentId))
            waitForAppointmentDetails(appointmentId)
        }
    }

    fun saveRetryMetric(optionSelected: TroubleConnectingDialog.Option) {
        analytics.saveMetric(
            TroubleConnectingDialogClickMetric(
                optionSelected = TroubleConnectingDialog.Option.TRY_AGAIN
            )
        )
    }

    fun createAppointment(patientId: Int, providerId: Int?) {
        resetState()
        val appointmentRequest = PatientPostBody(
            patientId = patientId,
            clinicId = localStorage.currentClinicId,
            providerId = providerId,
            visitType = "Well"
        )

        creationJob = viewModelScope.launch(dispatcherProvider.io) {
            var appointmentId: String? = null
            try {
                appointmentId = appointmentRepository.postAppointmentWithUTCZoneOffsetAndGetId(
                    appointmentRequest
                )
                setState(CreateAppointmentState.CreationSuccess(appointmentId.toInt()))
            } catch (e: Exception) {
                Timber.e("Error creating appointment for patientId $patientId: $e")
                setState(CreateAppointmentState.CreationFailed)
            }

            waitForAppointmentDetails(appointmentId?.toInt())
        }
    }

    private suspend fun waitForAppointmentDetails(appointmentId: Int?) {
        coroutineScope {
            delay(timeoutUI)
            setState(CreateAppointmentState.UpdateUI)
            delay(timeoutAPI)
            setState(CreateAppointmentState.TimeoutReached)
            appointmentId?.let {
                val fetchEligibilityStatus = async {
                    appointmentRepository.getAppointmentEligibilityStatus(it)
                }
                val fetchAppointment = async {
                    appointmentRepository.getAndInsertUpdatedAppointment(it)
                }

                val eligibilityStatus = fetchEligibilityStatus.await()
                val appointment = fetchAppointment.await()?.toAppointment()

                when {
                    (
                        appointment?.encounterState?.messages.isNullOrEmpty() ||
                            eligibilityStatus?.riskUpdatedUtc == null
                    ) -> {
                        Timber.e("Error: No eligibility for appointmentId: $it. Displaying risk-free")
                        setRiskFreeValues(appointment)
                        setState(CreateAppointmentState.TimeoutAppointment(appointment, true))
                    }

                    else -> setState(CreateAppointmentState.TimeoutAppointment(appointment, false))
                }
            }
        }
    }

    fun setAppointmentId(appointmentId: Int) {
        appointmentIdLiveData.postValue(appointmentId)
    }

    private suspend fun setRiskFreeValues(appointment: Appointment?) {
        appointment?.let {
            val encounterState = EncounterStateEntity(
                appointmentId = appointment.id,
                shotStatus = ShotStatus.PreShot,
                isClosed = false,
                createdUtc = LocalDateTime.now()
            )

            val message = EncounterMessageEntity(
                // Unique value for composite key (negative appointmentId)
                riskAssessmentId = (appointment.id * -1),
                appointmentId = appointment.id,
                status = AppointmentStatus.RISK_FREE,
                icon = AppointmentIcon.STAR,
                primaryMessage = "Ready to Vaccinate (Risk Free)",
                secondaryMessage = null,
                callToAction = CallToAction.None,
                topRejectCode = null,
                serviceType = AppointmentServiceType.VACCINE
            )

            appointment.encounterState = encounterState
            appointment.encounterState?.messages = listOf(message)
            appointmentRepository.upsertAppointments(listOf(appointment))
        }
    }
}
