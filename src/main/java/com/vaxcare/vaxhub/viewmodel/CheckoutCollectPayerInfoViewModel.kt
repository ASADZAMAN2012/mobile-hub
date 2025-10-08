/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.isInsurancePhoneCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isInsuranceScanDisabled
import com.vaxcare.vaxhub.core.extension.isPayorSelectionEnabled
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutCollectPayerInfoViewModel @Inject constructor(
    val locationRepository: LocationRepository
) : BaseViewModel() {
    fun toNextScreen(payer: Payer?) =
        viewModelScope.launch(Dispatchers.IO) {
            val flags = locationRepository.getFeatureFlagsAsync()
            val isSelectPayorEnabled = flags.isPayorSelectionEnabled()
            val isInsuranceScanDisabled = flags.isInsuranceScanDisabled()
            val isInsurancePhoneCollectDisabled = flags.isInsurancePhoneCaptureDisabled()
            val state = if (payer == null) {
                when {
                    isSelectPayorEnabled ->
                        CheckoutCollectPayerUiState.NavigateToSelectPayor

                    !isInsuranceScanDisabled -> CheckoutCollectPayerUiState.NavigateToCollectInsurance

                    !isInsurancePhoneCollectDisabled ->
                        CheckoutCollectPayerUiState.NavigateToPhoneCollectionFlow(null)

                    else -> CheckoutCollectPayerUiState.NavigateToSummaryFragment
                }
            } else if (!isInsurancePhoneCollectDisabled) {
                CheckoutCollectPayerUiState.NavigateToPhoneCollectionFlow(payer)
            } else {
                CheckoutCollectPayerUiState.NavigateToSummaryFragment
            }

            setState(state)
        }

    fun onNoChangeInPatientInsurance(payer: Payer?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (locationRepository.getFeatureFlagsAsync().isInsurancePhoneCaptureDisabled()) {
                setState(CheckoutCollectPayerUiState.NavigateToSummaryFragment)
            } else {
                setState(CheckoutCollectPayerUiState.NavigateToPhoneCollectionFlow(payer))
            }
        }
    }
}

sealed class CheckoutCollectPayerUiState : State {
    object NavigateToSelectPayor : CheckoutCollectPayerUiState()

    object NavigateToCollectInsurance : CheckoutCollectPayerUiState()

    object NavigateToSummaryFragment : CheckoutCollectPayerUiState()

    data class NavigateToPhoneCollectionFlow(val payer: Payer?) : CheckoutCollectPayerUiState()
}
