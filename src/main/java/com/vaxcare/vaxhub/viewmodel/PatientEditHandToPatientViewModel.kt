/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientEditHandToPatientViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : BaseViewModel() {
    sealed class PatientEditHandToPatientState : State {
        object LockedSession : PatientEditHandToPatientState()

        object OpenSession : PatientEditHandToPatientState()
    }

    fun startCollectionFlow() =
        viewModelScope.launch(Dispatchers.IO) {
            val state = locationRepository
                .getFeatureFlagByConstant(FeatureFlagConstant.FeatureHubLoginUserAndPin)?.let {
                    userRepository.lockSession()
                    PatientEditHandToPatientState.LockedSession
                } ?: PatientEditHandToPatientState.OpenSession

            setState(state)
        }
}
