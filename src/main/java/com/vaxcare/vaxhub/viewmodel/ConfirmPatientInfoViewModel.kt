/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfirmPatientInfoViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    @MHAnalyticReport private val analytics: AnalyticReport,
    private val providerRepository: ProviderRepository,
    private val localStorage: LocalStorage,
) : BaseViewModel() {
    private var _currentProvider: MutableLiveData<Provider?> = MutableLiveData(null)
    var currentProvider: LiveData<Provider?> = _currentProvider

    init {
        setState(ConfirmPatientInfoUIState.Loading())
        loadCurrentProvider()
    }

    private var broadcastReceiverTimeoutJob: Job? = null

    @TestOnly
    fun loadCurrentProvider() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = localStorage.userId
            val currentProvider: Provider? =
                providerRepository.getByIdAsync(currentUserId) ?: providerRepository.getAllAsync()
                    .firstOrNull()

            currentProvider?.let {
                _currentProvider.postValue(it)
                setState(ConfirmPatientInfoUIState.Init)
            } ?: setState(ConfirmPatientInfoUIState.NoProvidersFoundError)
        }
    }

    fun updateCurrentProvider(currentProvider: Provider) {
        _currentProvider.postValue(currentProvider)
    }

    fun createAppointmentWithNewPatientAndWaitForACE(newPatient: PatientPostBody.NewPatient) {
        viewModelScope.launch(Dispatchers.IO) {
            setState(ConfirmPatientInfoUIState.Loading(R.string.fragment_confirm_patient_info_please_wait))
            try {
                val newAppointmentId = createAppointmentAndGetId(newPatient)
                setState(
                    ConfirmPatientInfoUIState.ListenForAppointmentChangedEvent(
                        newAppointmentId
                    )
                )
                startEligibilityTimeOutToProcessNewAppointment(newAppointmentId)
            } catch (e: Exception) {
                Timber.e(e, "Couldn't create appointment with new patient with $newPatient")
                setState(ConfirmPatientInfoUIState.NoInternetConnectivity)
            }
        }
    }

    @Suppress("KotlinConstantConditions")
    private fun startEligibilityTimeOutToProcessNewAppointment(newAppointmentId: Int) {
        broadcastReceiverTimeoutJob = viewModelScope.launch {
            delay(10_000L)

            checkAndResolveReceiverStatus(newAppointmentId)

            if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "qa") {
                delay(30_000L)
            } else {
                delay(10_000L)
            }

            checkAndResolveReceiverStatus(newAppointmentId)
        }
    }

    private fun checkAndResolveReceiverStatus(newAppointmentId: Int) {
        when (state.value) {
            is ConfirmPatientInfoUIState.ListenForAppointmentChangedEvent -> {
                setState(ConfirmPatientInfoUIState.Loading(R.string.taking_a_few_more_seconds))
            }

            is ConfirmPatientInfoUIState.Loading -> {
                triggerForceRiskFree(newAppointmentId)
            }

            else -> {
                Timber.e("Unexpected state while listening for ACE event")
            }
        }
    }

    private fun triggerForceRiskFree(newAppointmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appointmentRepository.fetchAndUpsertAppointmentAsRiskFree(newAppointmentId)
                setState(
                    ConfirmPatientInfoUIState.NavigateToCheckoutPatientAsRiskFree(
                        newAppointmentId
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Error fetching new appointment previously created")
                setState(ConfirmPatientInfoUIState.NoInternetConnectivity)
            }
        }
    }

    fun stopTimeoutAndNotifyToNavigateToCheckoutPatient(appointmentId: Int) {
        broadcastReceiverTimeoutJob?.cancel()

        setState(
            ConfirmPatientInfoUIState.NavigateToCheckoutPatientWithEligibilityCheckReceived(
                appointmentId
            )
        )
    }

    fun isExpectedAppointmentChangeEvent(intent: Intent, currentAppointmentId: Int): Boolean {
        val receivedAppointmentId = intent.extras?.getInt(Receivers.ACE_APPOINTMENT_ID)
        val appointmentChangedReason = intent.extras?.getInt(Receivers.ACE_CHANGE_REASON)
            ?.let { AppointmentChangeReason.fromInt(it) }
        return receivedAppointmentId == currentAppointmentId &&
            appointmentChangedReason == AppointmentChangeReason.RiskUpdated
    }

    private suspend fun createAppointmentAndGetId(newPatient: PatientPostBody.NewPatient): Int {
        val getInitialPaymentMode = when (newPatient.paymentInformation?.primaryInsuranceId) {
            Payer.PayerType.EMPLOYER.id -> "EmployerPay"
            Payer.PayerType.SELF.id -> "SelfPay"
            Payer.PayerType.OTHER.id,
            Payer.PayerType.UNINSURED.id -> "PartnerBill"

            else -> "InsurancePay"
        }

        return appointmentRepository.postAppointmentWithUTCZoneOffsetAndGetId(
            PatientPostBody(
                newPatient = newPatient,
                clinicId = localStorage.clinicId,
                providerId = currentProvider.value?.id,
                initialPaymentMode = getInitialPaymentMode,
                visitType = "Well"
            )
        ).toInt()
    }

    fun retrieveProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            setState(ConfirmPatientInfoUIState.Loading())
            try {
                setState(
                    ConfirmPatientInfoUIState.SelectAProvider(
                        providerRepository.getAllAsync(),
                        currentProvider.value
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "No providers found")
                setState(
                    ConfirmPatientInfoUIState.NoProvidersFoundError
                )
            }
        }
    }

    fun saveMetric(metric: BaseMetric) {
        analytics.saveMetric(metric)
    }
}

sealed interface ConfirmPatientInfoUIState : State {
    object Init : ConfirmPatientInfoUIState

    object NoInternetConnectivity : ConfirmPatientInfoUIState

    object NoProvidersFoundError : ConfirmPatientInfoUIState

    data class ListenForAppointmentChangedEvent(val appointmentId: Int) :
        ConfirmPatientInfoUIState

    data class NavigateToCheckoutPatientAsRiskFree(val appointmentId: Int) :
        ConfirmPatientInfoUIState

    data class NavigateToCheckoutPatientWithEligibilityCheckReceived(val appointmentId: Int) :
        ConfirmPatientInfoUIState

    data class Loading(
        @StringRes val loadingMessage: Int? = null
    ) : ConfirmPatientInfoUIState

    data class SelectAProvider(
        val providers: List<Provider>,
        val currentProvider: Provider?
    ) : ConfirmPatientInfoUIState
}
