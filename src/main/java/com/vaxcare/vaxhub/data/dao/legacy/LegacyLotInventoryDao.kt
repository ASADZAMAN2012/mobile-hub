/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data.dao.legacy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaxcare.vaxhub.model.legacy.LegacyLotInventory
import com.vaxcare.vaxhub.model.legacy.LegacyProductMapping

@Dao
abstract class LegacyLotInventoryDao() {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(inventory: List<LegacyLotInventory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMapping(mapping: List<LegacyProductMapping>)

    @Query("SELECT * FROM LegacyProductMapping WHERE epProductId = :productId LIMIT 1")
    abstract suspend fun getMapping(productId: Int): LegacyProductMapping?

    @Query("SELECT * FROM LegacyProductMapping")
    abstract suspend fun getAllMappings(): List<LegacyProductMapping>

    @Query("DELETE FROM LegacyLotInventory")
    abstract suspend fun deleteAll()
}
