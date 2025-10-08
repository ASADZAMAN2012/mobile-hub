/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.ViewModel
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocationDataViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    fun getLocation() = locationRepository.get()

    suspend fun getFeatureFlagsAsync(): List<FeatureFlag> {
        return locationRepository.getFeatureFlagsAsync()
    }

    suspend fun getFeatureFlagByName(featureFlagConstant: FeatureFlagConstant): FeatureFlag? {
        return locationRepository.getFeatureFlagByConstant(featureFlagConstant)
    }
}
