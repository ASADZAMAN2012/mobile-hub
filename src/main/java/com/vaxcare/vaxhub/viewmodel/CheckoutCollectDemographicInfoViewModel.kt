/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.isInsurancePhoneCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isInsuranceScanDisabled
import com.vaxcare.vaxhub.core.extension.isMissingPayerFields
import com.vaxcare.vaxhub.core.extension.isNewInsurancePromptDisabled
import com.vaxcare.vaxhub.core.extension.isPayorSelectionEnabled
import com.vaxcare.vaxhub.model.patient.InfoType
import com.vaxcare.vaxhub.model.patient.InvalidInfoWrapper
import com.vaxcare.vaxhub.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutCollectDemographicInfoViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : BaseViewModel() {
    sealed class CollectDemoInfoUIState : State {
        data class NavigateToNewPayer(
            val infoWrapper: InvalidInfoWrapper
        ) : CollectDemoInfoUIState()

        object NavigateToPayerSelect : CollectDemoInfoUIState()

        object NavigateToInsuranceScan : CollectDemoInfoUIState()

        object NavigateToPhoneCollect : CollectDemoInfoUIState()

        object NavigateToSummary : CollectDemoInfoUIState()
    }

    private val _collectDemoUIState = MutableStateFlow<State>(Reset)
    val collectDemoUIState: StateFlow<State> = _collectDemoUIState

    override fun resetState() {
        _collectDemoUIState.value = Reset
    }

    fun determineAndEmitDestination(infoWrapper: InvalidInfoWrapper) {
        viewModelScope.launch(Dispatchers.IO) {
            when (infoWrapper.infoType) {
                is InfoType.Demographic ->
                    _collectDemoUIState.value = CollectDemoInfoUIState.NavigateToSummary

                else -> emitNextDestination(infoWrapper)
            }
        }
    }

    private fun emitNextDestination(infoWrapper: InvalidInfoWrapper) =
        viewModelScope.launch(Dispatchers.IO) {
            val flags = locationRepository.getFeatureFlagsAsync()
            val isNewInsurancePromptDisabled = flags.isNewInsurancePromptDisabled()
            val isSelectPayorEnabled = flags.isPayorSelectionEnabled()
            val isInsuranceScanDisabled = flags.isInsuranceScanDisabled()
            val isInsurancePhoneCollectDisabled = flags.isInsurancePhoneCaptureDisabled()

            val isNewInsuranceNavigation =
                !isNewInsurancePromptDisabled && !infoWrapper.infoType.fields.isMissingPayerFields()
            val state = when {
                isNewInsuranceNavigation -> CollectDemoInfoUIState.NavigateToNewPayer(infoWrapper)
                isSelectPayorEnabled -> CollectDemoInfoUIState.NavigateToPayerSelect
                !isInsuranceScanDisabled -> CollectDemoInfoUIState.NavigateToInsuranceScan
                !isInsurancePhoneCollectDisabled -> CollectDemoInfoUIState.NavigateToPhoneCollect
                else -> CollectDemoInfoUIState.NavigateToSummary
            }

            _collectDemoUIState.value = state
        }
}
