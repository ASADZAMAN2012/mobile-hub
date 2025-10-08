/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.IntegrationType.BI
import com.vaxcare.vaxhub.model.metric.CheckoutLoginMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutCompleteViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    @MHAnalyticReport private val analyticReport: AnalyticReport,
    private val appointmentRepository: AppointmentRepository,
) : BaseViewModel() {
    sealed class CheckoutCompleteState : State {
        data class FeaturesLoaded(
            val isIntegrationTypeBi: Boolean
        ) : CheckoutCompleteState()
    }

    private val _familyAppointments = MutableLiveData<List<Appointment>>(emptyList())
    val familyAppointments: LiveData<List<Appointment>> = _familyAppointments

    fun loadFeatures() {
        viewModelScope.launch(Dispatchers.IO) {
            val isIntegrationTypeBi = locationRepository.getLocationAsync()?.integrationType == BI
            setState(CheckoutCompleteState.FeaturesLoaded(isIntegrationTypeBi))
        }
    }

    fun reportLogin(
        success: Boolean,
        targetDestination: String,
        appointmentId: Int?
    ) {
        analyticReport.saveMetric(
            CheckoutLoginMetric(
                isSuccess = success,
                targetNavigation = targetDestination,
                appointmentId = appointmentId
            )
        )
    }

    fun loadPotentialFamilyAppointments(appointment: Appointment) {
        viewModelScope.launch(Dispatchers.IO) {
            _familyAppointments.postValue(appointmentRepository.getPotentialFamilyAppointments(appointment))
        }
    }
}
