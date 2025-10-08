/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.isFullSsnAndNotNull
import com.vaxcare.vaxhub.core.extension.isMaskedSsnFormatted
import com.vaxcare.vaxhub.core.extension.isMbiFormatAndNotNull
import com.vaxcare.vaxhub.core.extension.trimDashes
import com.vaxcare.vaxhub.core.model.enums.MedDIDType.MBI
import com.vaxcare.vaxhub.core.model.enums.MedDIDType.SSN
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.MedDCheckRequestBody
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.model.metric.AceReceivedMetric
import com.vaxcare.vaxhub.model.metric.CheckoutAppointmentMedDStatusMetric
import com.vaxcare.vaxhub.model.metric.MedDCheckRunMetric
import com.vaxcare.vaxhub.model.metric.MedDCheckTriggeredMetric
import com.vaxcare.vaxhub.model.metric.MedDCheckTriggeredMetric.TriggeredMode.MANUALLY
import com.vaxcare.vaxhub.model.metric.MedDStateMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.service.PartDService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MedDCheckViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val dispatcherProvider: DispatcherProvider,
    @MHAnalyticReport private val analytics: AnalyticReport,
    private val partDService: PartDService
) : BaseViewModel() {
    private val timeout = 20000L
    private val textChangeTimeout = 10000L
    private var timeoutState: State? = null
    private var medDAutoRunCheckStart: String? = null

    sealed class MedDCheckState : State {
        object Loading : MedDCheckState()

        object MedDCheckFailed : MedDCheckState()

        object TextChangeTimeout : MedDCheckState()

        object TimeoutResponse : MedDCheckState()

        data class MissingCheckField(val appointment: Appointment?) : MedDCheckState()

        data class MedDCheckAutoRunning(val appointment: Appointment?) : MedDCheckState()

        data class CopayResponse(val medDInfo: MedDInfo?) : MedDCheckState()
    }

    private val _medDCheckUIState = MutableStateFlow<State>(Reset)
    val medDCheckUIState: StateFlow<State> = _medDCheckUIState

    fun getLatestMedDInfoAndForceUpdate() {
        partDService.forceRefresh()
        viewModelScope.launch(dispatcherProvider.io) {
            getMedDInfoAndSetState(false)
        }
    }

    private suspend fun CoroutineScope.getMedDInfoAndSetState(shouldSelfCancel: Boolean) {
        partDService.medDInfo.cancellable().collectLatest {
            val state = getStateFromMedDResponseAndCacheResponse(it)
            if (lastMedDResponse == null) {
                viewModelScope.launch {
                    partDService.updateMedDInfo(
                        MedDInfo(
                            eligible = false,
                            copays = listOf()
                        )
                    )
                }
            }
            setMedDCheckState(state)
            if (shouldSelfCancel) {
                this.coroutineContext.job.cancel()
            }
        }
    }

    fun updatePartDResponseFromEvent(appointmentId: Int, intent: Intent) {
        viewModelScope.launch {
            val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)
            partDService.updatePartDResponseFromEvent(
                appointmentId = appointmentId,
                intent = intent,
                medDCta = appointment?.getMedDCta()?.javaClass?.simpleName,
                riskAssessmentId = appointment?.encounterState?.medDMessage?.riskAssessmentId
            )
            getMedDInfoAndSetState(true)
        }
    }

    private var lastMedDResponse: MedDInfo? = null

    init {
        loadingStates.add(MedDCheckState.Loading)
    }

    /**
     * For MedD flow - we attempt to automatically do the check without user input. We will do
     * the check if we already have a value for SSN (or MBI if the MBI FF is on)
     *
     * @param appointmentId appointmentId to run the check
     * @param isCheckMbi flag indicating the MBI FF is on
     */
    fun runMedDCheckIfAvailable(appointmentId: Int, isCheckMbi: Boolean) {
        viewModelScope.launch {
            val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)

            if (appointment != null) {
                val (mbi, ssn) = appointment.patient.paymentInformation?.mbi to appointment.patient.ssn

                when {
                    isCheckMbi -> when {
                        mbi.isMbiFormatAndNotNull() -> runMedDCheckWithMBI(appointment, mbi)
                        ssn.isMaskedSsnFormatted() -> runMedDCheckWithSSN(appointment, ssn)
                        else ->
                            _medDCheckUIState.value =
                                MedDCheckState.MissingCheckField(appointment)
                    }

                    ssn.isFullSsnAndNotNull() -> runMedDCheckWithSSN(appointment, ssn)

                    else -> setMedDCheckState(MedDCheckState.MissingCheckField(appointment))
                }
            } else {
                setMedDCheckState(MedDCheckState.MissingCheckField(null))
            }
        }
    }

    private fun setMedDCheckState(state: State) {
        _medDCheckUIState.value = state
        idlingResource?.setIdle(state !in loadingStates)
    }

    private fun runMedDCheckWithMBI(appointment: Appointment, mBI: String) {
        doWorkAndIdle {
            doMedDCheckAsync(
                appointmentId = appointment.id,
                requestBody = MedDCheckRequestBody(
                    ssn = null,
                    mbi = mBI.trimDashes()
                ),
                errorState = MedDCheckState.MissingCheckField(appointment)
            )
        }

        analytics.saveMetric(
            MedDCheckTriggeredMetric(
                patientVisitId = appointment.id,
                medDIDType = MBI,
                triggeredMode = MANUALLY
            )
        )

        setMedDCheckState(MedDCheckState.MedDCheckAutoRunning(appointment))
    }

    private fun runMedDCheckWithSSN(appointment: Appointment, sSN: String) {
        doWorkAndIdle {
            doMedDCheckAsync(
                appointmentId = appointment.id,
                requestBody = MedDCheckRequestBody(
                    ssn = sSN.trimDashes(),
                    mbi = null
                ),
                errorState = MedDCheckState.MissingCheckField(appointment)
            )
        }

        analytics.saveMetric(
            MedDCheckTriggeredMetric(
                patientVisitId = appointment.id,
                medDIDType = SSN,
                triggeredMode = MANUALLY
            )
        )

        setMedDCheckState(MedDCheckState.MedDCheckAutoRunning(appointment))
    }

    /**
     * Execute work asynchronously with a delay and timeout.
     * This is a simulated "timeout" for flows when we are reliant on the backend response or
     * a Firebase Cloud Messaging Event (in this case ACE) before moving forward.
     * After a delay @see timeout - the state will be set to the timeoutState. This state should be
     * mutated inside the work block.
     *
     * @param interimState optional state to set before the work is executed
     * @param work work to be executed
     */
    private fun doWorkAndIdle(interimState: State? = null, work: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch {
            timeoutState = MedDCheckState.TimeoutResponse
            interimState?.let { setMedDCheckState(it) }
            launch(context = dispatcherProvider.io, block = work)
            delay(textChangeTimeout)
            setMedDCheckState(MedDCheckState.TextChangeTimeout)
            delay(timeout)
            timeoutState?.let { setMedDCheckState(it) }
        }

    fun doMedDCheck(appointmentId: Int, inputValue: String) {
        val requestBody = when {
            inputValue.isMbiFormatAndNotNull() -> {
                analytics.saveMetric(
                    MedDCheckTriggeredMetric(
                        patientVisitId = appointmentId,
                        medDIDType = MBI,
                        triggeredMode = MANUALLY
                    )
                )

                MedDCheckRequestBody(
                    ssn = null,
                    mbi = inputValue.trimDashes()
                )
            }

            inputValue.isFullSsnAndNotNull() -> {
                analytics.saveMetric(
                    MedDCheckTriggeredMetric(
                        patientVisitId = appointmentId,
                        medDIDType = SSN,
                        triggeredMode = MANUALLY
                    )
                )

                MedDCheckRequestBody(
                    ssn = inputValue.trimDashes(),
                    mbi = null
                )
            }

            else -> null
        }

        doWorkAndIdle(MedDCheckState.Loading) {
            doMedDCheckAsync(
                appointmentId = appointmentId,
                requestBody = requestBody,
                errorState = MedDCheckState.MedDCheckFailed
            )
        }
    }

    fun saveMedDCheckRunMetric(
        appointmentId: Int,
        medDInfo: MedDInfo?,
        resultsUnavailable: Boolean,
        displayedMessage: String? = null,
        medDCheckStartedAt: String?,
        checkOutContext: Int
    ) {
        analytics.saveMetric(
            MedDCheckRunMetric(
                visitId = appointmentId,
                checkContext = MedDCheckRunMetric.CheckContext.fromInt(checkOutContext).displayName,
                validResultReturned = medDInfo != null,
                patientMedDCovered = medDInfo?.eligible ?: false,
                resultsUnavailable = resultsUnavailable,
                copays = medDInfo?.copays,
                displayedMessage = displayedMessage,
                medDCheckStartedAt = medDCheckStartedAt ?: medDAutoRunCheckStart
            )
        )
    }

    private suspend fun doMedDCheckAsync(
        appointmentId: Int,
        requestBody: MedDCheckRequestBody?,
        errorState: State
    ) {
        try {
            medDAutoRunCheckStart = LocalDateTime.now().toString()
            appointmentRepository.doMedDCheck(appointmentId, requestBody!!)
        } catch (e: Exception) {
            Timber.e(e, "There was a problem doing the medD check")
            timeoutState = errorState
            setMedDCheckState(errorState)
        }
    }

    fun updateAppointmentDetails(appointmentId: Int, medDInfo: MedDInfo?) =
        viewModelScope.launch(dispatcherProvider.io) {
            timeoutState = null
            appointmentRepository
                .getAppointmentByIdAsync(appointmentId)
                ?.encounterState
                ?.medDMessage
                ?.let { medDMessage ->
                    analytics.saveMetric(
                        CheckoutAppointmentMedDStatusMetric(
                            visitId = appointmentId,
                            dateOfService = LocalDateTime.now(),
                            risk = medDMessage.status,
                            appointmentChangeReason = "Part D Event",
                            medDCta = medDMessage.callToAction?.javaClass?.simpleName,
                            medDRiskAssessmentId = medDMessage.riskAssessmentId
                        )
                    )
                }

            setMedDCheckState(MedDCheckState.CopayResponse(medDInfo))
        }

    private fun getStateFromMedDResponseAndCacheResponse(response: MedDInfo?): State {
        lastMedDResponse = response
        return if (lastMedDResponse != null) {
            MedDCheckState.CopayResponse(lastMedDResponse)
        } else {
            MedDCheckState.MedDCheckFailed
        }
    }

    fun checkForMedDCheckFinished() {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                partDService.forceRefresh()
                partDService.medDInfo.collectLatest { response ->
                    timeoutState = null
                    if (response == null) {
                        doWorkAndIdle { }
                    } else {
                        val state = getStateFromMedDResponseAndCacheResponse(response)
                        setMedDCheckState(state)
                    }
                }
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                setMedDCheckState(MedDCheckState.MedDCheckFailed)
            }
        }
    }

    fun reportState(appointmentId: Int, state: MedDCheckState?) {
        analytics.saveMetric(MedDStateMetric(appointmentId, state))
    }

    fun reportAceReceived(
        appointmentChangeReason: AppointmentChangeReason,
        appointmentChangeType: AppointmentChangeType?,
        appointmentId: Int,
    ) {
        analytics.saveMetric(
            AceReceivedMetric(
                appointmentChangeReason,
                appointmentChangeType,
                appointmentId
            )
        )
    }
}
