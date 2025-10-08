/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.domain.UploadAppointmentMediaUseCase
import com.vaxcare.vaxhub.domain.partd.ConvertPartDCopayToProductUseCase
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.toMedDInfo
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PatientRepository
import com.vaxcare.vaxhub.repository.ProviderRepository
import com.vaxcare.vaxhub.repository.ShotAdministratorRepository
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.web.PatientsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CheckoutSummaryViewModel @Inject constructor(
    appointmentRepository: AppointmentRepository,
    locationRepository: LocationRepository,
    userRepository: UserRepository,
    localStorage: LocalStorage,
    patientsApi: PatientsApi,
    @MHAnalyticReport analytics: AnalyticReport,
    private val shotAdministratorRepository: ShotAdministratorRepository,
    private val patientRepository: PatientRepository,
    uploadAppointmentMediaUseCase: UploadAppointmentMediaUseCase,
    dispatcherProvider: DispatcherProvider,
    private val providerRepository: ProviderRepository,
    private val convertCopayToProduct: ConvertPartDCopayToProductUseCase
) : BaseCheckoutSummaryViewModel(
        appointmentRepository = appointmentRepository,
        locationRepository = locationRepository,
        userRepository = userRepository,
        localStorage = localStorage,
        patientsApi = patientsApi,
        analytics = analytics,
        uploadAppointmentMediaUseCase = uploadAppointmentMediaUseCase,
        dispatcherProvider = dispatcherProvider
    ) {
    suspend fun retrieveMedDCopays(appointmentId: Int): MedDInfo? =
        try {
            patientRepository
                .getMedDCopays(appointmentId).body()?.toMedDInfo(
                    notFoundMessageResId = null,
                    notCoveredMessageResId = null,
                    converter = convertCopayToProduct::invoke
                )
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    suspend fun getAppointmentAsync(appointmentId: Int) = appointmentRepository.getAppointmentByIdAsync(appointmentId)

    suspend fun getShotAdminsAsync() = shotAdministratorRepository.getAllAsync()

    fun getShotAdminsLiveData() = shotAdministratorRepository.getAll()

    suspend fun getProvidersAsync() = providerRepository.getAllAsync()
}
