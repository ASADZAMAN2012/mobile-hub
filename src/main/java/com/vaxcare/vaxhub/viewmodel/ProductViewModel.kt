/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ProductSelectionMetric
import com.vaxcare.core.report.model.checkout.RelativeDoS
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.di.args.InsertLotNumbersJobArgs
import com.vaxcare.vaxhub.model.enums.LotNumberSources
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.ProductAndPackage
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProduct
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.repository.SimpleOnHandInventoryRepository
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val simpleOnHandInventoryRepository: SimpleOnHandInventoryRepository,
    private val analytics: AnalyticReport
) : ViewModel() {
    val lotNumberSelection = MutableLiveData<LotNumberWithProduct>(null)

    suspend fun findProductByNdc(ndc: String) = productRepository.findProductByNdcCode(ndc)

    suspend fun findLotNumberByName(name: String) = productRepository.findLotNumberByNameAsync(name)

    suspend fun findMapping(id: Int) = productRepository.findMapping(id)

    suspend fun findOneTouch(id: Int) = productRepository.findOneTouch(id)

    suspend fun getSimpleOnHandInventoryByProductLotName(productLotName: String): List<SimpleOnHandProduct> =
        viewModelScope.async(Dispatchers.IO) {
            simpleOnHandInventoryRepository.getSimpleOnHandProductsByLotName(productLotName)
        }.await()

    fun getAllVaccines(): LiveData<List<ProductAndPackage>> {
        val productCategoryList = listOf(ProductCategory.VACCINE, ProductCategory.LARC)
        return productRepository.getAllProductsByCategories(productCategoryList)
    }

    fun insertLot(lot: LotNumberWithProduct, context: Context) {
        OneTimeWorker.buildOneTimeUniqueWorker(
            wm = WorkManager.getInstance(context),
            parameters = OneTimeParams.InsertLotNumbers(
                params = InsertLotNumbersJobArgs(
                    lotNumberName = lot.name,
                    epProductId = lot.productId,
                    expiration = lot.expirationDate,
                    source = LotNumberSources.VaxHubScan.id
                )
            )
        )
    }

    fun saveProductSelectionMetric(
        lotNumberWithProduct: LotNumberWithProduct,
        relativeDoS: RelativeDoS?,
        appointmentId: Int?,
        copays: Map<String, BigDecimal>?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val selfPayRate =
                productRepository.findOneTouch(lotNumberWithProduct.productId)?.selfPayRate

            analytics.saveMetric(
                ProductSelectionMetric(
                    screenSource = "Lot Selection",
                    scannerType = "Manual",
                    productSource = "Manual Lot Selection",
                    hubModel = "Mobile Hub",
                    salesProductId = lotNumberWithProduct.salesProductId,
                    productName = lotNumberWithProduct.product.displayName,
                    ndc = lotNumberWithProduct.product.productNdc ?: "",
                    lotNumber = lotNumberWithProduct.name,
                    expirationDate = lotNumberWithProduct.expirationDate?.toLocalDateString() ?: "",
                    relativeDoS = relativeDoS,
                    patientVisitId = appointmentId,
                    selfPayRate = selfPayRate,
                    copays = copays
                )
            )
        }
    }
}
