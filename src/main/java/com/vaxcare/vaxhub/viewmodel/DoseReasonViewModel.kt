/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.extension.isCheckoutDemographicsCollectionDisabled
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
class DoseReasonViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : BaseViewModel() {
    sealed class DoseReasonUIState : State {
        data class NavigateToCollectDemo(
            val infoWrapper: InvalidInfoWrapper
        ) : DoseReasonUIState()

        data class NavigateToNewPayer(
            val infoWrapper: InvalidInfoWrapper
        ) : DoseReasonUIState()

        data class NavigateToPayerSelect(
            val infoWrapper: InvalidInfoWrapper
        ) : DoseReasonUIState()

        object NavigateToInsuranceScan : DoseReasonUIState()

        object NavigateToPhoneCollect : DoseReasonUIState()

        object NavigateToSummary : DoseReasonUIState()
    }

    private val _doseReasonUIState = MutableStateFlow<State>(Reset)
    val doseReasonUIState: StateFlow<State> = _doseReasonUIState

    override fun resetState() {
        _doseReasonUIState.value = Reset
    }

    fun determineAndEmitDestination(infoWrapper: InvalidInfoWrapper?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (infoWrapper == null) {
                _doseReasonUIState.value = DoseReasonUIState.NavigateToSummary
            } else {
                val flags = locationRepository.getFeatureFlagsAsync()
                val options = PayorNavigationOptions(
                    isNewInsurancePromptDisabled = flags.isNewInsurancePromptDisabled(),
                    isCheckoutDemographicsCollectionDisabled =
                        flags.isCheckoutDemographicsCollectionDisabled(),
                    isSelectPayorEnabled = flags.isPayorSelectionEnabled(),
                    isInsuranceScanDisabled = flags.isInsuranceScanDisabled(),
                    isInsurancePhoneCollectDisabled = flags.isInsurancePhoneCaptureDisabled()
                )

                when (infoWrapper.infoType) {
                    is InfoType.Both -> {
                        val state = if (!options.isCheckoutDemographicsCollectionDisabled) {
                            DoseReasonUIState.NavigateToCollectDemo(
                                infoWrapper
                            )
                        } else {
                            getNextInsuranceCollectionFromOptions(options, infoWrapper)
                        }

                        _doseReasonUIState.value = state
                    }

                    is InfoType.Demographic -> {
                        val state = if (options.isCheckoutDemographicsCollectionDisabled) {
                            DoseReasonUIState.NavigateToSummary
                        } else {
                            DoseReasonUIState.NavigateToCollectDemo(infoWrapper)
                        }

                        _doseReasonUIState.value = state
                    }

                    is InfoType.Payer -> {
                        val state = getNextInsuranceCollectionFromOptions(options, infoWrapper)
                        _doseReasonUIState.value = state
                    }
                }
            }
        }
    }

    private data class PayorNavigationOptions(
        val isNewInsurancePromptDisabled: Boolean,
        val isCheckoutDemographicsCollectionDisabled: Boolean,
        val isSelectPayorEnabled: Boolean,
        val isInsuranceScanDisabled: Boolean,
        val isInsurancePhoneCollectDisabled: Boolean
    )

    private fun getNextInsuranceCollectionFromOptions(
        options: PayorNavigationOptions,
        infoWrapper: InvalidInfoWrapper
    ): State {
        val isNewInsuranceNavigation =
            !options.isNewInsurancePromptDisabled && !infoWrapper.infoType.fields.isMissingPayerFields()
        return when {
            isNewInsuranceNavigation -> DoseReasonUIState.NavigateToNewPayer(infoWrapper)
            options.isSelectPayorEnabled -> DoseReasonUIState.NavigateToPayerSelect(infoWrapper)
            !options.isInsuranceScanDisabled -> DoseReasonUIState.NavigateToInsuranceScan
            !options.isInsurancePhoneCollectDisabled -> DoseReasonUIState.NavigateToPhoneCollect
            else -> DoseReasonUIState.NavigateToSummary
        }
    }
}
