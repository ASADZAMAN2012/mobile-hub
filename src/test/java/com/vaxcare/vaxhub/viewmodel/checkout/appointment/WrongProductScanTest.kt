package com.vaxcare.vaxhub.viewmodel.checkout.appointment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.core.model.TwoDeeBarcode
import com.vaxcare.vaxhub.model.WrongProductNdc
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.ui.fragment.BaseScannerViewModel
import com.vaxcare.vaxhub.util.FlowDispatcherRule
import com.vaxcare.vaxhub.util.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.time.LocalDate

@ExperimentalCoroutinesApi
class WrongProductScanTest {
    @get:Rule
    var dispatcherRule = FlowDispatcherRule()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private lateinit var viewModel: BaseScannerViewModel

    private val productRepository = mockk<ProductRepository>()
    private val locationRepository = mockk<LocationRepository>()
    private val wrongProductRepository = mockk<WrongProductRepository>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = object : BaseScannerViewModel(
            locationRepository,
            productRepository,
            wrongProductRepository
        ) {}
    }

    @Test
    fun `badProduct scan test`() {
        val barcode = TwoDeeBarcode(
            barcode = "123456",
            symbolName = "datamatrix",
            expiration = LocalDate.now(),
            lotNumber = "123456",
            productNdc = "123ABC"
        )
        val wrongProductNdc = WrongProductNdc("123ABC", "This Product is <b>wrong!</b>")
        val expectedState = BaseScannerViewModel.BaseScannerState.ProductNotAllowed(
            wrongProductNdc.ndc,
            wrongProductNdc.errorMessage
        )
        coEvery { productRepository.findLotNumberByNameAsync(any()) } returns null
        coEvery { locationRepository.getFeatureFlagsAsync() } returns emptyList()
        coEvery { wrongProductRepository.findProductByNdc(any()) } returns wrongProductNdc
        viewModel.onTwoDeeBarcode(barcode)
        val state = viewModel.state.getOrAwaitValue(2)
        assertEquals(expectedState, state)
    }
}
