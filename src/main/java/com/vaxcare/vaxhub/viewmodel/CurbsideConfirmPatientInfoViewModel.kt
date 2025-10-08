/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.data.dao.ProviderDao
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.service.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CurbsideConfirmPatientInfoViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val providerDao: ProviderDao,
    private val locationRepository: LocationRepository,
    private val networkMonitor: NetworkMonitor
) : BaseViewModel() {
    sealed class ConfirmPatientInfoState : State {
        data class PatientLoaded(
            val patient: Patient?,
            val providers: List<Provider>,
            val isAddAppt3: Boolean
        ) : ConfirmPatientInfoState()

        object ProceedToCreateAppointment : ConfirmPatientInfoState()

        object NetworkError : ConfirmPatientInfoState()
    }

    private var patientLoaded: Patient? = null

    fun checkNetworkState() {
        networkMonitor.pingServersAndUpdateNetWorkStatus { networkStatus ->
            setState(LoadingState)
            val state = when (networkStatus) {
                NetworkStatus.CONNECTED -> ConfirmPatientInfoState.ProceedToCreateAppointment
                else -> ConfirmPatientInfoState.NetworkError
            }

            setState(state)
        }
    }

    /**
     * Populate patient and list of Providers for the state
     */
    fun getPatientInfoAndProviders(patientId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            setState(LoadingState)
            val isAddAppt3 = locationRepository.getFeatureFlagsAsync()
                .any { it.featureFlagName == FeatureFlagConstant.FeatureAddAppt3.value }
            val providers = providerDao.getAllAsync()
            try {
                if (patientId >= 0 && patientLoaded == null) {
                    patientLoaded = appointmentRepository.getPatientById(patientId)
                }
            } catch (e: Exception) {
                Timber.e("Failed to fetch patientId $patientId: $e")
            }
            setState(ConfirmPatientInfoState.PatientLoaded(patientLoaded, providers, isAddAppt3))
        }
}
