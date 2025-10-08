/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.isInsurancePhoneCaptureDisabled
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutCollectInsuranceViewModel @Inject constructor(
    val locationRepository: LocationRepository
) : BaseViewModel() {
    fun noInsuranceCardSelected() {
        setState(LoadingState)
        viewModelScope.launch(Dispatchers.IO) {
            if (locationRepository.getFeatureFlagsAsync().isInsurancePhoneCaptureDisabled()) {
                setState(CheckoutCollectInsuranceState.NavigateToSummary)
            } else {
                setState(CheckoutCollectInsuranceState.NavigateToPhoneCaptureFlow)
            }
        }
    }

    fun getOptOutButtonWording() {
        setState(LoadingState)
        viewModelScope.launch(Dispatchers.IO) {
            if (locationRepository.getFeatureFlagsAsync().isInsurancePhoneCaptureDisabled()) {
                setState(CheckoutCollectInsuranceState.CashCheckOption)
            } else {
                setState(CheckoutCollectInsuranceState.NoCardInsuranceOption)
            }
        }
    }
}

sealed class CheckoutCollectInsuranceState : State {
    object NavigateToPhoneCaptureFlow : CheckoutCollectInsuranceState()

    object NavigateToSummary : CheckoutCollectInsuranceState()

    object CashCheckOption : CheckoutCollectInsuranceState()

    object NoCardInsuranceOption : CheckoutCollectInsuranceState()
}
