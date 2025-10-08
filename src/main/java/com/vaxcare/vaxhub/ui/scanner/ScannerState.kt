/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.scanner

import com.vaxcare.core.model.BarcodePayload
import com.vaxcare.vaxhub.model.DriverLicense

sealed class ScannerState {
    data class Activation(val success: Boolean) : ScannerState()

    data class Barcode(val barcodePayload: BarcodePayload) : ScannerState()

    data class DriverLicenseBarcode(val symbology: String, val driverLicense: DriverLicense) : ScannerState()

    object Uninitialized : ScannerState()

    object Started : ScannerState()

    object Refreshed : ScannerState()

    object Resumed : ScannerState()

    object Paused : ScannerState()

    object Stopped : ScannerState()
}
