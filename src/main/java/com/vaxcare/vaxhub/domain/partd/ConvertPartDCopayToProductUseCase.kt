/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.domain.partd

import com.vaxcare.vaxhub.model.partd.PartDCopay
import com.vaxcare.vaxhub.repository.ProductRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConvertPartDCopayToProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(copays: List<PartDCopay>) =
        runBlocking {
            val productIds = copays.mapNotNull { copay -> copay.productId }
            productRepository.findProductsByProductIds(productIds)
                .map { productAndPackage ->
                    val product = productAndPackage.toProduct()
                    product to copays.first { it.productId == product.id }
                }.groupBy { it.first.antigen }
        }
}
