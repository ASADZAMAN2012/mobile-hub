/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.service

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.codecorp.CDCamera
import com.codecorp.CDDecoder
import com.codecorp.CDDevice
import com.codecorp.CDLicense
import com.codecorp.CDLicenseResult
import com.codecorp.CDPerformanceFeatures
import com.codecorp.CDResult
import com.codecorp.CDSymbology
import com.codecorp.CDSymbology.CDSymbologyType
import com.vaxcare.core.config.VaxCareConfigListener
import com.vaxcare.core.config.VaxCareConfigResult
import com.vaxcare.core.model.ErrorBarcode
import com.vaxcare.core.model.TwoDeeBarcode
import com.vaxcare.vaxhub.core.extension.toAlphanumeric
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.setup.CameraSettings
import com.vaxcare.vaxhub.ui.scanner.ScannerState
import org.jetbrains.annotations.TestOnly
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerManager @Inject constructor(
    private val cameraSettings: CameraSettings
) : VaxCareConfigListener {
    enum class ScanType {
        DOSE,
        DRIVER_LICENSE
    }

    companion object {
        private const val NDC_LENGTH = 10
        private const val EXP_DATE_LENGTH = 6
        private const val LOT_NUMBER_MAX_LENGTH = 10
        private const val NDC_START_POSITION = 5
        private const val LOT_NUMBER_START_POSITION = 26
        private const val EXP_DATE_START_POSITION = 18

        private const val DATA_MATRIX_SYMBOL_ID = "]d2"
        private const val INFORMATION_SEPARATOR_THREE = '\u001D'
        private const val EXPIRATION_INDICATOR_VALUE = "10"
        private const val LOT_NUMBER_INDICATOR_VALUE = "17"
    }

    private var scanType: ScanType = ScanType.DOSE
    private val mDecoder: CDDecoder
        get() = CDDecoder.shared
    private val mLicense: CDLicense
        get() = CDLicense.shared
    private val mCamera: CDCamera
        get() = CDCamera.shared
    private val mDevice: CDDevice
        get() = CDDevice.shared

    private val formatter = DateTimeFormatter.ofPattern("yyMMdd")
    private val yearMonthFormatter = DateTimeFormatter.ofPattern("yyMM")

    private var isInitialized = false
    private var scanningFlag = false
    private var preview: View? = null
    private var refreshing = false
    private val config = ScanConfig(scan1d = false, scan2d = true)

    private val _state = MutableLiveData<ScannerState>(ScannerState.Uninitialized)
    val state: LiveData<ScannerState> = _state

    fun initializeScanner() {
        refreshing = true
        if (cameraSettings.expiration < LocalDateTime.now()) {
            cameraSettings.checkValues(this)
        } else {
            reconstructLibrary()
        }
    }

    fun configureScanner(
        scan1d: Boolean = false,
        scan2d: Boolean = true,
        type: ScanType
    ) {
        Timber.d("configureScanner($scan1d, $scan2d)")
        config.scan1d = scan1d
        config.scan2d = scan2d
        scanType = type
    }

    fun getCameraPreview(): View? {
        Timber.i("getCameraPreview(): $preview")
        return preview
    }

    fun startScanner() {
        Timber.i("startScanner()")
        if (isInitialized && !scanningFlag) {
            try {
                Timber.d("Scan: camera: ${mCamera.connectedCameras}")
                mCamera.startCamera { getCDResult(it) }
                mCamera.videoCapturing = true
                preview = mCamera.startPreview()
                mDecoder.setDuplicateDelay(cameraSettings.duplicateScanTimeoutMs)
                mDecoder.decoding = true
                scanningFlag = true
                setScannerState(ScannerState.Started)
            } catch (e: Exception) {
                Timber.e("Error starting scanner: $e")
            }
        }
    }

    fun refreshScanner(
        scan1d: Boolean = false,
        scan2d: Boolean = true,
        isForceRefresh: Boolean = false
    ) {
        Timber.i("refreshScanner($scan1d, $scan2d)")
        when {
            isInitialized && !refreshing -> {
                refreshing = true
                config.scan1d = scan1d
                config.scan2d = scan2d
                if (isLicenseExpiring()) {
                    cameraSettings.checkValues(this, isForceRefresh)
                } else {
                    setScannerState(ScannerState.Refreshed)
                    refreshing = false
                }
            }

            !isInitialized || isLicenseExpiring() -> cameraSettings.checkValues(this, isForceRefresh)
            else -> Unit
        }
    }

    private fun isLicenseExpiring() = LocalDateTime.now() > cameraSettings.expiration

    fun pauseDecoding() {
        Timber.i("pauseDecoding()")
        if (scanningFlag) {
            mDecoder.decoding = false
            setScannerState(ScannerState.Paused)
        }
    }

    fun resumeDecoding() {
        Timber.i("resumeDecoding()")
        if (scanningFlag) {
            mDecoder.decoding = true
            setScannerState(ScannerState.Resumed)
        }
    }

    fun stopScanner() {
        Timber.i("stopScanner()")
        try {
            mDecoder.decoding = false
            scanningFlag = false
            mCamera.stopPreview()
            mCamera.videoCapturing = false
        } catch (e: Exception) {
            // In rare cases we can navigate away from scan screens while mCamera is null
            // or in an invalid state
            Timber.e("Error stopping scanner: $e")
            resetLibrary()
        }
        setScannerState(ScannerState.Stopped)
    }

    private fun reconstructLibrary() {
        mCamera.cameraAPI = cameraSettings.cameraApi
        mCamera.resolution = cameraSettings.resolution
        mCamera.focus = cameraSettings.focus
        mCamera.agcMode = cameraSettings.agcMode
        CDPerformanceFeatures.shared.lowContrast = true
        mDecoder.setBarcodesToDecode(1, true)
        mDevice.audio = cameraSettings.enableAudio
        mCamera.torch = cameraSettings.enableTorch
        mDecoder.decoding = true

        Timber.d("customerId: ${cameraSettings.customerId}")
        Timber.d("license: ${cameraSettings.license}")
        mLicense.setCustomerID(cameraSettings.customerId)
        mLicense.activateLicense(cameraSettings.license) { onActivationResult(it) }
        Timber.d("New Settings ${printCameraProperties()}")
    }

    /**
     * Marks the library as uninitialized so that the next scanner fragment loaded
     * will perform all initialization steps once again
     *
     */
    fun resetLibrary() {
        isInitialized = false
    }

    /**
     * The scanner library used to decode the barcode objects
     */
    private fun printCameraProperties(): String {
        val stringBuilder: StringBuffer = StringBuffer()
            .append("Current Focus Mode: ${mCamera.focus}")
            .append("\n")
            .append("Is Zoom supported: ${mCamera.supportedZoomRange}")
            .append("\n")
            .append("Zoom: ${mCamera.zoom}")
            .append("\n")
            .append("Is resolution supported: ${mCamera.isResolutionSupported(mCamera.resolution)}")
            .append("\n")
            .append("Camera resolution: ${mCamera.resolution}")
            .append("\n")

        return stringBuilder.toString()
    }

    /**
     * On a successful scan of the barcode data, stop decoding and start a handler that will start
     * decoding after a set interval to allow the user to see what was scanned. Based on the data
     * received from the library, process the appropriate data using one of the parsing methods.
     *
     * This method is only called on a successful scan when it recognizes an enabled barcode type.
     *
     * @see parse2dBarcode
     *
     * @param results the raw data received from the library
     */
    private fun getCDResult(results: Array<out CDResult?>?) {
        if (results?.isNotEmpty() == true && results[0]?.status != CDResult.CDDecodeStatus.noDecode) {
            results.firstOrNull()?.let { result ->
                cameraSettings.reportBarcode(result.barcodeData, result.rawSymbology.name)
                when (result.status) {
                    CDResult.CDDecodeStatus.noActiveLicense,
                    CDResult.CDDecodeStatus.decodingDisabled,
                    CDResult.CDDecodeStatus.noUniqueDecode,
                    CDResult.CDDecodeStatus.failedQRConfigCode -> {
                        logBarcodeError(
                            result.barcodeData,
                            result.rawSymbology,
                            "Error parsing barcode: ${result.status}"
                        )
                        onError(result.barcodeData)
                    }

                    CDResult.CDDecodeStatus.success,
                    CDResult.CDDecodeStatus.decodedQRConfigCode -> {
                        when (result.rawSymbology) {
                            CDSymbologyType.dataMatrix ->
                                parse2dBarcode(
                                    barcode = result.barcodeData,
                                    symbology = result.rawSymbology
                                )

                            CDSymbologyType.pdf417 -> parsePDF417Barcode(
                                barcode = result.barcodeData,
                                symbology = result.rawSymbology
                            )

                            else -> logBarcodeError(
                                barcode = result.barcodeData,
                                symbology = result.rawSymbology,
                                message = "Not supported barcode type: ${result.symbology}"
                            )
                        }
                    }

                    else -> Unit // CDDecodeStatus.noDecode constantly fires while decoding
                }
            }
        }
    }

    /**
     * Returns a parsed 2d object. The object contains a lot object, product object, and the exp
     * date.
     *
     * @param barcode
     * @return A parsed 2d object, otherwise an error object
     */
    @TestOnly
    fun parse2dBarcode(barcode: String, symbology: CDSymbologyType) {
        if (config.scan2d && scanType == ScanType.DOSE) {
            // reset
            var expDateStartPosition = EXP_DATE_START_POSITION
            var lotNumberStartPosition = LOT_NUMBER_START_POSITION

            Timber.d("2D Barcode raw: $barcode")
            var cleanedBarcode = barcode
            if (isAbnormalBarcode(cleanedBarcode)) {
                parseAbnormalBarcode(cleanedBarcode).let { info ->
                    cleanedBarcode = info.barcode
                    expDateStartPosition = info.expDateStartPosition
                    lotNumberStartPosition = info.lotNumberStartPosition
                }
            }
            Timber.d("2D Barcode Cleaned: $cleanedBarcode")
            val productNdc = if (cleanedBarcode.length >= (NDC_START_POSITION + NDC_LENGTH)) {
                barcode.substring(NDC_START_POSITION, NDC_START_POSITION + NDC_LENGTH)
            } else {
                ""
            }
            val expiration = if (cleanedBarcode.length < expDateStartPosition + EXP_DATE_LENGTH) {
                LocalDate.parse("1900-01-01")
            } else {
                parseExpirationDate(
                    barcode.substring(expDateStartPosition, expDateStartPosition + EXP_DATE_LENGTH)
                )
            }

            val lotNumber =
                if (
                    cleanedBarcode.length in
                    (lotNumberStartPosition + 1)..(lotNumberStartPosition + LOT_NUMBER_MAX_LENGTH)
                ) {
                    cleanedBarcode.substring(lotNumberStartPosition).toAlphanumeric()
                } else if (isLotBeforeExpiration(cleanedBarcode)) {
                    cleanedBarcode.substring(lotNumberStartPosition, expDateStartPosition - 2)
                } else {
                    ""
                }.filter { it.isLetterOrDigit() }
            Timber.d("NDC $productNdc | Expiration $expiration | LotNumber $lotNumber")
            val barcodeState =
                expiration?.let { exp ->
                    TwoDeeBarcode(
                        symbolName = symbology.name,
                        barcode = cleanedBarcode,
                        expiration = exp,
                        lotNumber = lotNumber,
                        productNdc = productNdc
                    )
                } ?: ErrorBarcode(
                    symbolName = symbology.name,
                    barcode = cleanedBarcode,
                    message = "Expiration was Null"
                )

            setScannerState(ScannerState.Barcode(barcodeState))
        }
    }

    private fun parseExpirationDate(expDateString: String): LocalDate? {
        val emptyDay = "00"
        val date = try {
            if (expDateString.endsWith(emptyDay)) {
                val year = YearMonth.parse(
                    expDateString.substring(
                        0,
                        expDateString.length - emptyDay.length
                    ),
                    yearMonthFormatter
                )
                year.atDay(1)
            } else {
                LocalDate.parse(expDateString, formatter)
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
        return date
    }

    private fun isAbnormalBarcode(barcode: String): Boolean {
        return barcode.length < LOT_NUMBER_START_POSITION ||
            barcode.substring(
                LOT_NUMBER_START_POSITION - 2,
                LOT_NUMBER_START_POSITION
            ) != EXPIRATION_INDICATOR_VALUE ||
            barcode.indexOf(INFORMATION_SEPARATOR_THREE) >= 0 ||
            barcode.contains(DATA_MATRIX_SYMBOL_ID) ||
            barcode.startsWith("2")
    }

    private fun parseAbnormalBarcode(barcode: String): TwoDBarcodeParsingInfo {
        Timber.d("Abnormal barcode: $barcode")
        var abnormalBarcodeType = "Unknown abnormality"

        var expDateStartPosition = EXP_DATE_START_POSITION
        var lotNumberStartPosition = LOT_NUMBER_START_POSITION

        if (barcode.contains(DATA_MATRIX_SYMBOL_ID)) {
            abnormalBarcodeType = "Data matrix symbol"
            var delimiter = barcode.indexOf(DATA_MATRIX_SYMBOL_ID)
            delimiter = if (delimiter < 0) {
                0
            } else {
                delimiter
            }
            if (barcode.substring(delimiter + 4, delimiter + 6) != "01") {
                Timber.e("Unable to find a correct barcode: $barcode")
            } else if (barcode.length > delimiter + 30) {
                expDateStartPosition = delimiter + 22
                lotNumberStartPosition = delimiter + 30
            }
        }

        if (barcode.substring(0, 2) != "01") {
            abnormalBarcodeType = "Starting digits"
            if (barcode.first() == '2') {
                return TwoDBarcodeParsingInfo(
                    barcode.substring(1, barcode.length),
                    expDateStartPosition,
                    lotNumberStartPosition
                )
            }
        } else if (barcode.length > expDateStartPosition && barcode.substring(
                expDateStartPosition - 2,
                expDateStartPosition
            ) != LOT_NUMBER_INDICATOR_VALUE
        ) {
            if (isLotBeforeExpiration(barcode)) {
                abnormalBarcodeType = "Lot before expiration"
                expDateStartPosition = barcode.length - 6
                lotNumberStartPosition = EXP_DATE_START_POSITION
            } else {
                abnormalBarcodeType = "Expiration in wrong location"
                val delimiter = barcode.lastIndexOf(INFORMATION_SEPARATOR_THREE)
                if (barcode.substring(delimiter + 1, delimiter + 3) == LOT_NUMBER_INDICATOR_VALUE) {
                    expDateStartPosition = delimiter + 3
                    lotNumberStartPosition = delimiter + 11
                }
            }
        }

        cameraSettings.reportAbnormalBarcode(barcode, abnormalBarcodeType)
        return TwoDBarcodeParsingInfo(barcode, expDateStartPosition, lotNumberStartPosition)
    }

    private fun isLotBeforeExpiration(barcode: String): Boolean {
        if (barcode.length < EXP_DATE_START_POSITION) {
            return false
        }

        return try {
            barcode.substring(
                EXP_DATE_START_POSITION - 2,
                EXP_DATE_START_POSITION
            ) == EXPIRATION_INDICATOR_VALUE &&
                barcode.takeLast(8).startsWith(LOT_NUMBER_INDICATOR_VALUE) &&
                parseExpirationDate(barcode.takeLast(6)) != null
        } catch (e: Exception) {
            Timber.e(e.message)
            false
        }
    }

    override fun onFetchSuccess(vaxCareConfigResult: VaxCareConfigResult) {
        if (vaxCareConfigResult.isCodeCorpUpdated || !isInitialized) {
            reconstructLibrary()
        } else {
            setScannerState(ScannerState.Refreshed)
            refreshing = false
        }
    }

    override fun onFetchFailure(e: Exception) {
        if (!isInitialized) {
            reconstructLibrary()
        } else {
            setScannerState(ScannerState.Refreshed)
            refreshing = false
        }
    }

    private fun parsePDF417Barcode(barcode: String, symbology: CDSymbologyType) {
        Timber.d("PDF417 Barcode: $barcode")
        if (scanType == ScanType.DRIVER_LICENSE) {
            val driverLicense = DriverLicense.convertBarCodeToDriverLicense(barcode)
            setScannerState(ScannerState.DriverLicenseBarcode(symbology.name, driverLicense))
        }
    }

    /**
     * Necessary to configure which symbologies (bar code types) are enabled
     * The SDK default values do not include code39 (Consent forms)
     * Changes to these values aren't respected after the mCamera is started
     *
     * Recently had to remove Code128 here as that was interfering with driverlicense scans.
     * For some reason the sdk will return the Code128 as a scanned item over the PDF417 - though
     * this could be a coincidence and the lighting used at the time of testing was inadequate
     *
     */
    private fun enableSymbologies() {
        val symbologyList = mLicense.licensedSymbologies
        symbologyList.forEach { symbology ->
            when (symbology) {
                CDSymbologyType.code39 -> CDSymbology.Code39.setCode39(false)

                CDSymbologyType.dataMatrix ->
                    CDSymbology.DataMatrix.setDataMatrix(true)

                CDSymbologyType.code128_CCA,
                CDSymbologyType.code128_CCB,
                CDSymbologyType.code128_CCC,
                CDSymbologyType.code128 -> CDSymbology.Code128.setCode128(false)

                CDSymbologyType.interleaved2of5 ->
                    CDSymbology.Interleaved2Of5.setInterleaved2of5(false)

                CDSymbologyType.upca_CCA,
                CDSymbologyType.upca_CCB,
                CDSymbologyType.upca_CCC,
                CDSymbologyType.upca -> CDSymbology.UPCA.setUPCA(false)

                CDSymbologyType.upce,
                CDSymbologyType.upce_CCA,
                CDSymbologyType.upce_CCB,
                CDSymbologyType.upce_CCC -> CDSymbology.UPCE.setUPCE(false)

                CDSymbologyType.ean8_CCA,
                CDSymbologyType.ean8_CCB,
                CDSymbologyType.ean8_CCC,
                CDSymbologyType.ean8 -> CDSymbology.EAN8.setEAN8(false)

                CDSymbologyType.ean13_CCA,
                CDSymbologyType.ean13_CCB,
                CDSymbologyType.ean13_CCC,
                CDSymbologyType.ean13 ->
                    CDSymbology.EAN13.setEAN13(false)

                CDSymbologyType.gs1DatabarExpanded,
                CDSymbologyType.gs1DatabarExpandedStacked,
                CDSymbologyType.gs1DatabarLimited,
                CDSymbologyType.gs1DatabarOmniTruncated,
                CDSymbologyType.gs1DatabarStacked ->
                    CDSymbology.GS1Databar.setGS1Databar(false)

                CDSymbologyType.codabar -> CDSymbology.Codabar.setCodabar(false)

                CDSymbologyType.aztecCode -> CDSymbology.Aztec.setAztec(false)

                CDSymbologyType.dotcode -> CDSymbology.DotCode.setDotCode(false)

                CDSymbologyType.qrCode,
                CDSymbologyType.qrCodeMicro,
                CDSymbologyType.qrCodeModel1,
                CDSymbologyType.qrConfigurationCode -> CDSymbology.QRCode.setQR(false)

                CDSymbologyType.pdf417,
                CDSymbologyType.microPDF417 -> CDSymbology.PDF417.setPDF417(true)

                else -> Unit
            }
        }
    }

    private fun onActivationResult(result: CDLicenseResult) {
        Timber.i("ScannerLicense onActivationResult: ${result.status}")
        when (result.status) {
            CDLicenseResult.CDLicenseStatus.activated -> {
                Timber.d("Scanner License Valid and Activated")
                isInitialized = true
                enableSymbologies()
                setScannerState(ScannerState.Activation(true))
            }

            else -> {
                Timber.e("Scanner License Failure: ${result.status}")
                isInitialized = false
                setScannerState(ScannerState.Activation(false))
            }
        }
        refreshing = false
    }

    private fun onError(resultCode: String?) {
        Timber.e("ScannerLicense OnError $resultCode")
        isInitialized = false
        setScannerState(ScannerState.Activation(false))
    }

    /**
     * Set the scanner state in the ViewModel
     * Fragments require postValue, dialogs require setValue
     *
     * @param scannerState
     */
    private fun setScannerState(scannerState: ScannerState) {
        try {
            _state.value = scannerState
        } catch (e: Exception) {
            _state.postValue(scannerState)
        }
    }

    private fun logBarcodeError(
        barcode: String,
        symbology: CDSymbologyType,
        message: String
    ) = setScannerState(ScannerState.Barcode(ErrorBarcode(symbology.name, barcode, message)))

    /**
     * Object to hold scan1d and scan2d vars from the fragment
     */
    private data class ScanConfig(
        var scan1d: Boolean,
        var scan2d: Boolean
    )
}

private data class TwoDBarcodeParsingInfo(
    val barcode: String,
    val expDateStartPosition: Int,
    val lotNumberStartPosition: Int
)
