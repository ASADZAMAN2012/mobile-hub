/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.data.MockLocations
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepository
import com.vaxcare.vaxhub.worker.jobs.inventory.SimpleOnHandInventoryJob
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

typealias JobResults = Pair<String, Boolean>

@OptIn(ExperimentalCoroutinesApi::class)
class VaxJobTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var simpleOnHandInventoryJob: SimpleOnHandInventoryJob
    private val simpleOnHandInventoryRepository: SimpleOnHandInventoryRepository =
        mockk(relaxed = true)
    private val locationRepository: LocationRepository = mockk()
    private val analyticReport: AnalyticReport = mockk(relaxed = true)
    private val callbackReporter = CallbackReporter()

    private val observer: Observer<JobResults> = mockk(relaxed = true)

    private class CallbackReporter {
        private val _currentValue = MutableLiveData<JobResults>()
        val currentValue: LiveData<JobResults> = _currentValue.distinctUntilChanged()

        fun reportJob(results: JobResults) {
            _currentValue.postValue(results)
        }
    }

    @Before
    fun setup() {
        simpleOnHandInventoryJob = SimpleOnHandInventoryJob(
            simpleOnHandInventoryRepository = simpleOnHandInventoryRepository,
            locationRepository = locationRepository,
            callback = { jobName, success -> callbackReporter.reportJob(jobName to success) },
            analyticReport = analyticReport
        )
        callbackReporter.currentValue.observeForever(observer)
    }

    @Test
    fun `simpleOnHandInventoryJob does not run without FF and inventory sources`() =
        runTest {
            coEvery { locationRepository.getLocationAsync() } returns MockLocations.emptyLocationDataNoFF
            val jobName = SimpleOnHandInventoryJob::class.simpleName!!
            simpleOnHandInventoryJob.doWork()
            advanceUntilIdle()
            verify { observer.onChanged(jobName to false) }
        }

    @Test
    fun `simpleOnHandInventoryJob runs with FF and inventory sources`() =
        runTest {
            coEvery { locationRepository.getLocationAsync() } returns MockLocations.locationDataWithVfcAndPublicStockFF
            val jobName = SimpleOnHandInventoryJob::class.simpleName!!
            simpleOnHandInventoryJob.doWork()
            advanceUntilIdle()
            verify { observer.onChanged(jobName to true) }
        }

    @Test
    fun `simpleOnHandInventoryJob does not run with empty FF`() =
        runTest {
            coEvery { locationRepository.getLocationAsync() } returns MockLocations.locationDataWithVfcAndNoFF
            val jobName = SimpleOnHandInventoryJob::class.simpleName!!
            simpleOnHandInventoryJob.doWork()
            advanceUntilIdle()
            verify { observer.onChanged(jobName to false) }
        }

    @Test
    fun `simpleOnHandInventoryJob does not run with FF and no inventory sources`() =
        runTest {
            coEvery { locationRepository.getLocationAsync() } returns
                MockLocations.locationDataWithNoStocksAndPublicStockFF
            val jobName = SimpleOnHandInventoryJob::class.simpleName!!
            simpleOnHandInventoryJob.doWork()
            advanceUntilIdle()
            verify { observer.onChanged(jobName to false) }
        }
}
