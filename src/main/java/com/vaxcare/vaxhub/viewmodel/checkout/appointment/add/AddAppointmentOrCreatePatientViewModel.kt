/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel.checkout.appointment.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.isFeaturePublicStockPilotEnabled
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.model.enums.NetworkStatus.CONNECTED
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.ClinicRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PatientRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.checkout.adapter.AddAppointmentResultAdapter
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.DOB_CAPTURE
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.NavigateToCheckoutPatient
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.NavigateToDoBCapture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddAppointmentOrCreatePatientViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val appointmentRepository: AppointmentRepository,
    private val clinicRepository: ClinicRepository,
    @MHAnalyticReport private val analytics: AnalyticReport,
    private val networkMonitor: NetworkMonitor,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val determinePatientCheckoutInitialScreenUseCase: DeterminePatientCheckoutInitialScreenUseCase
) : ViewModel() {
    private val _uiState: MutableLiveData<AddAppointmentOrCreatePatientUIState> =
        MutableLiveData(AddAppointmentOrCreatePatientUIState.Init)
    val uiState: LiveData<AddAppointmentOrCreatePatientUIState> = _uiState

    fun findPatientsWith(keyword: String) =
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.postValue(AddAppointmentOrCreatePatientUIState.Loading)

            if (keyword.isEmpty()) {
                _uiState.postValue(AddAppointmentOrCreatePatientUIState.NoPatientsFound)
            } else {
                try {
                    val patients = patientRepository.searchPatients(keyword)
                    val appointments = getFilteredAppointments(keyword)
                    val larcProductIDs = productRepository.getLarcProductIDs()
                    val abandonedAppointmentIDs =
                        appointmentRepository.getAllAbandonedAppointments().map { it.appointmentId }

                    val filteredAppointments = appointments.filter {
                        !it.checkedOut && it.id !in abandonedAppointmentIDs && it.administeredVaccines.none { vaccine ->
                            vaccine.productId in larcProductIDs
                        }
                    }.sortedByDescending { it.appointmentTime }

                    val results: List<AddAppointmentResultAdapter.ResultWrapper> =
                        createResultsFrom(patients, filteredAppointments)
                    val isFeaturePublicStockPilotEnabled: Boolean =
                        locationRepository.getLocationAsync()?.activeFeatureFlags
                            ?.isFeaturePublicStockPilotEnabled() == true

                    if (results.isNotEmpty()) {
                        _uiState.postValue(
                            AddAppointmentOrCreatePatientUIState.PatientsFound(
                                listOfPatientsFound = results,
                                isFeaturePublicStockPilotEnabled = isFeaturePublicStockPilotEnabled
                            )
                        )
                    } else {
                        _uiState.postValue(AddAppointmentOrCreatePatientUIState.NoPatientsFound)
                    }
                } catch (e: Exception) {
                    Timber.e(e.localizedMessage)
                    _uiState.postValue(AddAppointmentOrCreatePatientUIState.NoPatientsFound)
                }
            }
        }

    fun onUncheckedAppointmentSelected(appointment: Appointment) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (determinePatientCheckoutInitialScreenUseCase(appointment)) {
                CHECKOUT_PATIENT -> _uiState.postValue(
                    NavigateToCheckoutPatient(
                        appointment.id
                    )
                )

                DOB_CAPTURE -> _uiState.postValue(
                    NavigateToDoBCapture(
                        appointmentId = appointment.id,
                        patientId = appointment.patient.id
                    )
                )
            }
        }
    }

    @TestOnly
    suspend fun getFilteredAppointments(keyword: String): List<Appointment> =
        appointmentRepository.getAppointmentsByIdentifierAsync(
            identifier = keyword,
            startDate = rangeOfDaysForAppointments().first,
            endDate = rangeOfDaysForAppointments().second
        )

    private fun createResultsFrom(
        patients: List<SearchPatient>,
        filteredAppointments: List<Appointment>
    ): List<AddAppointmentResultAdapter.ResultWrapper> {
        val appointmentWrappers = filteredAppointments.map {
            AddAppointmentResultAdapter.ResultWrapper.AppointmentResult(it)
        }
        val patientWrappers = patients.map {
            AddAppointmentResultAdapter.ResultWrapper.PatientResult(it)
        }
        return mutableListOf<AddAppointmentResultAdapter.ResultWrapper>().apply {
            addAll(appointmentWrappers)
            addAll(patientWrappers)
        }
    }

    @TestOnly
    suspend fun rangeOfDaysForAppointments(): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val clinic = clinicRepository.getCurrentClinic()
        val startingDate =
            if (clinic != null && clinic.isTemporaryClinic() && clinic.startDate != null) {
                clinic.startDate
            } else {
                LocalDate.now(zoneId)
            }

        val fromDate = startingDate.minusDays(7L).atStartOfDay(zoneId)
        val untilDate = startingDate.plusDays(7L).atStartOfDay(zoneId)

        return Pair(fromDate.toInstant().toEpochMilli(), untilDate.toInstant().toEpochMilli())
    }

    fun createNewPatient() {
        _uiState.postValue(AddAppointmentOrCreatePatientUIState.Loading)
        networkMonitor.pingServersAndUpdateNetWorkStatus { networkStatus ->
            _uiState.postValue(
                when (networkStatus) {
                    CONNECTED -> AddAppointmentOrCreatePatientUIState.NotifyNotHandDeviceToPatient
                    else -> AddAppointmentOrCreatePatientUIState.NoVaxCareConnectivity
                }
            )
        }
    }

    fun saveMetric(metric: BaseMetric) {
        analytics.saveMetric(metric)
    }

    fun resetUIStateToDefault() {
        _uiState.postValue(AddAppointmentOrCreatePatientUIState.Init)
    }
}

sealed interface AddAppointmentOrCreatePatientUIState {
    object Init : AddAppointmentOrCreatePatientUIState

    object Loading : AddAppointmentOrCreatePatientUIState

    object NoPatientsFound : AddAppointmentOrCreatePatientUIState

    object NoVaxCareConnectivity : AddAppointmentOrCreatePatientUIState

    object NotifyNotHandDeviceToPatient : AddAppointmentOrCreatePatientUIState

    data class PatientsFound(
        val listOfPatientsFound: List<AddAppointmentResultAdapter.ResultWrapper>,
        val isFeaturePublicStockPilotEnabled: Boolean
    ) : AddAppointmentOrCreatePatientUIState

    data class NavigateToCheckoutPatient(val appointmentId: Int) :
        AddAppointmentOrCreatePatientUIState

    data class NavigateToDoBCapture(val appointmentId: Int, val patientId: Int) :
        AddAppointmentOrCreatePatientUIState
}
