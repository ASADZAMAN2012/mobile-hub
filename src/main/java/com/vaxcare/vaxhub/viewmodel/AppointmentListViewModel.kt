/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant.FeatureAddAppt3
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant.RightPatientRightDose
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.ClinicRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.OrdersRepository
import com.vaxcare.vaxhub.repository.OrdersRepository.Companion.SyncContextFrom.SCHEDULE
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.CHECKOUT_PATIENT
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase.InitialScreen.DOB_CAPTURE
import com.vaxcare.vaxhub.viewmodel.AppointmentListViewModel.AppointmentListState.SyncError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AppointmentListViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val clinicRepository: ClinicRepository,
    private val locationRepository: LocationRepository,
    private val ordersRepository: OrdersRepository,
    private val productRepository: ProductRepository,
    private val localStorage: LocalStorage,
    private val dispatcherProvider: DispatcherProvider,
    private val determinePatientCheckoutInitialScreenUseCase: DeterminePatientCheckoutInitialScreenUseCase
) : BaseViewModel() {
    sealed class AppointmentListState : State {
        data class NavigateToCheckoutPatient(val appointmentId: Int) : AppointmentListState()

        data class NavigateToDoBCapture(val appointmentId: Int, val patientId: Int) :
            AppointmentListState()

        data class AppointmentsLoaded(
            val appointmentList: List<Appointment>,
            val clinic: Clinic?,
            val patientSchedule: Int,
            val errorSyncing: Boolean,
            val isAddAppt3: Boolean
        ) : AppointmentListState()

        data class SyncError(val time: LocalDateTime = LocalDateTime.now()) : AppointmentListState()
    }

    /**
     * Ported from AppointmentViewModel
     *
     * @return - dates that have a scheduled appointment
     */
    fun getAllDatesThatHaveAppointments(): LiveData<Set<LocalDate>> {
        return appointmentRepository.getAll().map {
            it.map { appointment -> appointment.appointmentTime.toLocalDate() }.toSet()
        }
    }

    fun getFeatureFlags() = locationRepository.getFeatureFlags()

    fun onUncheckedOutAppointmentSelected(appointment: Appointment) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (determinePatientCheckoutInitialScreenUseCase(appointment)) {
                CHECKOUT_PATIENT -> setState(
                    AppointmentListState.NavigateToCheckoutPatient(appointment.id)
                )

                DOB_CAPTURE -> setState(
                    AppointmentListState.NavigateToDoBCapture(
                        appointment.id,
                        appointment.patient.id
                    )
                )
            }
        }
    }

    fun syncClinicAppointmentsByDate(date: LocalDate) =
        viewModelScope.launch(dispatcherProvider.io) {
            if (dateInSyncRange(date)) {
                syncAll(date)
            }
        }

    private fun dateInSyncRange(date: LocalDate): Boolean {
        val now = LocalDate.now()
        return now.minusDays(90).isBefore(date) && now.plusDays(90).isAfter(date)
    }

    fun updatePatientSchedule(patientSchedule: Int, date: LocalDate) {
        viewModelScope.launch {
            try {
                setState(LoadingState)

                localStorage.patientSchedule = patientSchedule

                val clinic = clinicRepository.getCurrentClinic()
                val data = appointmentRepository.findAppointmentsByDate(date)
                val flags = locationRepository.getFeatureFlagsAsync()
                val addAppt3 = isFlagEnabled(FeatureAddAppt3, flags)

                if (data.isEmpty()) {
                    syncAll(date)
                } else {
                    filterAndSetState(data, patientSchedule, clinic, false, addAppt3)
                }
            } catch (e: Exception) {
                setState(SyncError())
                Timber.e(e, "Exception")
            }
        }
    }

    private suspend fun syncAll(date: LocalDate) =
        withContext(dispatcherProvider.io) {
            try {
                setState(LoadingState)

                var showError = false
                var isAddAppt3 = false
                val clinic = clinicRepository.getCurrentClinic()
                val patientSchedule = localStorage.patientSchedule

                supervisorScope {
                    val ordersCall = async {
                        val flags = locationRepository.getFeatureFlagsAsync()
                        isAddAppt3 = isFlagEnabled(FeatureAddAppt3, flags)
                        if (isFlagEnabled(RightPatientRightDose, flags)) {
                            ordersRepository.syncOrdersChanges(syncContextFrom = SCHEDULE)
                        }
                    }

                    try {
                        // ignore exception
                        ordersCall.await()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing orders")
                    }

                    val appointmentsCall =
                        async { appointmentRepository.getAppointmentsByDate(date) }
                    try {
                        appointmentsCall.await()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing appointments")
                        showError = true
                    }
                }

                val data = appointmentRepository.findAppointmentsByDate(date)
                filterAndSetState(data, patientSchedule, clinic, showError, isAddAppt3)
            } catch (e: Exception) {
                Timber.e(e, "Error getting clinic")
                setState(SyncError())
            }
        }

    private suspend fun isFlagEnabled(feature: FeatureFlagConstant, flags: List<FeatureFlag>?): Boolean {
        val features = flags ?: run { locationRepository.getFeatureFlagsAsync() }
        return features.any { it.featureFlagName == feature.value }
    }

    /**
     * Run appointment list through feature flag filters before notifying UI state of
     * appointment list
     *
     * @param data
     * @param patientSchedule
     * @param clinic
     * @param showError
     * @param isAddAppt3 will filter out appointments with LARC checkouts
     */
    private suspend fun filterAndSetState(
        data: List<Appointment>,
        patientSchedule: Int,
        clinic: Clinic?,
        showError: Boolean,
        isAddAppt3: Boolean
    ) {
        val larcProducts = productRepository.getLarcProductIDs()
        val appointmentList = when {
            patientSchedule == 1 -> {
                data.filter { !it.checkedOut }
            }

            isAddAppt3 -> {
                val abandonedIds =
                    appointmentRepository.getAllAbandonedAppointments().map { it.appointmentId }
                data.filter {
                    it.id !in abandonedIds && it.administeredVaccines.none { vaccine ->
                        vaccine.productId in larcProducts
                    }
                }
            }

            else -> data
        }

        setState(
            AppointmentListState.AppointmentsLoaded(
                appointmentList,
                clinic,
                patientSchedule,
                showError,
                isAddAppt3
            )
        )
    }
}
