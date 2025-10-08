/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.extension.getStartOfDay
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.SearchPatient
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.ClinicRepository
import com.vaxcare.vaxhub.repository.PatientRepository
import com.vaxcare.vaxhub.ui.checkout.adapter.PatientSearchResultAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class PatientSearchViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val clinicRepository: ClinicRepository,
    private val patientRepository: PatientRepository,
    private val localStorage: LocalStorage
) : BaseViewModel() {
    companion object {
        const val DAYS_PER_WEEK = 7L
    }

    sealed class PatientSearchState : State {
        data class FilterSearchResultState(
            val query: String,
            val value: List<PatientSearchResultAdapter.PatientSearchWrapper>
        ) : PatientSearchState()

        data class AppointmentsByDateAsyncState(val appointments: List<Appointment>) :
            PatientSearchState()
    }

    fun filterSearchResults(query: String) =
        viewModelScope.launch(Dispatchers.IO) {
            setState(LoadingState)
            val fetchDayAppointments = async {
                getAppointmentsByIdentifierAsync(query, DAYS_PER_WEEK)
            }
            val fetchSearchPatients = async {
                searchPatients(query)
            }
            val dayAppointments = fetchDayAppointments.await()
            var searchPatients = fetchSearchPatients.await()
            val todayPatientIds = dayAppointments
                .filter { it.appointmentTime.toLocalDate().isEqual(LocalDate.now()) }
                .map { it.patient.id }
            searchPatients = searchPatients.filter { !todayPatientIds.contains(it.id) }

            val wrappers = mutableListOf<PatientSearchResultAdapter.PatientSearchWrapper>()
            wrappers.addAll(
                dayAppointments.map {
                    PatientSearchResultAdapter.PatientSearchWrapper(
                        SearchPatient.convertPatient(it.patient),
                        it
                    )
                }
            )
            wrappers.addAll(searchPatients.map { PatientSearchResultAdapter.PatientSearchWrapper(it) })
            val today = LocalDateTime.now()

            val futureAppts = mutableListOf<PatientSearchResultAdapter.PatientSearchWrapper>()
            val noAppts = mutableListOf<PatientSearchResultAdapter.PatientSearchWrapper>()
            val todayAndPast = mutableListOf<PatientSearchResultAdapter.PatientSearchWrapper>()
            wrappers.forEach {
                when {
                    it.appointment == null -> noAppts.add(it)
                    it.appointment.appointmentTime.toLocalDate()
                        .isAfter(today.toLocalDate()) -> futureAppts.add(it)
                    else -> todayAndPast.add(it)
                }
            }

            coroutineScope {
                awaitAll(
                    async { todayAndPast.sortByDescending { it.appointment!!.appointmentTime } },
                    async { futureAppts.sortBy { it.appointment?.appointmentTime } }
                )
            }

            setState(
                PatientSearchState.FilterSearchResultState(
                    query,
                    todayAndPast + futureAppts + noAppts
                )
            )
        }

    private suspend fun getAppointmentsByIdentifierAsync(identifier: String, dateRange: Long = 0): List<Appointment> {
        val pair = appointmentDateRange(dateRange)
        return appointmentRepository.getAppointmentsByIdentifierAsync(
            identifier,
            pair.first,
            pair.second
        )
    }

    private suspend fun appointmentDateRange(dayRange: Long = 0): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val clinic = clinicRepository.getCurrentClinic()
        val date = if (clinic != null && clinic.isTemporaryClinic() && clinic.startDate != null) {
            clinic.startDate
        } else {
            LocalDate.now(zoneId)
        }
        val startDate = date.minusDays(dayRange).atStartOfDay(zoneId)
        val endDate = date.plusDays(dayRange).atStartOfDay(zoneId)
        return Pair(startDate.toInstant().toEpochMilli(), endDate.toInstant().toEpochMilli())
    }

    private suspend fun searchPatients(queryString: String): List<SearchPatient> {
        return try {
            patientRepository.searchPatients(queryString)
        } catch (e: Exception) {
            listOf()
        }
    }

    fun filterSearchResultsByAddNewAppointment(query: String, appointmentList: List<Appointment>? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            setState(LoadingState)
            val fetchTodayPatientIds = async {
                val appointments = appointmentList ?: checkIfNeedForceFetchAppointments()
                appointments?.map { it.patient.id }
            }
            val fetchSearchPatients = async {
                searchPatients(query)
            }
            val todayPatientIds = fetchTodayPatientIds.await()
            var searchPatients = fetchSearchPatients.await()
            if (todayPatientIds != null) {
                searchPatients = searchPatients.filter { !todayPatientIds.contains(it.id) }
            }
            val result = searchPatients.map { PatientSearchResultAdapter.PatientSearchWrapper(it) }
            setState(PatientSearchState.FilterSearchResultState(query, result))
        }

    private suspend fun checkIfNeedForceFetchAppointments(): List<Appointment>? {
        getClinicAppointmentsByToday()
        return getAppointmentsByDateAsync()
    }

    private suspend fun getClinicAppointmentsByToday() {
        try {
            val clinic = clinicRepository.getCurrentClinic()
            val date =
                if (clinic != null && clinic.isTemporaryClinic() && clinic.startDate != null) {
                    clinic.startDate
                } else {
                    LocalDate.now()
                }

            val appointments = patientRepository.getClinicAppointmentsByDate(
                localStorage.currentClinicId.toInt(),
                date
            ).map { it.toAppointment() }
            appointmentRepository.upsertAppointments(appointments)
            localStorage.lastFetchAppointmentsMillisecond = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getAppointmentsByDateAsync(today: LocalDateTime? = null): List<Appointment> {
        val date = today ?: LocalDateTime.now()
        val startDate = date.getStartOfDay()
        val endDate = startDate.plus(1, ChronoUnit.DAYS)
        val zone = ZoneId.systemDefault()
        val result = appointmentRepository.getAllByAppointmentAsync(
            startDate.atZone(zone).toInstant().toEpochMilli(),
            endDate.atZone(zone).toInstant().toEpochMilli()
        )
        setState(PatientSearchState.AppointmentsByDateAsyncState(result))
        return result
    }

    fun preFetchAppointments() =
        viewModelScope.launch(Dispatchers.IO) {
            getClinicAppointmentsByToday()
            getAppointmentsByDateAsync()
        }
}
