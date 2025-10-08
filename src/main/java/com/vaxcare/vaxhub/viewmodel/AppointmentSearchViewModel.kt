/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.DOB_CAPTURE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AppointmentSearchViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val determinePatientCheckoutInitialScreenUseCase: DeterminePatientCheckoutInitialScreenUseCase
) : BaseViewModel() {
    var searchDate: LocalDate = LocalDate.now()

    sealed class AppointmentSearchState : State {
        data class FeatureFlagsLoaded(val flags: List<FeatureFlag>) : AppointmentSearchState()

        data class FilterSearchResultState(
            val query: String,
            val value: List<Appointment>
        ) : AppointmentSearchState()

        data class NavigateToCheckoutPatient(val appointmentId: Int) : AppointmentSearchState()

        data class NavigateToDoBCapture(val appointmentId: Int, val patientId: Int) :
            AppointmentSearchState()
    }

    fun loadFeatureFlags() =
        viewModelScope.launch(Dispatchers.IO) {
            val flags = locationRepository.getFeatureFlagsAsync()
            setState(AppointmentSearchState.FeatureFlagsLoaded(flags))
        }

    fun filterSearchResults(query: String) =
        viewModelScope.launch(Dispatchers.IO) {
            setState(LoadingState)
            val fetchAppointments = async {
                getAppointmentsByIdentifierAsync(query)
            }

            val appointmentResults = fetchAppointments.await().toMutableList()
            val larcProductIDs = productRepository.getLarcProductIDs()
            val abandonedIds =
                appointmentRepository.getAllAbandonedAppointments().map { it.appointmentId }

            val results = appointmentResults.filter {
                it.id !in abandonedIds && it.administeredVaccines.none { vaccine ->
                    vaccine.productId in larcProductIDs
                }
            }.sortedByDescending { it.appointmentTime }

            setState(AppointmentSearchState.FilterSearchResultState(query, results))
        }

    fun onUncheckedOutAppointmentSelected(appointment: Appointment) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (determinePatientCheckoutInitialScreenUseCase(appointment)) {
                CHECKOUT_PATIENT -> setState(
                    AppointmentSearchState.NavigateToCheckoutPatient(
                        appointmentId = appointment.id
                    )
                )

                DOB_CAPTURE -> setState(
                    AppointmentSearchState.NavigateToDoBCapture(
                        appointmentId = appointment.id,
                        patientId = appointment.patient.id
                    )
                )
            }
        }
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
}
