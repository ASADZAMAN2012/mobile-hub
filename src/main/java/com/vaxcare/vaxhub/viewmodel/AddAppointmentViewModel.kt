/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.IntegrationType.BI
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PatientRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.ui.checkout.adapter.AddAppointmentResultAdapter
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.DOB_CAPTURE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddAppointmentViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val determinePatientCheckoutInitialScreenUseCase: DeterminePatientCheckoutInitialScreenUseCase
) : BaseViewModel() {
    var searchDate: LocalDate = LocalDate.now()

    private val _isIntegrationTypeBi: MutableLiveData<Boolean> = MutableLiveData(false)
    val isIntegrationTypeBi: LiveData<Boolean> = _isIntegrationTypeBi

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            _isIntegrationTypeBi.postValue(locationRepository.getLocationAsync()?.integrationType == BI)
        }
    }

    sealed class AddAppointmentUiState : State {
        data class FeatureFlagsLoaded(val flags: List<FeatureFlag>) : AddAppointmentUiState()

        data class FilterUiResultStateAdd(
            val query: String,
            val value: List<AddAppointmentResultAdapter.ResultWrapper>
        ) : AddAppointmentUiState()

        data class NavigateToCheckoutPatient(val appointmentId: Int) : AddAppointmentUiState()

        data class NavigateToDoBCapture(val appointmentId: Int, val patientId: Int) :
            AddAppointmentUiState()
    }

    fun loadFeatureFlags() =
        viewModelScope.launch(dispatcherProvider.io) {
            val flags = locationRepository.getFeatureFlagsAsync()
            setState(AddAppointmentUiState.FeatureFlagsLoaded(flags))
        }

    fun onUncheckedOutAppointmentSelected(appointment: Appointment) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (determinePatientCheckoutInitialScreenUseCase(appointment)) {
                CHECKOUT_PATIENT -> setState(
                    AddAppointmentUiState.NavigateToCheckoutPatient(
                        appointmentId = appointment.id
                    )
                )

                DOB_CAPTURE -> setState(
                    AddAppointmentUiState.NavigateToDoBCapture(
                        appointmentId = appointment.id,
                        patientId = appointment.patient.id
                    )
                )
            }
        }
    }

    fun filterSearchResults(query: String, isOnline: Boolean) =
        viewModelScope.launch(dispatcherProvider.io) {
            setState(LoadingState)
            val fetchAppointments = async {
                getAppointmentsByIdentifierAsync(query)
            }
            val fetchPatients = async {
                if (isOnline) {
                    searchPatients(query)
                } else {
                    emptyList()
                }
            }

            val appointmentResults = fetchAppointments.await().toMutableList()
            val patientResults = fetchPatients.await().toMutableList()

            val larcProductIDs = productRepository.getLarcProductIDs()

            val abandonedIds =
                appointmentRepository.getAllAbandonedAppointments().map { it.appointmentId }

            val filteredAppointments = appointmentResults.filter {
                !it.checkedOut && it.id !in abandonedIds && it.administeredVaccines.none { vaccine ->
                    vaccine.productId in larcProductIDs
                }
            }.sortedByDescending { it.appointmentTime }

            val appointmentWrappers = filteredAppointments.map {
                AddAppointmentResultAdapter.ResultWrapper.AppointmentResult(it)
            }
            val patientWrappers = patientResults.map {
                AddAppointmentResultAdapter.ResultWrapper.PatientResult(it)
            }

            val results = mutableListOf<AddAppointmentResultAdapter.ResultWrapper>()
            results.addAll(appointmentWrappers)
            results.addAll(patientWrappers)

            setState(AddAppointmentUiState.FilterUiResultStateAdd(query, results))
        }

    private suspend fun getAppointmentsByIdentifierAsync(identifier: String): List<Appointment> {
        val zoneId = ZoneId.systemDefault()
        val startTime = searchDate.atStartOfDay(zoneId)
        val endTime = searchDate.plusDays(1).atStartOfDay(zoneId)

        return appointmentRepository.getAppointmentsByIdentifierAsync(
            identifier,
            startTime.toInstant().toEpochMilli(),
            endTime.toInstant().toEpochMilli()
        )
    }

    private suspend fun searchPatients(queryString: String): List<SearchPatient> {
        return try {
            patientRepository.searchPatients(queryString)
        } catch (e: Exception) {
            listOf()
        }
    }
}
