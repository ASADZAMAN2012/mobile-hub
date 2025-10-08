/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNewPatientViewModel @Inject constructor(
    locationRepository: LocationRepository,
    productRepository: ProductRepository,
    wrongProductRepository: WrongProductRepository,
    @MHAnalyticReport val analyticReport: AnalyticReport,
    private val appointmentRepository: AppointmentRepository
) : BaseScannerViewModel(locationRepository, productRepository, wrongProductRepository) {
    private val _appointment: MutableLiveData<Appointment?> = MutableLiveData<Appointment?>(null)
    val appointment: LiveData<Appointment?> = _appointment

    fun loadAppointment(appointmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)
            _appointment.postValue(appointment)
        }
    }

    fun saveMetric(metric: BaseMetric) {
        analyticReport.saveMetric(metric)
    }
}
