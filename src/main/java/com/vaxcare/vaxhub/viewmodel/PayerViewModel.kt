/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.core.extension.isCreditCardCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isInsurancePhoneCaptureDisabled
import com.vaxcare.vaxhub.core.extension.isInsuranceScanDisabled
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.metric.PayerSelectedMetric
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class PayerViewModel @Inject constructor(
    private val payerRepository: PayerRepository,
    private val locationRepository: LocationRepository,
    private val analyticReport: AnalyticReport
) : BaseViewModel() {
    sealed class PayerUiState(
        open val payer: Payer? = null,
        open val isSkipInsuranceScan: Boolean? = null,
        open val isInsurancePhoneCaptureDisabled: Boolean? = null,
        open val isCreditCardCaptureDisabled: Boolean? = null
    ) : State {
        data class NormalPayerUpdated(
            override val payer: Payer,
            override val isSkipInsuranceScan: Boolean,
            override val isInsurancePhoneCaptureDisabled: Boolean,
            override val isCreditCardCaptureDisabled: Boolean
        ) : PayerUiState()

        data class NonNormalPayerUpdated(
            override val payer: Payer,
            override val isSkipInsuranceScan: Boolean,
            override val isInsurancePhoneCaptureDisabled: Boolean,
            override val isCreditCardCaptureDisabled: Boolean
        ) : PayerUiState()

        object Failed : PayerUiState()
    }

    fun getPayersByIdentifier(identifier: String): LiveData<List<Payer>> {
        return payerRepository.searchPayers(identifier)
    }

    fun getLastTwoRecentPayers(): LiveData<List<Payer>> {
        return payerRepository.getTwoMostRecentPayers()
    }

    private suspend fun updatePayerUsed(payer: Payer) {
        payer.updatedTime = Instant.now()
        payerRepository.updatePayer(payer)
    }

    fun selectedPayer(payer: Payer) =
        viewModelScope.launch(Dispatchers.IO) {
            val flags = locationRepository.getFeatureFlagsAsync()
            analyticReport.saveMetric(
                PayerSelectedMetric(
                    selectedPayer = payer.getName(),
                    selectedPayerId = payer.id.toString(),
                    selectedPlanId = payer.insurancePlanId.toString()
                )
            )
            val state = if (payer.isNormalPayer()) {
                updatePayerUsed(payer)
                PayerUiState.NormalPayerUpdated(
                    payer = payer,
                    isSkipInsuranceScan = flags.isInsuranceScanDisabled(),
                    isInsurancePhoneCaptureDisabled = flags.isInsurancePhoneCaptureDisabled(),
                    isCreditCardCaptureDisabled = flags.isCreditCardCaptureDisabled()
                )
            } else {
                PayerUiState.NonNormalPayerUpdated(
                    payer = payer,
                    isSkipInsuranceScan = flags.isInsuranceScanDisabled(),
                    isInsurancePhoneCaptureDisabled = flags.isInsurancePhoneCaptureDisabled(),
                    isCreditCardCaptureDisabled = flags.isCreditCardCaptureDisabled()
                )
            }

            setState(state)
        }
}
