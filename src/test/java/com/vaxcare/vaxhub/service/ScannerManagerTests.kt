/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.codecorp.CDSymbology.CDSymbologyType
import com.vaxcare.core.config.VaxCareConfig
import com.vaxcare.core.model.TwoDeeBarcode
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.data.MockBarcode
import com.vaxcare.vaxhub.data.MockBarcode.EXPIRATION_IN_WRONG_LOCATION
import com.vaxcare.vaxhub.data.MockBarcode.LOT_BEFORE_EXPIRATION
import com.vaxcare.vaxhub.data.MockBarcode.NORMAL_BARCODE
import com.vaxcare.vaxhub.data.MockBarcode.UNKNOWN_ABNORMALITY
import com.vaxcare.vaxhub.model.setup.CameraSettings
import com.vaxcare.vaxhub.ui.scanner.ScannerState
import com.vaxcare.vaxhub.util.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.time.LocalDate

class ScannerManagerTests {
    companion object {
        private const val DEFAULT_EXP_DATE = "1900-01-01"
    }

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @RelaxedMockK
    val vaxCareConfig = mockk<VaxCareConfig>()

    @RelaxedMockK
    val analyticReport = mockk<AnalyticReport>()

    private var cameraSettings: CameraSettings? = null
    private var scannerManager: ScannerManager? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        cameraSettings = CameraSettings(vaxCareConfig, analyticReport)
        cameraSettings?.let { scannerManager = ScannerManager(it) }
    }

    @Test
    fun `Verify hidden unicode characters filtered from barcode scan`() {
        val barcode = "010034928142488510UT8415JA\u001D17250630"
        val symbology = CDSymbologyType.dataMatrix

        coEvery { analyticReport.saveMetric(any()) } just Runs
        scannerManager?.parse2dBarcode(barcode, symbology)

        val state = scannerManager?.state?.getOrAwaitValue()
        Assert.assertTrue(state is ScannerState.Barcode)

        val barcodePayload = (state as? ScannerState.Barcode)?.barcodePayload
        if (barcodePayload is TwoDeeBarcode) {
            assertEquals("UT8415JA", barcodePayload.lotNumber)
        } else {
            fail("Barcode payload is not a TwoDeeBarcode!")
        }
    }

    @Test
    fun `parse2dBarcode should emit  a valid TwoDeeBarcode object when a standard barcode format value is passed`() {
        val barcode = NORMAL_BARCODE.value
        val symbology = CDSymbologyType.dataMatrix

        coEvery { analyticReport.saveMetric(any()) } just Runs
        scannerManager?.parse2dBarcode(barcode, symbology)

        val state = scannerManager?.state?.getOrAwaitValue()
        Assert.assertTrue(state is ScannerState.Barcode)

        val barcodePayload = (state as? ScannerState.Barcode)?.barcodePayload
        if (barcodePayload is TwoDeeBarcode) {
            assertEquals(NORMAL_BARCODE.lotNumber, barcodePayload.lotNumber)
            assertEquals(NORMAL_BARCODE.expirationDate, barcodePayload.expiration)
        } else {
            fail("Barcode payload is not a TwoDeeBarcode!")
        }
    }

    @Test
    fun `parse2dBarcode should emit a TwoDeeBarcode object when a barcode with swapped lot number and expiration is passed`() {
        val barcode = LOT_BEFORE_EXPIRATION.value
        val symbology = CDSymbologyType.dataMatrix

        coEvery { analyticReport.saveMetric(any()) } just Runs
        scannerManager?.parse2dBarcode(barcode, symbology)

        val state = scannerManager?.state?.getOrAwaitValue()
        Assert.assertTrue(state is ScannerState.Barcode)

        val barcodePayload = (state as? ScannerState.Barcode)?.barcodePayload
        if (barcodePayload is TwoDeeBarcode) {
            assertEquals(LOT_BEFORE_EXPIRATION.lotNumber, barcodePayload.lotNumber)
            assertEquals(LOT_BEFORE_EXPIRATION.expirationDate, barcodePayload.expiration)
        } else {
            fail("Barcode payload is not a TwoDeeBarcode!")
        }
    }

    @Test
    fun `parse2dBarcode should emit a TwoDeeBarcode object when a barcode with expiration in wrong location passed`() {
        val barcode = EXPIRATION_IN_WRONG_LOCATION.value
        val symbology = CDSymbologyType.dataMatrix

        coEvery { analyticReport.saveMetric(any()) } just Runs
        scannerManager?.parse2dBarcode(barcode, symbology)

        val state = scannerManager?.state?.getOrAwaitValue()
        Assert.assertTrue(state is ScannerState.Barcode)

        val barcodePayload = (state as? ScannerState.Barcode)?.barcodePayload
        if (barcodePayload is TwoDeeBarcode) {
            assertEquals(EXPIRATION_IN_WRONG_LOCATION.lotNumber, barcodePayload.lotNumber)
            assertEquals(EXPIRATION_IN_WRONG_LOCATION.expirationDate, barcodePayload.expiration)
        } else {
            fail("Barcode payload is not a TwoDeeBarcode!")
        }
    }

    @Test
    fun `parse2dBarcode should emit an TwoDeeBarcode object with fallback values when a barcode with starting digits is passed`() {
        val barcode = MockBarcode.STARTING_DIGITS.value
        val symbology = CDSymbologyType.dataMatrix
        val fallbackTwoDeeBarcodeExpDate: LocalDate = LocalDate.parse(DEFAULT_EXP_DATE)

        coEvery { analyticReport.saveMetric(any()) } just Runs
        scannerManager?.parse2dBarcode(barcode, symbology)

        val state = scannerManager?.state?.getOrAwaitValue()
        Assert.assertTrue(state is ScannerState.Barcode)

        val barcodePayload = (state as? ScannerState.Barcode)?.barcodePayload
        if (barcodePayload is TwoDeeBarcode) {
            assertEquals(fallbackTwoDeeBarcodeExpDate, barcodePayload.expiration)
        } else {
            fail("Barcode payload is not a TwoDeeBarcode!")
        }
    }

    @Test
    fun `parse2dBarcode should emit an TwoDeeBarcode object with fallback values when a barcode with an unknown abnormality is passed`() {
        val barcode = UNKNOWN_ABNORMALITY.value
        val symbology = CDSymbologyType.dataMatrix

        coEvery { analyticReport.saveMetric(any()) } just Runs
        scannerManager?.parse2dBarcode(barcode, symbology)

        val state = scannerManager?.state?.getOrAwaitValue()
        Assert.assertTrue(state is ScannerState.Barcode)

        val barcodePayload = (state as? ScannerState.Barcode)?.barcodePayload
        if (barcodePayload is TwoDeeBarcode) {
            assertEquals(UNKNOWN_ABNORMALITY.expirationDate, barcodePayload.expiration)
        } else {
            fail("Barcode payload is not a TwoDeeBarcode!")
        }
    }
}
