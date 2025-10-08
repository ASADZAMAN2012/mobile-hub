/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.fragment

import androidx.lifecycle.viewModelScope
import com.vaxcare.core.extensions.toLocalDateString
import com.vaxcare.core.model.ErrorBarcode
import com.vaxcare.core.model.TwoDeeBarcode
import com.vaxcare.core.report.model.ProductSelectionMetric
import com.vaxcare.vaxhub.model.DriverLicense
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.WrongProductRepository
import com.vaxcare.vaxhub.viewmodel.BaseViewModel
import com.vaxcare.vaxhub.viewmodel.State
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class BaseScannerViewModel(
    protected val locationRepository: LocationRepository,
    protected val productRepository: ProductRepository,
    protected val wrongProductRepository: WrongProductRepository
) : BaseViewModel() {
    private val productSource2D = "2D Scan"
    private val mobileHub = "Mobile Hub"

    sealed class BaseScannerState : State {
        data class LotNumberBarcodeFound(
            val productSelectionMetric: ProductSelectionMetric,
            val lotNumberWithProduct: LotNumberWithProduct,
            val featureFlags: List<FeatureFlag>,
            val isNewLotNumber: Boolean = false
        ) : BaseScannerState()

        data class DriverLicenseBarcodeFound(
            val productSelectionMetric: ProductSelectionMetric,
            val driverLicense: DriverLicense
        ) : BaseScannerState()

        data class BarcodeErrorFound(
            val productSelectionMetric: ProductSelectionMetric,
            val errorMessage: String
        ) : BaseScannerState()

        data class ProductNotAllowed(
            val ndcCode: String,
            val errorMessage: String
        ) : BaseScannerState()
    }

    fun onTwoDeeBarcode(barcode: TwoDeeBarcode) {
        val productSelectionMetric =
            ProductSelectionMetric(
                symbologyScanned = barcode.symbolName,
                rawBarcodeData = barcode.barcode,
                ndc = barcode.productNdc,
                expirationDate = barcode.expiration.toLocalDateString(),
                lotNumber = barcode.lotNumber,
                productSource = productSource2D,
                hubModel = mobileHub
            )

        viewModelScope.launch {
            try {
                val result = coroutineScope {
                    ProductAndFlags(
                        async { productRepository.findLotNumberByNameAsync(barcode.lotNumber) }.await(),
                        async { locationRepository.getFeatureFlagsAsync() }.await()
                    )
                }

                val ln = result.product
                val featureFlags = result.flags

                wrongProductRepository.findProductByNdc(barcode.productNdc)
                    ?.let { wrongProductNdc ->
                        setState(
                            BaseScannerState.ProductNotAllowed(
                                ndcCode = wrongProductNdc.ndc,
                                errorMessage = wrongProductNdc.errorMessage
                            )
                        )

                        return@launch
                    }

                if (ln == null) {
                    // Lot number was not found
                    val product = productRepository.findProductByNdcCode(barcode.productNdc)

                    if (product != null) {
                        val mapping = productRepository.findMapping(product.id)
                        val newLotNumber = LotNumberWithProduct(
                            expirationDate = barcode.expiration,
                            id = 0,
                            name = barcode.lotNumber,
                            salesLotNumberId = -1,
                            productId = product.id,
                            salesProductId = mapping?.id ?: -1,
                            product = product.toProduct(),
                            ageIndications = product.ageIndications,
                            cptCodes = product.cptCvxCodes,
                            dosesInSeries = mapping?.dosesInSeries ?: 1
                        )

                        productSelectionMetric.apply {
                            this.salesProductId = newLotNumber.salesProductId
                            this.productName = mapping?.prettyName ?: product.displayName
                        }

                        setState(
                            BaseScannerState.LotNumberBarcodeFound(
                                productSelectionMetric,
                                newLotNumber,
                                featureFlags,
                                true
                            )
                        )
                    } else {
                        setState(
                            BaseScannerState.BarcodeErrorFound(
                                productSelectionMetric,
                                "No product/lot found, lotnumber: ${barcode.lotNumber}, product: ${barcode.productNdc}"
                            )
                        )
                    }
                } else {
                    // Lot number was found
                    productSelectionMetric.apply {
                        this.salesProductId = ln.salesProductId
                        this.productName = ln.product.displayName
                        this.productSource = productSource2D
                    }
                    val mapping = productRepository.findMapping(ln.productId)

                    ln.dosesInSeries = mapping?.dosesInSeries ?: ln.dosesInSeries

                    setState(
                        BaseScannerState.LotNumberBarcodeFound(
                            productSelectionMetric,
                            ln,
                            featureFlags
                        )
                    )
                }
            } catch (e: Exception) {
                setState(
                    BaseScannerState.BarcodeErrorFound(
                        productSelectionMetric,
                        "No product/lot found, lotnumber: ${barcode.lotNumber}, product: ${barcode.productNdc}"
                    )
                )
            }
        }
    }

    fun onDriverLicenseBarcode(symbology: String, driverLicense: DriverLicense) {
        val metric = ProductSelectionMetric(
            symbologyScanned = symbology,
            productSource = productSource2D,
            hubModel = mobileHub
        )

        setState(BaseScannerState.DriverLicenseBarcodeFound(metric, driverLicense))
    }

    fun onErrorBarcode(barcode: ErrorBarcode) {
        val productSelectionMetric =
            ProductSelectionMetric(
                symbologyScanned = barcode.symbolName,
                rawBarcodeData = barcode.barcode,
                hubModel = mobileHub
            )

        setState(
            BaseScannerState.BarcodeErrorFound(
                productSelectionMetric,
                barcode.message
            )
        )
    }

    /**
     * Wrapper object for Product and FF call
     */
    private data class ProductAndFlags(
        val product: LotNumberWithProduct?,
        val flags: List<FeatureFlag>
    )
}
