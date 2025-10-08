/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.ViewModel
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    locationRepository: LocationRepository,
    localStorage: LocalStorage
) : ViewModel() {
    val clinicName = locationRepository.getClinicName()

    val deviceSerialNumber = localStorage.deviceSerialNumber
}
