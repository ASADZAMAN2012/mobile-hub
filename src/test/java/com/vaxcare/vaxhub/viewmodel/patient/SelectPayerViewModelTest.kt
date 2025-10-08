/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel.patient

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.data.allPayers
import com.vaxcare.vaxhub.data.recentPayers
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PayerRepository
import com.vaxcare.vaxhub.util.FlowDispatcherRule
import com.vaxcare.vaxhub.util.getOrAwaitValue
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.payer.SelectPayerViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.flow
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

const val SEARCH_TERM = "Vax"

class SelectPayerViewModelTest {
    @get:Rule
    val flowRule = FlowDispatcherRule()

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var selectPayerViewModel: SelectPayerViewModel

    @RelaxedMockK
    lateinit var payerRepository: PayerRepository

    @RelaxedMockK
    lateinit var locationRepository: LocationRepository

    @RelaxedMockK
    lateinit var analytics: AnalyticReport

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { locationRepository.getFeatureFlags() } returns featureFlagsLiveData
        coEvery { payerRepository.searchPayers(any()) } returns searchPayersLiveData
        coEvery { payerRepository.searchPayers(SEARCH_TERM) } returns searchPayersLiveData.map { list ->
            list.filter {
                it.insuranceName?.contains(
                    SEARCH_TERM,
                    true
                ) == true
            }
        }
        coEvery { payerRepository.getThreeMostRecentPayers() } returns recentPayersFlow
        selectPayerViewModel = SelectPayerViewModel(
            payerRepository = payerRepository,
            locationRepository = locationRepository,
            analytics = analytics
        )
    }

    @Test
    fun `Recent Payers State`() {
        val expectedPayersListSize = recentPayers.size
        val state = selectPayerViewModel.state.getOrAwaitValue()
        Assert.assertTrue(state is SelectPayerViewModel.SelectPayerState.RecentPayersFetched)
        val actualListSize = (state as SelectPayerViewModel.SelectPayerState.RecentPayersFetched)
            .recentPayers.size
        Assert.assertTrue(actualListSize == expectedPayersListSize)
    }

    @Test
    fun `Search All Payers`() {
        val expectedSearchListSize = allPayers.size
        val allSearchResults = selectPayerViewModel.searchedPayers.getOrAwaitValue()
        Assert.assertTrue(allSearchResults.size == expectedSearchListSize)
    }

    @Test
    fun `Search 'Vax' Payers`() {
        val expectedSearchListSize =
            allPayers.filter { it.insuranceName?.contains(SEARCH_TERM) == true }.size
        selectPayerViewModel.onSearch(SEARCH_TERM)
        val vaxSearchResults = selectPayerViewModel.searchedPayers.getOrAwaitValue()
        Assert.assertTrue(vaxSearchResults.size == expectedSearchListSize)
    }
}

private val recentPayersFlow = flow { emit(recentPayers) }

private val searchPayersLiveData = MutableLiveData(allPayers)

private val featureFlagsLiveData = MutableLiveData<List<FeatureFlag>>(listOf())
