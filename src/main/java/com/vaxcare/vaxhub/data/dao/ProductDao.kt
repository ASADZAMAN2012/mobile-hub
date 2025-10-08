/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.vaxhub.model.inventory.AgeIndication
import com.vaxcare.vaxhub.model.inventory.CptCvxCode
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.NdcCode
import com.vaxcare.vaxhub.model.inventory.Package
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.ProductAndPackage
import com.vaxcare.vaxhub.model.inventory.ProductDto
import com.vaxcare.vaxhub.model.inventory.ProductOneTouch

@Suppress("ktlint:standard:max-line-length")
@Dao
abstract class ProductDao() {
    @Transaction
    @Query("SELECT * FROM Product")
    abstract fun getAllProducts(): LiveData<List<ProductAndPackage>>

    @Transaction
    @Query("SELECT * FROM Product ORDER BY displayName")
    abstract suspend fun getAllProductsAsync(): List<ProductAndPackage>

    @Transaction
    @Query(
        """SELECT * FROM Product WHERE categoryId in (:categoryIds) AND status IN (1, 2)
            ORDER BY displayName COLLATE NOCASE"""
    )
    abstract fun getAllProductsByCategories(categoryIds: List<Int>): LiveData<List<ProductAndPackage>>

    @Transaction
    @Query(
        """SELECT map.id
            FROM Product p INNER JOIN LegacyProductMapping map ON map.epProductId = p.Id
            WHERE p.categoryId in (:categoryIds)"""
    )
    abstract suspend fun getAllSalesProductIdsByCategories(categoryIds: List<Int>): List<Int>

    @Transaction
    @Insert
    suspend fun insert(products: List<ProductDto>) {
        insertProducts(
            products.map {
                Product(
                    antigen = it.antigen,
                    categoryId = it.categoryId,
                    description = it.description,
                    displayName = if (it.isFlu().not()) {
                        it.displayName
                    } else {
                        it.prettyName
                            ?: it.displayName
                    },
                    id = it.id,
                    inventoryGroup = it.inventoryGroup,
                    lossFee = it.lossFee,
                    productNdc = it.productNdc,
                    routeCode = it.routeCode,
                    presentation = it.presentation,
                    purchaseOrderFee = it.purchaseOrderFee,
                    visDates = it.visDates,
                    status = it.status,
                    prettyName = it.prettyName
                )
            }
        )
        insertPackages(products.map { it.packages }.flatten())
        insertCptCvxCodes(products.map { it.cptCvxCodes }.flatten())
        insertNdcCodes(
            products.map { pro ->
                pro.packages.map { pack ->
                    pack.packageNdcs.map {
                        NdcCode(
                            it,
                            pack.productId
                        )
                    }
                }.flatten()
            }.flatten()
        )
        deleteAgeIndicationsByProductIds(products.map { it.id })
        insertAgeIndications(products.map { pro -> pro.ageIndications }.flatten())
    }

    @Query("SELECT * FROM Product WHERE id = :id LIMIT 1")
    abstract suspend fun findProductById(id: Int): Product?

    @Query(
        """SELECT p.*
            FROM Product p
            INNER JOIN LegacyProductMapping map ON map.epProductId = p.Id
            WHERE map.id IN (:ids)"""
    )
    abstract suspend fun findProductsBySalesProductIds(ids: List<Int>): List<Product>?

    @Transaction
    @Query(
        """SELECT map.id
            FROM Product p INNER JOIN LegacyProductMapping map ON map.epProductId = p.Id
            WHERE p.inventoryGroup = :inventoryGroup"""
    )
    abstract suspend fun getAllProductIdsByInventoryGroup(inventoryGroup: String): List<Int>

    @Transaction
    @Query(
        """SELECT p.* FROM NdcCode nc
            INNER JOIN Product p ON p.id = nc.productId
            WHERE nc.ndcCode = :ndc"""
    )
    abstract suspend fun findProductByNdcCode(ndc: String): ProductAndPackage?

    @Transaction
    @Query(
        """SELECT * FROM Product
            WHERE id in (:productIds)"""
    )
    abstract suspend fun findProductsAndPackagesByProductIds(productIds: List<Int>): List<ProductAndPackage>

    @Transaction
    @Query(
        """SELECT * FROM LotNumber ln
            INNER JOIN Product p on p.id = ln.productId
            WHERE ln.name = :lotNumber"""
    )
    abstract suspend fun findLotNumberByName(lotNumber: String): LotNumberWithProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPackages(packages: List<Package>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCptCvxCodes(cptCvxCodes: List<CptCvxCode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertNdcCodes(ndcList: List<NdcCode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAgeIndications(ageIndications: List<AgeIndication>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOneTouch(productOneTouch: List<ProductOneTouch>)

    @Query("SELECT * FROM ProductOneTouch WHERE productId = :productId LIMIT 1")
    abstract suspend fun getOneTouch(productId: Int): ProductOneTouch?

    @Query("DELETE FROM AgeIndication WHERE productId in (:productIds)")
    protected abstract suspend fun deleteAgeIndicationsByProductIds(productIds: List<Int>)
}
