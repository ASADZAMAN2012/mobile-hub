/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel.checkout.appointment.payer

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScreenNavigationMetric
import com.vaxcare.vaxhub.core.extension.isEmployerCoveredEnabled
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.metric.PayerSelectedMetric
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PayerRepository
import com.vaxcare.vaxhub.viewmodel.BaseViewModel
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SelectPayerViewModel @Inject constructor(
    private val payerRepository: PayerRepository,
    private val locationRepository: LocationRepository,
    @MHAnalyticReport private val analytics: AnalyticReport
) : BaseViewModel() {
    private val queryFlow = MutableStateFlow<String?>(null)

    sealed class SelectPayerState : State {
        data class RecentPayersFetched(
            val recentPayers: List<Payer>,
            val isEmployerCoveredEnabled: Boolean
        ) : SelectPayerState()

        data class PayerSelected(val payer: Payer) : SelectPayerState()
    }

    init {
        viewModelScope.launch {
            payerRepository.getThreeMostRecentPayers()
                .combine(locationRepository.getFeatureFlags().asFlow(), ::Pair)
                .onEach { (recentPayers, flags) ->
                    setState(
                        SelectPayerState.RecentPayersFetched(
                            recentPayers = recentPayers,
                            isEmployerCoveredEnabled = flags.isEmployerCoveredEnabled()
                        )
                    )
                }.stateIn(viewModelScope)
        }
    }

    /**
     * LiveData that reflects the query searched on the UI. Null/empty string will return the full
     * list
     */
    val searchedPayers: LiveData<List<Payer>> = queryFlow.flatMapLatest { query ->
        payerRepository.searchPayers(query ?: "").asFlow()
    }.asLiveData()

    /**
     * Updates the searchedPayers LiveData by mutating the queryFlow with the incoming query
     *
     * @param query search query from view
     */
    fun onSearch(query: String?) = queryFlow.tryEmit(query)

    fun logScreenNavigation(screenTitle: String) = analytics.saveMetric(ScreenNavigationMetric(screenTitle))

    fun onPayerSelected(payer: Payer) {
        analytics.saveMetric(
            PayerSelectedMetric(
                selectedPayer = payer.getName(),
                selectedPayerId = payer.id.toString(),
                selectedPlanId = payer.insurancePlanId.toString()
            )
        )
        setState(SelectPayerState.PayerSelected(payer))
        viewModelScope.launch(Dispatchers.IO) {
            updatePayerUsed(payer)
        }
    }

    private suspend fun updatePayerUsed(payer: Payer) {
        payer.updatedTime = Instant.now()
        payerRepository.updatePayer(payer)
    }
}
