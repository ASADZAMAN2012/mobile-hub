/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao.legacy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.model.legacy.LegacyCount
import com.vaxcare.vaxhub.model.legacy.LegacyCountEntry
import com.vaxcare.vaxhub.model.legacy.LegacyProductWithCountDto

@Dao
abstract class LegacyCountDao {
    // 1. Start from Products
    // 2. Go to Entries
    // 3. Go to counts
    // 4. Filter by date
    @Transaction
    @Query(
        """
        SELECT
            -- Product
            p.antigen,
            p.categoryId,
            p.description,
            p.displayName,
            p.id,
            p.lossFee,
            CASE WHEN i.onHand IS NULL THEN 0 ELSE i.onHand END AS onHand,
            p.presentation,
            p.purchaseOrderFee,
            p.status,
            
            -- Count Data
            counts.createdOn,
            counts.guid,
            counts.lotNumber,
            counts.stock,
            counts.newOnHand,
            counts.prevOnHand,
            counts.doseValue
        FROM Product p
        LEFT JOIN (
            SELECT SUM(l.onHand) AS onHand, l.productId 
            FROM LegacyLotInventory l
            WHERE l.inventorySource = :inventorySource
            GROUP BY l.productId
        ) i ON i.productId = p.id
        LEFT JOIN (
            SELECT 
                cc.createdOn,
                cc.guid,
                cce.lotNumber,
                cc.stock,
                cce.newOnHand,
                cce.prevOnHand,
                cce.doseValue,
                cce.epProductId
            FROM LegacyCountEntry cce --ON cce.epProductId = p.id
            LEFT JOIN LegacyCount cc ON cc.guid = cce.countGuid
            WHERE STRFTIME('%m', datetime(cc.createdOn/1000, 'unixepoch')) = :month
                AND STRFTIME('%Y', datetime(cc.createdOn/1000, 'unixepoch')) = :year
                AND cc.stock = :inventorySource
        ) counts ON counts.epProductId = p.id
        WHERE p.categoryId in (2,3)
        ORDER BY p.displayName
    """
    )
    abstract suspend fun getCountRollupByMonth(
        month: String,
        year: String,
        inventorySource: InventorySource
    ): List<LegacyProductWithCountDto>

    @Transaction
    @Insert
    suspend fun insert(counts: List<LegacyCount>) {
        deleteCountEntries()
        deleteCounts()
        insertCounts(counts)
        insertCountEntries(counts.flatMap { it.countList })
    }

    @Query("DELETE FROM LegacyCount")
    protected abstract suspend fun deleteCounts()

    @Query("DELETE FROM LegacyCountEntry")
    protected abstract suspend fun deleteCountEntries()

    @Insert
    protected abstract suspend fun insertCounts(counts: List<LegacyCount>)

    @Insert
    protected abstract suspend fun insertCountEntries(counts: List<LegacyCountEntry>)
}
