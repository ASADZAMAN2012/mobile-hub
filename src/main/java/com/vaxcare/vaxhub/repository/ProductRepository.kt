/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.repository

import androidx.lifecycle.LiveData
import com.vaxcare.vaxhub.data.dao.ProductDao
import com.vaxcare.vaxhub.data.dao.legacy.LegacyLotInventoryDao
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.ProductAndPackage
import com.vaxcare.vaxhub.model.inventory.ProductOneTouch
import com.vaxcare.vaxhub.model.legacy.LegacyProductMapping
import com.vaxcare.vaxhub.web.InventoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface ProductRepository {
    suspend fun findLotNumberByNameAsync(name: String): LotNumberWithProduct?

    suspend fun findOneTouch(id: Int): ProductOneTouch?

    suspend fun findMapping(id: Int): LegacyProductMapping?

    fun getAllProductsByCategories(categories: List<ProductCategory>): LiveData<List<ProductAndPackage>>

    suspend fun getListOfProductIdsByCategory(productCategory: ProductCategory): List<Int>

    suspend fun getLarcProductIDs(): List<Int>

    suspend fun findProductsBySalesProductIds(ids: List<Int>): List<Product>?

    suspend fun syncAllProducts(isCalledByJob: Boolean = false)

    /**
     * Return a list of Sales ProductIDs by inventoryGroup
     *
     * @param inventoryGroup
     * @return
     */
    suspend fun findProductIdsByInventoryGroup(inventoryGroup: String): List<Int>

    suspend fun findProductByNdcCode(ndc: String): ProductAndPackage?

    suspend fun findProductsByProductIds(productIds: List<Int>): List<ProductAndPackage>
}

class ProductRepositoryImpl @Inject constructor(
    private val inventoryApi: InventoryApi,
    private val productDao: ProductDao,
    private val legacyLotInventoryDao: LegacyLotInventoryDao
) : ProductRepository {
    companion object {
        private const val LARC_INVENTORY_GROUP = "LARC"
    }

    override suspend fun findLotNumberByNameAsync(name: String): LotNumberWithProduct? =
        try {
            productDao.findLotNumberByName(name)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    override suspend fun findOneTouch(id: Int): ProductOneTouch? = productDao.getOneTouch(id)

    override suspend fun findMapping(id: Int): LegacyProductMapping? = legacyLotInventoryDao.getMapping(id)

    override fun getAllProductsByCategories(categories: List<ProductCategory>): LiveData<List<ProductAndPackage>> =
        productDao.getAllProductsByCategories(categories.map { it.id })

    override suspend fun getListOfProductIdsByCategory(productCategory: ProductCategory): List<Int> =
        withContext(Dispatchers.IO) {
            productDao.getAllSalesProductIdsByCategories(listOf(productCategory.id))
        }

    override suspend fun getLarcProductIDs(): List<Int> =
        coroutineScope {
            val larcProductIDsByCategory = async {
                getListOfProductIdsByCategory(ProductCategory.LARC)
            }.await()
            val larcProductIDsByInventoryGroup = async {
                findProductIdsByInventoryGroup(LARC_INVENTORY_GROUP)
            }.await()
            larcProductIDsByCategory + larcProductIDsByInventoryGroup
        }.distinct()

    override suspend fun findProductsBySalesProductIds(ids: List<Int>): List<Product>? =
        productDao.findProductsBySalesProductIds(ids)

    override suspend fun syncAllProducts(isCalledByJob: Boolean) {
        syncProductMappings(isCalledByJob)
        syncProducts(isCalledByJob)
        syncProductOneTouch(isCalledByJob)
    }

    override suspend fun findProductIdsByInventoryGroup(inventoryGroup: String): List<Int> {
        return productDao.getAllProductIdsByInventoryGroup(inventoryGroup)
    }

    override suspend fun findProductByNdcCode(ndc: String): ProductAndPackage? = productDao.findProductByNdcCode(ndc)

    override suspend fun findProductsByProductIds(productIds: List<Int>): List<ProductAndPackage> =
        productDao.findProductsAndPackagesByProductIds(productIds)

    private suspend fun syncProducts(isCalledByJob: Boolean) {
        val mappings = legacyLotInventoryDao.getAllMappings()
            .groupBy { it.epProductId }
            .map { it.key to it.value.firstOrNull()?.prettyName }
        val products = inventoryApi.getProducts(isCalledByJob)
            .map { product ->
                product.apply {
                    prettyName = mappings.firstOrNull { map -> map.first == product.id }?.second
                }
            }
        productDao.insert(products)
    }

    private suspend fun syncProductMappings(isCalledByJob: Boolean) {
        val productMapping = inventoryApi.getProductMappings(isCalledByJob)
        legacyLotInventoryDao.insertMapping(productMapping)
    }

    private suspend fun syncProductOneTouch(isCalledByJob: Boolean) {
        val productOneTouch = inventoryApi.getProductOneTouch(isCalledByJob)
        productDao.insertOneTouch(productOneTouch)
    }
}
